package ch.ethz.ruxu.asl;

import ch.ethz.ruxu.asl.exceptions.BadRequestException;
import ch.ethz.ruxu.asl.exceptions.EndOfStreamException;
import ch.ethz.ruxu.asl.exceptions.ResourceUnavailableException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;

public class Middleware {
    // The logger
    private static final Logger logger = LogManager.getLogger(Middleware.class);
    // A flag to indicate whether there is any error in the worker
    static volatile boolean workerError = false;
    // Socket address that the middleware will listen to
    private SocketAddress middlewareAddress;
    // An array of socket addresses of the servers
    private SocketAddress[] serverAddresses;
    // The reader thread pool size for each server
    private int readerThreadPoolSize;
    // The replication factor for write (set and delete) requests
    private int writerReplicationFactor;

    /**
     * Constructor to create a new middleware
     * @param middlewareIP the IP address of the middleware
     * @param middlewarePort the port number that the middleware is listening to
     * @param serverAddresses a list of the server addresses in the IP:port format
     * @param readerThreadPoolSize the reader thread pool size for each server
     * @param writerReplicationFactor the replication factor for write requests
     */
    public Middleware(String middlewareIP,
                      int middlewarePort,
                      List<String> serverAddresses,
                      int readerThreadPoolSize,
                      int writerReplicationFactor) {
        this.middlewareAddress = new InetSocketAddress(middlewareIP, middlewarePort);
        this.serverAddresses = new SocketAddress[serverAddresses.size()];
        for (int i = 0; i < this.serverAddresses.length; i++) {
            String[] parts = serverAddresses.get(i).split(":");
            this.serverAddresses[i] = new InetSocketAddress(parts[0], Integer.parseInt(parts[1]));
        }
        this.readerThreadPoolSize = readerThreadPoolSize;
        this.writerReplicationFactor = writerReplicationFactor;
    }

    /**
     * Create a list of connection pools with length equal to the number of servers such that connection pool i will
     * have j connections to the server i with j equal to the reader thread pool size
     * @return a list of connection pools, if and only if, the connection pools are created successfully. Otherwise null
     */
    private List<BlockingQueue<SocketChannel>> createConnectionPools() {
        List<BlockingQueue<SocketChannel>> connectionPools = new ArrayList<>(this.serverAddresses.length);
        for (int i = 0; i < this.serverAddresses.length; i++) {
            BlockingQueue<SocketChannel> connectionPool = new ArrayBlockingQueue<>(this.readerThreadPoolSize, true);
            connectionPools.add(connectionPool);
        }

        try {
            for (int i = 0; i < this.serverAddresses.length; i++) {
                BlockingQueue<SocketChannel> connectionPool = connectionPools.get(i);
                for (int j = 0; j < this.readerThreadPoolSize; j++) {
                    SocketChannel serverSocketChannel = SocketChannel.open();
                    // The read requests are synchronous
                    serverSocketChannel.configureBlocking(true);
                    serverSocketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
                    serverSocketChannel.setOption(StandardSocketOptions.SO_LINGER, 5);
                    serverSocketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
                    serverSocketChannel.connect(this.serverAddresses[i]);
                    while (!serverSocketChannel.finishConnect()) {
                        // Wait until the connection is finished
                    }
                    connectionPool.add(serverSocketChannel);
                }
                Middleware.logger.debug("Middleware established reader connection pool for server {} with pool size {}",
                        this.serverAddresses[i].toString(), this.readerThreadPoolSize);
            }
            return connectionPools;
        } catch (IOException e) {
            Middleware.logger.error("Middleware failed to to establish read connection pool", e);
            this.closeConnectionPools(connectionPools);
            return null;
        }
    }

    /**
     * Close all opening connections contained in the connection pools
     * @param connectionPools a list of connection pools
     */
    private void closeConnectionPools(List<BlockingQueue<SocketChannel>> connectionPools) {
        for (int i = 0; i < this.serverAddresses.length; i++) {
            BlockingQueue<SocketChannel> connectionPool = connectionPools.get(i);
            SocketChannel serverSocketChannel = connectionPool.poll();
            while (serverSocketChannel != null) {
                try {
                    serverSocketChannel.close();
                } catch (IOException e) {
                    Middleware.logger.error("Middleware failed to close reader connection pool", e);
                }
                serverSocketChannel = connectionPool.poll();
            }
            Middleware.logger.debug(
                    "Middleware closed reader connection pool for server {}", this.serverAddresses[i].toString());
        }
    }

