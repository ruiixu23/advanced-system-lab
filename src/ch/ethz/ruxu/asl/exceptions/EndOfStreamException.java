package ch.ethz.ruxu.asl.exceptions;

public class EndOfStreamException extends Exception {
    private static final String DEFAULT_MESSAGE = "End of stream reached";

    public EndOfStreamException() {
        super(EndOfStreamException.DEFAULT_MESSAGE);
    }

    public EndOfStreamException(String message) {
        super(message);
    }

    public EndOfStreamException(Throwable cause) {
        super(EndOfStreamException.DEFAULT_MESSAGE);
    }

    public EndOfStreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
