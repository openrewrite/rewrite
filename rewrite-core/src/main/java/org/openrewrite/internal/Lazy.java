package org.openrewrite.internal;

import org.openrewrite.internal.lang.Nullable;

import java.util.function.Supplier;

public class Lazy<T> {

    @Nullable
    T initialized;

    Supplier<T> supplier;

    public Lazy(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Nullable
    public T get() {
        if (initialized == null) {
            initialized = supplier.get();
        }
        return initialized;
    }
}
