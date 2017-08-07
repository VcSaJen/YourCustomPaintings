package com.vcsajen.yourcustompaintings.exceptions;

/**
 * Created by VcSaJen on 31.07.2017 16:54.
 */
public class ImageDimensionsExceedException extends Exception {
    public ImageDimensionsExceedException() {
        super("Image dimensions are too big!");
    }

    public ImageDimensionsExceedException(int W, int H, int maxW, int maxH) {
        super(String.format("Image dimensions are too big (%dx%d instead of max %dx%d)!", W, H, maxW, maxH));
    }

    public ImageDimensionsExceedException(String message) {
        super(message);
    }

    public ImageDimensionsExceedException(String message, Throwable cause) {
        super(message, cause);
    }

    public ImageDimensionsExceedException(Throwable cause) {
        super(cause);
    }
}
