package ch.ethz.ruxu.asl;

import java.nio.ByteBuffer;

class Attachment {
    // An array of timestamps used for experimental instrumentation
    // 1st element: the client request is about to be read from the network buffer
    // 2nd element: the client request is queued for processing
    // 3rd element: the client request is dequeue and is about to be processed
    // 4th element: the client request has been forwarded to the server
    // 5th element: the server response has been received
    // 6th element: the server response has been forwarded to the client
    long timestamps[];
    // The UNIX timestamp in millisecond indicating when the request is complete
    long completionTime;
    // An optional id for the request for logging purposes
    long id;
    // The processing stage of the request
    Stage stage;
    // The primary buffer used to store the request payload
    ByteBuffer primaryBuffer;
    // An optional secondary buffer used to store the request payload if the primary buffer overflows
    ByteBuffer secondaryBuffer;
    // Actual number of bytes that have been read from the network buffer
    int actualNumBytes;
    // Expected number of bytes to be read from the network buffer
    int expectedNumBytes;
    // The command of the request, i.e. get, set, or delete
    Command command;
    // The key of the request
    byte[] key;
    // The id of the primary server
    int serverId;
    // The number of pending requests for set and delete requests
    int numPendingResponse;
    // Indicate whether the request is successfully or not
    boolean failed;
    // Indicate whether a cache missed happens
    boolean missed;

    /**
     * Constructor to create a new attachment
     */
    Attachment() {
        this.timestamps = new long[6];
        this.completionTime = 0;
        this.id = 0;
        this.stage = null;
        this.primaryBuffer = ByteBuffer.allocate(2048);
        this.secondaryBuffer = null;
        this.actualNumBytes = 0;
        this.expectedNumBytes = -1;
        this.command = null;
        this.key = null;
        this.serverId = -1;
        this.numPendingResponse = 0;
        this.failed = false;
        this.missed = false;
    }

    @Override
    public String toString() {
        return String.format("%d,%s,%d,%d,%c,%c,%d,%d,%d,%d,%d",
                this.completionTime,
                this.command.toString(),
                this.id,
                this.serverId,
                this.failed ? 'f':'s',
                this.missed ? 'm':'h',
                this.timestamps[1] - this.timestamps[0],
                this.timestamps[2] - this.timestamps[1],
                this.timestamps[3] - this.timestamps[2],
                this.timestamps[4] - this.timestamps[3],
                this.timestamps[5] - this.timestamps[4]);
    }
}
