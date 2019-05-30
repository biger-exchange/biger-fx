package com.biger.fx;

public class BigerFXException extends RuntimeException {
    public BigerFXException() {
    }

    public BigerFXException(String message) {
        super(message);
    }

    public BigerFXException(String message, Throwable cause) {
        super(message, cause);
    }

    public BigerFXException(Throwable cause) {
        super(cause);
    }

    public BigerFXException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
