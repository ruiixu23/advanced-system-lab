package ch.ethz.ruxu.asl.exceptions;

public class BadRequestException extends Exception {
    private static final String DEFAULT_MESSAGE = "Resource not available";

    public BadRequestException() {
        super(BadRequestException.DEFAULT_MESSAGE);
    }

    public BadRequestException(String message) {
        super(message);
    }

    public BadRequestException(Throwable cause) {
        super(BadRequestException.DEFAULT_MESSAGE);
    }

    public BadRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
