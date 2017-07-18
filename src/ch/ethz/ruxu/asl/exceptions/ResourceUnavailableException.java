package ch.ethz.ruxu.asl.exceptions;

public class ResourceUnavailableException extends Exception {
    private static final String DEFAULT_MESSAGE = "Resource not available";

    public ResourceUnavailableException() {
        super(ResourceUnavailableException.DEFAULT_MESSAGE);
    }

    public ResourceUnavailableException(String message) {
        super(message);
    }

    public ResourceUnavailableException(Throwable cause) {
        super(ResourceUnavailableException.DEFAULT_MESSAGE);
    }

    public ResourceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