    /**
     * Create and start an array of reader thread pools with the length equal to the number of servers such that reader
     * thread pool i is responsible for read requests to server i
     * @return an array of reader thread pools
     */
    private ExecutorService[] createAndStartReaders() {
        ExecutorService[] readers = new ExecutorService[this.serverAddresses.length];
        for (int i = 0; i < this.serverAddresses.length; i++) {
            readers[i] = Executors.newFixedThreadPool(this.readerThreadPoolSize);
        }
        return readers;
    }

    /**
     * Create a list of task queues with the length equal to the number of servers such that queue i is used to store
     * write requests to server i
     * @return a list of task queues
     */
    private List<BlockingQueue<SelectionKey>> createTaskQueues() {
        List<BlockingQueue<SelectionKey>> taskQueues = new ArrayList<>(this.serverAddresses.length);
        for (int i = 0; i < this.serverAddresses.length; i++) {
            BlockingQueue<SelectionKey> taskQueue = new LinkedBlockingQueue<>();
            taskQueues.add(taskQueue);
        }
        return taskQueues;
    }

    /**
     * Create and start an array of writer threads with the length equal to the number of servers such that writer i is
     * associated with the task queue i, and is responsible for write requests to server i
     * @param writeTaskQueues a list of task queues with length equal to the number of servers
     * @return an array of running writer threads
     */
    private Thread[] createAndStartWriters(List<BlockingQueue<SelectionKey>> writeTaskQueues) {
        Thread[] writers = new Thread[this.serverAddresses.length];
        for (int i = 0; i < this.serverAddresses.length; i++) {
            SocketAddress[] serverAddressesForWriter = new SocketAddress[this.writerReplicationFactor];
            for (int j = 0; j < this.writerReplicationFactor; j++) {
                if (i + j < this.serverAddresses.length) {
                    serverAddressesForWriter[j] = this.serverAddresses[i + j];
                } else {
                    serverAddressesForWriter[j] = this.serverAddresses[i + j - this.serverAddresses.length];
                }
            }
            writers[i] = new Thread(new WriterRunner(serverAddressesForWriter, writeTaskQueues.get(i)));
            writers[i].start();
        }
        return writers;
    }

    /**
     * Read the client request from the network buffer in a non-blocking way
     * @param clientSocketChannel the socket channel representing the connection from client
     * @param attachment the attachment associated with the client
     * @return true if, and only if, the complete request is read out from the network buffer
     * @throws BadRequestException  if the request is not supported by the middleware
     * @throws EndOfStreamException if the end of the stream is reached
     * @throws IOException if some other I/O error occurs
     */
    private boolean readClientRequest(SocketChannel clientSocketChannel,
                                      Attachment attachment)
            throws BadRequestException, EndOfStreamException, IOException  {
        // Prepare the attachment for a new request if necessary
        if (attachment.stage != Stage.CLIENT_REQUEST) {
            // Record the timestamp of when the client request is about to be read from the network buffer
            attachment.timestamps[0] = System.nanoTime();
            attachment.id = 0;
            attachment.stage = Stage.CLIENT_REQUEST;
            attachment.primaryBuffer.clear();
            attachment.secondaryBuffer = null;
            attachment.actualNumBytes = 0;
            attachment.expectedNumBytes = -1;
            attachment.command = null;
            attachment.key = null;
        }

        int numBytes;
        while (attachment.actualNumBytes != attachment.expectedNumBytes) {
            if (attachment.secondaryBuffer == null) {
                numBytes = clientSocketChannel.read(attachment.primaryBuffer);
            } else {
                numBytes = (int) clientSocketChannel.read(
                        new ByteBuffer[] {attachment.primaryBuffer, attachment.secondaryBuffer});
            }

            if (numBytes > 0) {
                attachment.actualNumBytes += numBytes;
                int primaryBufferPosition = attachment.primaryBuffer.position();
                if (attachment.expectedNumBytes == -1 && primaryBufferPosition >= 2) {
                    // The text line of the client request has not yet been parsed. By assumption, the primary buffer is
                    // big enough to hold the entire text line. Try to parse out the text line again
                    byte[] primaryBufferBytes = attachment.primaryBuffer.array();
                    for (int dataLineStart = 2; dataLineStart <= primaryBufferPosition; dataLineStart++) {
                        if (primaryBufferBytes[dataLineStart - 2] == 13
                                && primaryBufferBytes[dataLineStart - 1] == 10) {
                            // End of the text line \r\n found
                            byte[] textLineBytes = Arrays.copyOfRange(primaryBufferBytes, 0, dataLineStart);
                            String[] textLineStringParts =
                                    new String(textLineBytes, StandardCharsets.US_ASCII).split("\\s+");
                            switch (textLineStringParts[0]) {
                                case "get":
                                    // Protocol:
                                    // get <key>*\r\n
                                    attachment.expectedNumBytes = dataLineStart;
                                    attachment.command = Command.GET;
                                    break;
                                case "set":
                                    // Protocol:
                                    // set <key> <flags> <exptime> <bytes> [noreply]\r\n<data block>\r\n
                                    attachment.expectedNumBytes =
                                            dataLineStart + Integer.parseInt(textLineStringParts[4]) + 2;
                                    attachment.command = Command.SET;
                                    // Check if extra buffer needs to be allocated
                                    int secondaryBufferCapacity =
                                            attachment.expectedNumBytes - attachment.primaryBuffer.capacity();
                                    if (secondaryBufferCapacity > 0) {
                                        attachment.secondaryBuffer = ByteBuffer.allocate(secondaryBufferCapacity);
                                    }
                                    break;
                                case "delete":
                                    // Protocol:
                                    // delete <key> [noreply]\r\n
                                    attachment.expectedNumBytes = dataLineStart;
                                    attachment.command = Command.DELETE;
                                    break;
                                default:
                                    throw new BadRequestException();
                            }
                            attachment.key = textLineStringParts[1].getBytes(StandardCharsets.US_ASCII);
                            break;
                        }
                    }
                }
            } else if (numBytes == 0) {
                // Nothing can be read from the network buffer at the moment, try again later
                return false;
            } else {
                throw new EndOfStreamException();
            }
        }

        return true;
    }

