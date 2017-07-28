package com.vcsajen.yourcustompaintings.exceptions;

/**
 * Created by VcSaJen on 28.07.2017 12:07.
 */
public class ImageSizeLimitExceededException extends Exception {
    public ImageSizeLimitExceededException() {
        super("Image file size is too big!");
    }

    public ImageSizeLimitExceededException(String message) {
        super(message);
    }

    public ImageSizeLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }

    public ImageSizeLimitExceededException(Throwable cause) {
        super(cause);
    }
}
