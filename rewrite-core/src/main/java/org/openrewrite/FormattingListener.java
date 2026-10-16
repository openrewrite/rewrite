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
package org.openrewrite;

import org.jspecify.annotations.Nullable;

/**
 * Hears about each request to auto-format a tree made on the thread it is installed on, so that a test harness can
 * check that a recipe formats only what it changed.
 */
@Incubating(since = "8.94.0")
public abstract class FormattingListener implements AutoCloseable {
    private static final ThreadLocal<@Nullable FormattingListener> CURRENT = new ThreadLocal<>();

    // Plain and never cleared, so formatters pay next to nothing until a listener is installed
    private static boolean everInstalled;

    private @Nullable FormattingListener previous;

    /**
     * Formatters call this before they auto-format {@code tree}, stopping after {@code stopAfter} when it is not null.
     */
    public static void beforeFormat(Tree tree, @Nullable Tree stopAfter, Cursor cursor) {
        if (everInstalled) {
            FormattingListener listener = CURRENT.get();
            if (listener != null) {
                listener.onFormat(tree, stopAfter, cursor);
            }
        }
    }

    protected abstract void onFormat(Tree tree, @Nullable Tree stopAfter, Cursor cursor);

    /**
     * Listens to formatting on this thread until {@link #close()}.
     */
    public FormattingListener install() {
        previous = CURRENT.get();
        everInstalled = true;
        CURRENT.set(this);
        return this;
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