    /**
     * Start running the middleware
     */
    public void run() {
        Middleware.logger.debug("Middleware starting");

        MessageDigest md;
        try {
            md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            Middleware.logger.error("Middleware failed to initialize message digester", e);
            return;
        }

        // A list of queues holding connections to each server used by the reader thread pools
        List<BlockingQueue<SocketChannel>> connectionPools = this.createConnectionPools();
        if (connectionPools == null) {
            return;
        }

        // Create and start the reader thread pools
        ExecutorService[] readers = this.createAndStartReaders();

        // A list of queues to store write requests for each server
        List<BlockingQueue<SelectionKey>> writerTaskQueues = this.createTaskQueues();

        // Create and start the writers
        Thread[] writers = this.createAndStartWriters(writerTaskQueues);

        boolean includeThreadCounter = false;
        Thread counter = null;
        if (includeThreadCounter) {
            counter = new Thread(new ThreadCounterRunner());
            counter.start();
        }

        // The get and set request ids are counted separately to facilitate the sampling of the log entries
        long getRequestId = 0;
        long setRequestId = 0;
        long startTimestamp = 0;

        // Configure the middleware to listen on the specified address and start processing incoming requests
        try (Selector clientSelector = Selector.open();
             ServerSocketChannel middlewareSocketChannel = ServerSocketChannel.open()) {
            middlewareSocketChannel.configureBlocking(false);
            middlewareSocketChannel.bind(this.middlewareAddress);
            middlewareSocketChannel.register(clientSelector, SelectionKey.OP_ACCEPT);
            Middleware.logger.debug("Middleware listening on {}", this.middlewareAddress.toString());

            boolean clientConnected = false;
            int clientConnectionCount = 0;
            int numServers = this.serverAddresses.length;

            while (!Middleware.workerError) {
                if (clientSelector.selectNow() > 0) {
                    Iterator<SelectionKey> clientSelectorIterator = clientSelector.selectedKeys().iterator();
                    while (clientSelectorIterator.hasNext()) {
                        SelectionKey clientSelectionKey = clientSelectorIterator.next();
                        clientSelectorIterator.remove();
                        if (clientSelectionKey.isReadable()) {
                            // Read request from the client
                            SocketChannel clientSocketChannel = (SocketChannel) clientSelectionKey.channel();
                            Attachment attachment = (Attachment) clientSelectionKey.attachment();

                            try {
                                // Try to read the client request into the attachment from the network buffer
                                if (this.readClientRequest(clientSocketChannel, attachment)) {
                                    // Temporarily deregister this channel from the selector for read events. Upon
                                    // finishing processing the request, the worker will re-register the channel to the
                                    // selector for read events
                                    clientSelectionKey.interestOps(0);

                                    // Calculate the primary server id
                                    long hash = ByteBuffer.wrap(md.digest(attachment.key)).getLong()
                                            & 0x0fffffffffffffffL;
                                    attachment.serverId = (int) (hash % numServers);
                                    md.reset();

                                    if (attachment.command == Command.GET) {
                                        attachment.id = ++getRequestId;
                                        // Record the timestamp of when the task is queued for processing
                                        attachment.timestamps[1] = System.nanoTime();
                                        ReaderRunner readerRunner = new ReaderRunner(
                                                clientSelectionKey, connectionPools.get(attachment.serverId));
                                        readers[attachment.serverId].execute(readerRunner);
                                    } else if (attachment.command == Command.SET) {
                                        attachment.id = ++setRequestId;
                                        // Record the timestamp of when the task is queued for processing
                                        attachment.timestamps[1] = System.nanoTime();
                                        // Try to enqueue to task to the corresponding queue
                                        if (!writerTaskQueues
                                                .get(attachment.serverId)
                                                .offer(clientSelectionKey, 60, TimeUnit.SECONDS)) {
                                            throw new ResourceUnavailableException();
                                        }
                                    } else if (attachment.command == Command.DELETE) {
                                        // Record the timestamp of when the task is queued for processing
                                        attachment.timestamps[1] = System.nanoTime();
                                        // Try to enqueue to task to the corresponding queue
                                        if (!writerTaskQueues
                                                .get(attachment.serverId)
                                                .offer(clientSelectionKey, 60, TimeUnit.SECONDS)) {
                                            throw new ResourceUnavailableException();
                                        }
                                    } else {
                                        throw new BadRequestException();
                                    }
                                }
                            } catch (BadRequestException e) {
                                clientSocketChannel.close();
                                clientSelectionKey.cancel();
                                clientConnectionCount--;
                                Middleware.logger.error("Middleware received bad request from client", e);
                            } catch (EndOfStreamException e) {
                                clientSocketChannel.close();
                                clientSelectionKey.cancel();
                                clientConnectionCount--;
                                Middleware.logger.debug("Middleware reached end of stream from client");
                            } catch (ResourceUnavailableException e) {
                                clientSocketChannel.close();
                                clientSelectionKey.cancel();
                                clientConnectionCount--;
                                Middleware.logger.error("Middleware overloaded", e);
                            }
                        } else if (clientSelectionKey.isAcceptable()) {
                            // Accept a new connection from the client
                            SocketChannel clientSocketChannel = middlewareSocketChannel.accept();
                            clientSocketChannel.configureBlocking(false);
                            clientSocketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
                            clientSocketChannel.setOption(StandardSocketOptions.SO_LINGER, 5);
                            clientSocketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
                            clientSocketChannel.register(clientSelector, SelectionKey.OP_READ, new Attachment());
                            if (!clientConnected) {
                                startTimestamp = System.currentTimeMillis();
                                clientConnected = true;
                            }
                            clientConnectionCount++;
                            Middleware.logger.debug(String.format("%d,%s",
                                    System.currentTimeMillis(),
                                    "Middleware accepted new connection from client"));
                        }
                    }
                }

                if (clientConnected && clientConnectionCount == 0) {
                    break;
                }
            }
        } catch (IOException e) {
            Middleware.logger.error("Middleware I/O error", e);
        } catch (InterruptedException e) {
            Middleware.logger.error("Middleware interrupted", e);
        } finally {
            // Stop the thread counter
            if (includeThreadCounter) {
                counter.interrupt();
            }

            // Stop the readers
            Middleware.logger.debug("Middleware waiting for readers to shutdown");
            for (ExecutorService reader: readers) {
                reader.shutdown();
                try {
                    if (!reader.awaitTermination(60, TimeUnit.SECONDS)) {
                        reader.shutdownNow();
                        if (!reader.awaitTermination(60, TimeUnit.SECONDS)) {
                            Middleware.logger.error("Reader failed to shutdown");
                        }
                    }
                } catch (InterruptedException e) {
                    Middleware.logger.error("Middleware interrupted while waiting for reader to shutdown", e);
                    reader.shutdownNow();
                }
            }

            // Close the opening connections
            Middleware.logger.debug("Middleware waiting for connection pool to close");
            this.closeConnectionPools(connectionPools);

            // Stop the writers
            Middleware.logger.debug("Middleware waiting for writers to finish");
            for (Thread writer: writers) {
                writer.interrupt();
                try {
                    writer.join(60000);
                } catch (InterruptedException e) {
                    Middleware.logger.error("Middleware interrupted while waiting for writer to finish", e);
                }
            }

            Middleware.logger.debug("Middleware processed {} get and {} set requests in {} ms",
                    getRequestId, setRequestId, System.currentTimeMillis() - startTimestamp);

            Middleware.logger.debug("Middleware stopping");
        }
    }
}
