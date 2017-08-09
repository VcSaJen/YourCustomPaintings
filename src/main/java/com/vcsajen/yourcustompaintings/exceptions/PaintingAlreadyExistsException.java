package com.vcsajen.yourcustompaintings.exceptions;

/**
 * Created by VcSaJen on 08.08.2017 15:09.
 */
public class PaintingAlreadyExistsException extends Exception {
    public PaintingAlreadyExistsException() {
        super("Painting with that name already exists for given player!");
    }

    public PaintingAlreadyExistsException(String message) {
        super(message);
    }

    public PaintingAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }

    public PaintingAlreadyExistsException(Throwable cause) {
        super(cause);
    }
}
