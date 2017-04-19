package net.evilezh.mesosconsul.exceptions;

public class TransformExistException extends Exception {
    public TransformExistException(String message) {
        super(message);
    }

    public TransformExistException(String message, Throwable cause) {
        super(message, cause);
    }

    public TransformExistException(Throwable cause) {
        super(cause);
    }
}
