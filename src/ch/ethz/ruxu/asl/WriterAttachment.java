package ch.ethz.ruxu.asl;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.util.LinkedList;
import java.util.Queue;

class WriterAttachment {
    // The buffer to store response from the server
    ByteBuffer buffer;
    // A queue to store tasks whose response from the server is still pending
    Queue<SelectionKey> pendingTaskQueue;

    /**
     * Construct a new writer attachment
     */
    WriterAttachment() {
        this.buffer = ByteBuffer.allocate(2048);
        this.pendingTaskQueue = new LinkedList<>();
    }
}
