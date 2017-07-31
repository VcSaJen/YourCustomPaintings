package com.vcsajen.yourcustompaintings.exceptions;

/**
 * Created by VcSaJen on 31.07.2017 16:54.
 */
public class MapIdLimitExceedException extends Exception {
    public MapIdLimitExceedException() {
        super("Image file size is too big!");
    }

    public MapIdLimitExceedException(String message) {
        super(message);
    }

    public MapIdLimitExceedException(String message, Throwable cause) {
        super(message, cause);
    }

    public MapIdLimitExceedException(Throwable cause) {
        super(cause);
    }
}
