package net.evilezh.mesosconsul.exceptions;

public class ConsulNodeNotFoundException extends Exception {
    public ConsulNodeNotFoundException(String message) {
        super(message);
    }

    public ConsulNodeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConsulNodeNotFoundException(Throwable cause) {
        super(cause);
    }
}
