package alexiil.mc.lib.net;

import java.io.IOException;

public class InvalidInputDataException extends IOException {
    private static final long serialVersionUID = 1450919830268637665L;

    public InvalidInputDataException() {}

    public InvalidInputDataException(String message) {
        super(message);
    }

    public InvalidInputDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
