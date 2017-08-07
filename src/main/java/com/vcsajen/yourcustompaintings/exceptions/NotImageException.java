package com.vcsajen.yourcustompaintings.exceptions;

/**
 * Created by VcSaJen on 07.08.2017 2:36.
 */
public class NotImageException extends Exception {
    public NotImageException() {
        super("Not an image or unsupported format!");
    }

    public NotImageException(String message) {
        super(message);
    }

    public NotImageException(String message, Throwable cause) {
        super(message, cause);
    }

    public NotImageException(Throwable cause) {
        super(cause);
    }
}
