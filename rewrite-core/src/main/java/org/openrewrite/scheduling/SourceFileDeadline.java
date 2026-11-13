/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.scheduling;

import org.jspecify.annotations.Nullable;
import org.openrewrite.*;

import java.time.Duration;

/**
 * The {@link ExecutionContext#SOURCE_FILE_TIMEOUT} deadline of the recipe currently scanning or editing a source
 * file on this thread. {@link TreeVisitor} checks it periodically; long-running work that doesn't visit trees
 * can call {@link #check()} to be abandoned cooperatively as well.
 */
@Incubating(since = "8.93.0")
public final class SourceFileDeadline implements AutoCloseable {
    private static final ThreadLocal<@Nullable SourceFileDeadline> CURRENT = new ThreadLocal<>();

    // Plain and never cleared, so check() is nearly free until a deadline is used. Each thread writes it on its
    // first start(), as only its own write is guaranteed visible to it, and writing on every start() contends.
    private static boolean everStarted;
    private static final ThreadLocal<@Nullable Boolean> STARTED_ON_THREAD = new ThreadLocal<>();

    private final Recipe recipe;
    private final SourceFile sourceFile;
    private final long startNanos = System.nanoTime();
    private final long timeoutNanos;
    private final @Nullable SourceFileDeadline previous = CURRENT.get();

    // Stays set even if the recipe catches the exception thrown by check()
    private boolean abandoned;

    private SourceFileDeadline(Recipe recipe, SourceFile sourceFile, long timeoutNanos) {
        this.recipe = recipe;
        this.sourceFile = sourceFile;
        this.timeoutNanos = timeoutNanos;
    }

    static SourceFileDeadline start(Recipe recipe, SourceFile sourceFile, long timeoutNanos) {
        SourceFileDeadline deadline = new SourceFileDeadline(recipe, sourceFile, timeoutNanos);
        if (STARTED_ON_THREAD.get() == null) {
            STARTED_ON_THREAD.set(true);
            everStarted = true;
        }
        CURRENT.set(deadline);
        return deadline;
    }

    /**
     * @throws SourceFileTimeoutException if the deadline in effect on this thread has passed.
     */
    public static void check() {
        if (everStarted) {
            checkCurrent();
        }
    }

    private static void checkCurrent() {
        SourceFileDeadline deadline = CURRENT.get();
        if (deadline != null && System.nanoTime() - deadline.startNanos > deadline.timeoutNanos) {
            deadline.abandoned = true;
            throw deadline.timeout();
        }
    }

    boolean isAbandoned() {
        return abandoned;
    }

    void throwIfAbandoned() {
        if (abandoned) {
            throw timeout();
        }
    }

    private SourceFileTimeoutException timeout() {
        return new SourceFileTimeoutException(recipe, sourceFile.getSourcePath(), Duration.ofNanos(timeoutNanos));
    }

    @Override
    public void close() {
        if (previous == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(previous);
        }
    }
}
