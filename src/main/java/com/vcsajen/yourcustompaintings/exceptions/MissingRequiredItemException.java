package com.vcsajen.yourcustompaintings.exceptions;

import org.spongepowered.api.item.inventory.ItemStack;

/**
 * Created by VcSaJen on 15.08.2017 16:04.
 */
public class MissingRequiredItemException extends Exception {
    public MissingRequiredItemException() {
        super("Missing required item!");
    }

    public MissingRequiredItemException(ItemStack itemType, int count) {
        super("Missing required item(s)! ("+count+"x"+itemType.getType().getName()+")");
    }

    public MissingRequiredItemException(String message) {
        super(message);
    }

    public MissingRequiredItemException(String message, Throwable cause) {
        super(message, cause);
    }

    public MissingRequiredItemException(Throwable cause) {
        super(cause);
    }
}
