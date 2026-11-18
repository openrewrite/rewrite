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
package org.openrewrite.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.DataTableExecutionContextView;
import org.openrewrite.DataTableStore;
import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;

/**
 * An ExecutionContext view that suppresses data table row insertion.
 * Installs a no-op {@link DataTableStore} that silently drops inserts
 * while delegating reads to the real store.
 */
public class DataTableSuppressingExecutionContextView extends DelegatingExecutionContext {
    private DataTableSuppressingExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static DataTableSuppressingExecutionContextView view(ExecutionContext ctx) {
        if (ctx instanceof DataTableSuppressingExecutionContextView) {
            return (DataTableSuppressingExecutionContextView) ctx;
        }
        return new DataTableSuppressingExecutionContextView(ctx);
    }

    /**
     * @return Whether data table rows written through {@code ctx} are dropped, also when {@code ctx} wraps a view.
     */
    public static boolean isSuppressing(ExecutionContext ctx) {
        return ctx.getMessage(DataTableExecutionContextView.DATA_TABLE_STORE) == DataTableStore.noop();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> @Nullable T getMessage(String key) {
        if (DataTableExecutionContextView.DATA_TABLE_STORE.equals(key)) {
            return (T) DataTableStore.noop();
        }
        return super.getMessage(key);
    }
}
