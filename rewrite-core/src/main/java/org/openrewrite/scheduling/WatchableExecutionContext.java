package org.openrewrite.scheduling;

import org.openrewrite.ExecutionContext;
import org.openrewrite.internal.lang.Nullable;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class WatchableExecutionContext implements ExecutionContext {

    private final ExecutionContext delegate;

    private boolean hasNewMessages = false;

    public WatchableExecutionContext(ExecutionContext delegate) {
        this.delegate = delegate;
    }

    public boolean hasNewMessages() {
        return hasNewMessages;
    }

    public void resetHasNewMessages() {
        this.hasNewMessages = false;
    }

    @Override
    public void putMessage(String key, Object value) {
        hasNewMessages = true;
        delegate.putMessage(key, value);
    }

    @Override
    public <T> @Nullable T getMessage(String key) {
        return delegate.getMessage(key);
    }

    @Override
    public <T> @Nullable T pollMessage(String key) {
        return delegate.pollMessage(key);
    }

    @Override
    public Consumer<Throwable> getOnError() {
        return delegate.getOnError();
    }

    @Override
    public BiConsumer<Throwable, ExecutionContext> getOnTimeout() {
        return delegate.getOnTimeout();
    }

    @Override
    public Duration getRunTimeout(int inputs) {
        return delegate.getRunTimeout(inputs);
    }
}
