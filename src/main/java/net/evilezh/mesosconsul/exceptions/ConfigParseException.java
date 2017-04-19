package net.evilezh.mesosconsul.exceptions;

public class ConfigParseException extends Exception {
    public ConfigParseException(String message) {
        super(message);
    }

    public ConfigParseException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigParseException(Throwable cause) {
        super(cause);
    }
}
