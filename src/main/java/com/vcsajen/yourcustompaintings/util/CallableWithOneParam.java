package com.vcsajen.yourcustompaintings.util;

import java.util.concurrent.Callable;
import java.util.function.Function;

/**
 * Created by VcSaJen on 27.07.2017 18:59.
 */
public class CallableWithOneParam<T,R> implements Callable<R> {
    Function<T,R> function;
    T value;

    public CallableWithOneParam(T value, Function<T,R> function)
    {
        this.function = function;
        this.value = value;
    }

    @Override
    public R call() throws Exception {
        return function.apply(value);
    }
}
