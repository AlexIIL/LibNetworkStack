package alexiil.mc.lib.net;

public class InvalidInputDataException extends Exception {

    public InvalidInputDataException() {}

    public InvalidInputDataException(String message) {
        super(message);
    }

    public InvalidInputDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
