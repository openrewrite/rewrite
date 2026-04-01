/*
 * Copyright 2025 the original author or authors.
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

public class DataTableExecutionContextView extends DelegatingExecutionContext {
    public static final String DATA_TABLE_STORE = "org.openrewrite.dataTables.store";

    private DataTableExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static DataTableExecutionContextView view(ExecutionContext ctx) {
        if (ctx instanceof DataTableExecutionContextView) {
            return (DataTableExecutionContextView) ctx;
        }
        return new DataTableExecutionContextView(ctx);
    }

    public DataTableExecutionContextView setDataTableStore(DataTableStore store) {
        putMessage(DATA_TABLE_STORE, store);
        return this;
    }

    public DataTableStore getDataTableStore() {
        DataTableStore store = getMessage(DATA_TABLE_STORE);
        if (store == null) {
            store = new InMemoryDataTableStore();
            putMessage(DATA_TABLE_STORE, store);
        }
        return store;
    }
}
