/*
 * Copyright 2021 the original author or authors.
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
import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;

public class WatchableExecutionContext extends DelegatingExecutionContext {
    public WatchableExecutionContext(ExecutionContext delegate) {
        super(delegate);
    }

    private boolean hasNewMessages;

    public boolean hasNewMessages() {
        return hasNewMessages;
    }

    public void resetHasNewMessages() {
        this.hasNewMessages = false;
    }

    @Override
    public void putMessage(String key, @Nullable Object value) {
        hasNewMessages = true;
        super.putMessage(key, value);
    }

    public void putCycle(RecipeRunCycle<?> cycle) {
        super.putMessage(CURRENT_CYCLE, cycle);
    }
}
