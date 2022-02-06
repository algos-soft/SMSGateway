package it.algos.smsgateway.exceptions;

public class InvalidSmsException  extends Exception {

    public InvalidSmsException(Throwable cause) {
        super(cause);
    }

    public InvalidSmsException(String message) {
        super(message);
    }

    public InvalidSmsException(String message, Throwable cause) {
        super(message, cause);
    }
}
