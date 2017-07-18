package ch.ethz.ruxu.asl;

import ch.ethz.ruxu.asl.exceptions.EndOfStreamException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.SocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.BlockingQueue;


class WriterRunner implements Runnable {
    // The logger
    private final static Logger logger = LogManager.getLogger(WriterRunner.class);
    // The replication factor
    private int replicationFactor;
    // An array of socket addresses to the servers
    private SocketAddress[] serverAddresses;
    // The connection to the servers
    private SocketChannel[] serverSocketChannels;
    // The attachment for each server
    private WriterAttachment[] writerAttachments;
    // A queue of tasks waiting to be processed
    private BlockingQueue<SelectionKey> taskQueue;

     /**
     * Construct a new writer runner
     * @param serverAddresses an array of socket addresses to the servers
     * @param taskQueue a queue of tasks to be handled by the writer runner
     */
    WriterRunner(SocketAddress[] serverAddresses,
                 BlockingQueue<SelectionKey> taskQueue) {
        this.replicationFactor = serverAddresses.length;
        this.serverAddresses = serverAddresses;
        this.serverSocketChannels = new SocketChannel[this.replicationFactor];
        this.writerAttachments = new WriterAttachment[this.replicationFactor];
        this.taskQueue = taskQueue;
    }

    /**
     * Check if there are still pending tasks from any of the servers
     * @return true, if and only if, there is still at least one pending task from at least one of the servers
     */
    private boolean hasPendingTasks() {
        for (WriterAttachment writerAttachment : this.writerAttachments) {
            if (writerAttachment.pendingTaskQueue.size() > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Start running the writer runner
     */
    @Override
    public void run() {
        WriterRunner.logger.debug("Writer starting with replication factor {}", this.replicationFactor);

        try (Selector serverSelector = Selector.open()) {
            // Establish connections to the servers and initialise write attachments
            for (int i = 0; i < this.replicationFactor; i++) {
                SocketChannel serverSocketChannel = SocketChannel.open();
                serverSocketChannel.configureBlocking(false);
                serverSocketChannel.setOption(StandardSocketOptions.SO_KEEPALIVE, true);
                serverSocketChannel.setOption(StandardSocketOptions.SO_LINGER, 5);
                serverSocketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
                serverSocketChannel.connect(this.serverAddresses[i]);
                while (!serverSocketChannel.finishConnect()) {
                    // Wait until the connection is finished
                }
                WriterAttachment writerAttachment = new WriterAttachment();
                serverSocketChannel.register(serverSelector, SelectionKey.OP_READ, writerAttachment);
                this.serverSocketChannels[i] = serverSocketChannel;
                this.writerAttachments[i] = writerAttachment;
                WriterRunner.logger.debug("Writer connected to server {}", this.serverAddresses[i].toString());
            }

            while (!Thread.interrupted()) {
                // Check if there are responses from the servers
                if (serverSelector.selectNow() > 0) {
                    Iterator<SelectionKey> serverSelectorIterator = serverSelector.selectedKeys().iterator();
                    while (serverSelectorIterator.hasNext()) {
                        SelectionKey serverSelectionKey = serverSelectorIterator.next();
                        serverSelectorIterator.remove();
                        if (serverSelectionKey.isReadable()) {
                            // Read and parse the response from the network buffer
                            SocketChannel serverSocketChannel = (SocketChannel) serverSelectionKey.channel();
                            WriterAttachment writerAttachment = (WriterAttachment) serverSelectionKey.attachment();

                            while (writerAttachment.buffer.hasRemaining()) {
                                int numBytes = serverSocketChannel.read(writerAttachment.buffer);
                                if (numBytes > 0) {
                                    // Only parse the response if there is more bytes read from the network buffer
                                    int i = 0;  // Start of a new response
                                    int j = 2;  // Start of the next new response
                                    byte[] bufferBytes = writerAttachment.buffer.array();
                                    writerAttachment.buffer.flip();
                                    int bufferLimit = writerAttachment.buffer.limit();
                                    while (j <= bufferLimit) {
                                        if (bufferBytes[j - 2] == 13 && bufferBytes[j - 1] == 10) {
                                            // End of a response \r\n found
                                            byte[] textLineBytes = Arrays.copyOfRange(bufferBytes, i, j);
                                            boolean failed = false;
                                            boolean missed = false;
                                            switch (new String(textLineBytes, StandardCharsets.US_ASCII)) {
                                                case "STORED\r\n":
                                                    // Successful set operation
                                                    // Protocol:
                                                    // STORED\r\n
                                                    break;
                                                case "DELETED\r\n":
                                                    // Successful delete operation
                                                    // Protocol:
                                                    // DELETED\r\n
                                                    break;
                                                case "NOT_FOUND\r\n":
                                                    // Cache missed for delete operation
                                                    // Protocol:
                                                    // NOT_FOUND
                                                    missed = true;
                                                    break;
                                                default:
                                                    // Other errors
                                                    // Protocol:
                                                    // ERROR\r\n
                                                    // CLIENT_ERROR <error>\r\n
                                                    // SERVER_ERROR <error>\r\n
                                                    failed = true;
                                                    break;
                                            }

                                            // Remove the request from the pending task queue of the server
                                            SelectionKey clientSelectionKey =
                                                    writerAttachment.pendingTaskQueue.remove();
                                            Attachment attachment = (Attachment) clientSelectionKey.attachment();
                                            // Decrement the pending response count
                                            attachment.numPendingResponse--;
                                            if (attachment.numPendingResponse == 0) {
                                                // Record the timestamp of when the server response has been received
                                                attachment.timestamps[4] = System.nanoTime();
                                            }
                                            if (failed) {
                                                // Always use the latest failure response
                                                attachment.failed = true;
                                                attachment.primaryBuffer.clear();
                                                attachment.primaryBuffer.put(textLineBytes);
                                            } else if (!attachment.failed) {
                                                // Only use the success response if no failure response was received
                                                // before
                                                attachment.primaryBuffer.clear();
                                                attachment.primaryBuffer.put(textLineBytes);
                                            }
                                            attachment.missed = missed || attachment.missed;

                                            if (attachment.numPendingResponse == 0) {
                                                // Prepare the attachment for server response
                                                attachment.stage = Stage.CLIENT_RESPONSE;
                                                attachment.primaryBuffer.flip();
                                                // Send the response back to the client
                                                SocketChannel clientSocketChannel =
                                                        (SocketChannel) clientSelectionKey.channel();
                                                while (attachment.primaryBuffer.hasRemaining()) {
                                                    clientSocketChannel.write(attachment.primaryBuffer);
                                                }

                                                // Record the timestamp of when the server response has been forwarded
                                                // to the client
                                                attachment.timestamps[5] = System.nanoTime();

                                                // Record the timestamp of completion
                                                attachment.completionTime = System.currentTimeMillis();

                                                if (attachment.command == Command.SET && attachment.id % 100 == 0) {
                                                    // Only log set requests with a sampling frequency of 100
                                                    String message = attachment.toString();
                                                    WriterRunner.logger.info(message);
                                                }

                                                // Re-register for the channel to the selector for the read events
                                                clientSelectionKey.interestOps(SelectionKey.OP_READ);
                                            }

                                            // Move over to the next response
                                            i = j;
                                            writerAttachment.buffer.position(j);
                                        }

                                        // Keep searching
                                        j++;
                                    }

                                    // Compact the buffer such that all parsed responses are discarded
                                    writerAttachment.buffer.compact();
                                } else if (numBytes == 0) {
                                    // Since the loop condition guarantees that there is still remaining space in the
                                    // buffer, a return value of zero means that there is no more data available in the
                                    // network buffer. Therefore, there is no need to further exam the buffer
                                    break;
                                } else {
                                    // The server has closed the socket
                                    throw new EndOfStreamException();
                                }
                            }
                        }
                    }
                }

                // Send another task to the servers
                SelectionKey clientSectionKey;
                if (this.hasPendingTasks()) {
                    // If there are pending tasks, using the non-blocking removal so that the writer can continue to
                    // check whether new responses from the servers are available
                    clientSectionKey = this.taskQueue.poll();
                } else {
                    // If there are no more pending tasks, use the blocking removal so that the writer can wait until
                    // a new task become available
                    clientSectionKey = this.taskQueue.take();
                }
                if (clientSectionKey != null) {
                    Attachment attachment = (Attachment) clientSectionKey.attachment();
                    // Record the timestamp of when the client request is dequeue and is about to be processed
                    attachment.timestamps[2] = System.nanoTime();
                    // Prepare the attachment for server request
                    attachment.stage = Stage.SERVER_REQUEST;
                    attachment.primaryBuffer.flip();
                    if (attachment.secondaryBuffer != null) {
                        attachment.secondaryBuffer.flip();
                    }
                    attachment.numPendingResponse = 0;
                    // Forward the request to all servers
                    for (int i = 0; i < this.replicationFactor; i++) {
                        while (attachment.primaryBuffer.hasRemaining()) {
                            this.serverSocketChannels[i].write(attachment.primaryBuffer);
                        }
                        attachment.primaryBuffer.position(0);

                        if (attachment.secondaryBuffer != null) {
                            while (attachment.secondaryBuffer.hasRemaining()) {
                                this.serverSocketChannels[i].write(attachment.secondaryBuffer);
                            }
                            attachment.secondaryBuffer.position(0);
                        }

                        // Add the task to the corresponding pending task queue
                        this.writerAttachments[i].pendingTaskQueue.add(clientSectionKey);
                        attachment.numPendingResponse++;
                    }

                    // Record the timestamp of when the client request has been forwarded to the server
                    attachment.timestamps[3] = System.nanoTime();

                    // Prepare the attachment for server response
                    attachment.stage = Stage.SERVER_RESPONSE;
                    attachment.failed = false;
                    attachment.missed = false;
                }
            }
        } catch (InterruptedException e) {
            WriterRunner.logger.debug("Writer stopping");
        } catch (EndOfStreamException e) {
            WriterRunner.logger.error("Writer reached end of stream from server", e);
            Middleware.workerError = true;
        } catch (IOException e) {
            WriterRunner.logger.error("Writer I/O error", e);
            Middleware.workerError = true;
        } finally {
            // Close the opening connections created by the writer
            for (int i = 0; i < this.replicationFactor; i++) {
                SocketChannel serverSocketChannel = this.serverSocketChannels[i];
                if (serverSocketChannel != null) {
                    try {
                        serverSocketChannel.close();
                        WriterRunner.logger.debug(
                                "Writer closed connection to server {}", this.serverAddresses[i].toString());
                    } catch (IOException e) {
                        WriterRunner.logger.error(
                                "Writer failed to close connection to server {}", this.serverAddresses[i].toString());
                    }
                }
            }

            // Check whether there is any new or pending tasks
            if (this.taskQueue.size() == 0) {
                WriterRunner.logger.debug("Writer terminates with empty task queue");
            } else {
                WriterRunner.logger.error("Writer terminates with unhandled tasks");
            }

            if (this.hasPendingTasks()) {
                WriterRunner.logger.error("Writer terminates with unfinished tasks");
            } else {
                WriterRunner.logger.debug("Writer terminates with no pending tasks");
            }
        }
    }
}
