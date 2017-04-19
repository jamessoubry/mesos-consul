package net.evilezh.mesosconsul.exceptions;

public class TransformTypeException extends Exception {
    public TransformTypeException(String message) {
        super(message);
    }

    public TransformTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransformTypeException(Throwable cause) {
        super(cause);
    }
}
