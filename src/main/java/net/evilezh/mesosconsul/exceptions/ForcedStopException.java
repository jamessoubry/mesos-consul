package net.evilezh.mesosconsul.exceptions;

public class ForcedStopException extends Exception {
    public ForcedStopException(String message) {
        super(message);
    }

    public ForcedStopException(String message, Throwable cause) {
        super(message, cause);
    }

    public ForcedStopException(Throwable cause) {
        super(cause);
    }
}
