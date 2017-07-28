package com.vcsajen.yourcustompaintings.util;

import java.util.function.Consumer;

/**
 * Created by VcSaJen on 27.07.2017 19:00.
 */
public class RunnableWithOneParam<T> implements Runnable {
    Consumer<T> consumer;
    T value;

    public RunnableWithOneParam(T value, Consumer<T> consumer)
    {
        this.consumer = consumer;
        this.value = value;
    }

    @Override
    public void run() {
        consumer.accept(value);
    }
}
