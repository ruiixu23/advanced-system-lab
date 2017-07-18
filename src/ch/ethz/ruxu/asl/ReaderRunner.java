package ch.ethz.ruxu.asl;

import ch.ethz.ruxu.asl.exceptions.EndOfStreamException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.BlockingQueue;

class ReaderRunner implements Runnable {
    // The logger
    private static final Logger logger = LogManager.getLogger(ReaderRunner.class);
    // The selection key representing the connection from the client
    private SelectionKey clientSelectionKey;
    // The connection pool to the server
    private BlockingQueue<SocketChannel> serverConnectionPool;

    /**
     * Construct a new reader runner
     * @param clientSelectionKey the selection key representing the connection from the client
     * @param serverConnectionPool the connection pool to the server
     */
    ReaderRunner(SelectionKey clientSelectionKey,
                 BlockingQueue<SocketChannel> serverConnectionPool) {
        this.clientSelectionKey = clientSelectionKey;
        this.serverConnectionPool = serverConnectionPool;
    }

    /**
     * Return the server connection back to the connection pool
     * @param serverSocketChannel a socket channel to the server
     */
    private void returnConnection(SocketChannel serverSocketChannel) {
        this.serverConnectionPool.add(serverSocketChannel);
    }

    /**
     * Start running the reader runner
     */
    @Override
    public void run() {
        Attachment attachment = (Attachment) this.clientSelectionKey.attachment();
        // Record the timestamp of when the client request is dequeue and is about to be processed
        attachment.timestamps[2] = System.nanoTime();

        // Get a connection to the server
        SocketChannel serverSocketChannel;
        try {
            serverSocketChannel = this.serverConnectionPool.take();
        } catch (InterruptedException e) {
            ReaderRunner.logger.error("Reader interrupted while waiting for connection", e);
            Middleware.workerError = true;
            return;
        }

        // Prepare the attachment for server request
        attachment.stage = Stage.SERVER_REQUEST;
        try {
            // Forward the request to the server
            attachment.primaryBuffer.flip();
            while (attachment.primaryBuffer.hasRemaining()) {
                serverSocketChannel.write(attachment.primaryBuffer);
            }

            if (attachment.secondaryBuffer != null) {
                attachment.secondaryBuffer.flip();
                while (attachment.secondaryBuffer.hasRemaining()) {
                    serverSocketChannel.write(attachment.secondaryBuffer);
                }
            }
        } catch (IOException e) {
            this.returnConnection(serverSocketChannel);
            ReaderRunner.logger.error("Reader I/O error while sending request to server", e);
            Middleware.workerError = true;
            return;
        }

        // Record the timestamp of when the client request has been forwarded to the server
        attachment.timestamps[3] = System.nanoTime();

        // Prepare the attachment for server response
        attachment.stage = Stage.SERVER_RESPONSE;
        attachment.primaryBuffer.clear();
        attachment.secondaryBuffer = null;
        attachment.actualNumBytes = 0;
        attachment.expectedNumBytes = -1;
        attachment.failed = false;
        attachment.missed = false;
        try {
            // Read the response from the server
            int numBytes;
            while (attachment.actualNumBytes != attachment.expectedNumBytes) {
                if (attachment.secondaryBuffer == null) {
                    numBytes = serverSocketChannel.read(attachment.primaryBuffer);
                } else {
                    numBytes = (int) serverSocketChannel.read(
                            new ByteBuffer[]{attachment.primaryBuffer, attachment.secondaryBuffer});
                }

                if (numBytes > 0) {
                    attachment.actualNumBytes += numBytes;
                    int primaryBufferPosition = attachment.primaryBuffer.position();
                    if (attachment.expectedNumBytes == -1 && primaryBufferPosition >= 2) {
                        // The text line of the server response has not yet been parsed. By assumption, the primary
                        // buffer is big enough to hold the entire text line. Try to parse out the text line again
                        byte[] primaryBufferBytes = attachment.primaryBuffer.array();
                        for (int dataLineStart = 2; dataLineStart <= primaryBufferPosition; dataLineStart++) {
                            if (primaryBufferBytes[dataLineStart - 2] == 13
                                    && primaryBufferBytes[dataLineStart - 1] == 10) {
                                // End of the text line \r\n found
                                byte[] textLineBytes = Arrays.copyOfRange(primaryBufferBytes, 0, dataLineStart);
                                String[] textLineStringParts =
                                        new String(textLineBytes, StandardCharsets.US_ASCII).split("\\s+");
                                switch (textLineStringParts[0]) {
                                    case "END":
                                        // Read cache miss
                                        // Protocol:
                                        // END\r\n
                                        attachment.expectedNumBytes = dataLineStart;
                                        attachment.missed = true;
                                        break;
                                    case "VALUE":
                                        // Successful read operation
                                        // Protocol:
                                        // VALUE <key> <flags> <bytes> [<cas unique>]\r\n<data block>\r\nEND\r\n
                                        attachment.expectedNumBytes =
                                                dataLineStart + Integer.parseInt(textLineStringParts[3]) + 7;
                                        // Check if extra buffer needs to be allocated
                                        int secondaryBufferCapacity =
                                                attachment.expectedNumBytes - attachment.primaryBuffer.capacity();
                                        if (secondaryBufferCapacity > 0) {
                                            attachment.secondaryBuffer = ByteBuffer.allocate(secondaryBufferCapacity);
                                        }
                                        break;
                                    default:
                                        // Protocol:
                                        // ERROR\r\n
                                        // CLIENT_ERROR <error>\r\n
                                        // SERVER_ERROR <error>\r\n
                                        attachment.expectedNumBytes = dataLineStart;
                                        attachment.failed = true;
                                        break;
                                }
                                break;
                            }
                        }
                    }
                } else if (numBytes < 0) {
                    throw new EndOfStreamException();
                }
            }
        } catch (EndOfStreamException e) {
            this.returnConnection(serverSocketChannel);
            ReaderRunner.logger.error("Reader reached end of stream while reading response from the server", e);
            Middleware.workerError = true;
            return;
        } catch (IOException e) {
            this.returnConnection(serverSocketChannel);
            ReaderRunner.logger.error("Reader I/O error while reading response from the server", e);
            Middleware.workerError = true;
            return;
        }

        // Record the timestamp of when the server response has been received
        attachment.timestamps[4] = System.nanoTime();

        SocketChannel clientSocketChannel = (SocketChannel) this.clientSelectionKey.channel();
        // Prepare the attachment for server response
        attachment.stage = Stage.CLIENT_RESPONSE;
        try {
            // Forward the response to the client
            attachment.primaryBuffer.flip();
            while (attachment.primaryBuffer.hasRemaining()) {
                clientSocketChannel.write(attachment.primaryBuffer);
            }
            if (attachment.secondaryBuffer != null) {
                attachment.secondaryBuffer.flip();
                while (attachment.secondaryBuffer.hasRemaining()) {
                    clientSocketChannel.write(attachment.secondaryBuffer);
                }
            }
        } catch (IOException e) {
            this.returnConnection(serverSocketChannel);
            ReaderRunner.logger.error("Reader I/O error while sending response to the client", e);
            Middleware.workerError = true;
            return;
        }

        // Record the timestamp of when the server response has been forwarded to the client
        attachment.timestamps[5] = System.nanoTime();

        // Record the timestamp of completion
        attachment.completionTime = System.currentTimeMillis();

        if (attachment.id % 100 == 0) {
            // Only log with a sampling frequency of 100
            String message = attachment.toString();
            ReaderRunner.logger.info(message);
        }

        this.returnConnection(serverSocketChannel);

        // Re-register for the channel to the selector for the read events
        this.clientSelectionKey.interestOps(SelectionKey.OP_READ);
    }
}
