/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.scheduling.WorkingDirectoryExecutionContextView;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.Collections.unmodifiableMap;
import static org.openrewrite.Recipe.PANIC;

public class CursorValidatingExecutionContextView extends DelegatingExecutionContext {
    private static final String VALIDATE_CURSOR_ACYCLIC = "org.openrewrite.CursorValidatingExecutionContextView.ValidateCursorAcyclic";
    private static final String VALIDATE_CTX_MUTATION = "org.openrewrite.CursorValidatingExecutionContextView.ValidateExecutionContextImmutability";

    public CursorValidatingExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static CursorValidatingExecutionContextView view(ExecutionContext ctx) {
        if (ctx instanceof CursorValidatingExecutionContextView) {
            return (CursorValidatingExecutionContextView) ctx;
        }
        return new CursorValidatingExecutionContextView(ctx);
    }

    public boolean getValidateCursorAcyclic() {
        return getMessage(VALIDATE_CURSOR_ACYCLIC, false);
    }

    @SuppressWarnings("UnusedReturnValue")
    public CursorValidatingExecutionContextView setValidateCursorAcyclic(boolean validateCursorAcyclic) {
        putMessage(VALIDATE_CURSOR_ACYCLIC, validateCursorAcyclic);
        return this;
    }

    public CursorValidatingExecutionContextView setValidateImmutableExecutionContext(boolean allowExecutionContextMutation) {
        putMessage(VALIDATE_CTX_MUTATION, allowExecutionContextMutation);
        return this;
    }

    @Override
    public Map<String, @Nullable Object> getMessages() {
        Map<String, @Nullable Object> messages = super.getMessages();
        return getMessage(VALIDATE_CTX_MUTATION, false) ? new ValidatingMessages(messages) : messages;
    }

    @Override
    public void putMessage(String key, @Nullable Object value) {
        assertMutationAllowed(key);
        super.putMessage(key, value);
    }

    @Override
    public <T> T computeMessageIfAbsent(String key, Function<? super String, ? extends T> defaultValue) {
        return super.computeMessageIfAbsent(key, k -> {
            assertMutationAllowed(k);
            return defaultValue.apply(k);
        });
    }

    private void assertMutationAllowed(String key) {
        boolean mutationAllowed =
                !getMessage(VALIDATE_CTX_MUTATION, false) ||
                VALIDATE_CURSOR_ACYCLIC.equals(key) ||
                VALIDATE_CTX_MUTATION.equals(key) ||
                PANIC.equals(key) ||
                ExecutionContext.CURRENT_CYCLE.equals(key) ||
                ExecutionContext.CURRENT_RECIPE.equals(key) ||
                DataTableExecutionContextView.DATA_TABLE_STORE.equals(key) ||
                WorkingDirectoryExecutionContextView.WORKING_DIRECTORY_ROOT.equals(key) ||
                ExecutionContext.REQUIRE_PRINT_EQUALS_INPUT.equals(key) ||
                key.startsWith(Singleton.class.getName()) ||
                "org.openrewrite.python.liveDepsTrees".equals(key) ||
                "org.openrewrite.javascript.livePackageJsonTrees".equals(key) ||
                "org.openrewrite.javascript.registryClient".equals(key) ||
                key.startsWith("org.openrewrite.maven") // MavenExecutionContextView stores metrics
                || key.startsWith("io.moderne"); // We ought to know what we're doing
        assert mutationAllowed : "Recipe mutated execution context key \"" + key + "\". " +
                "Recipes should not mutate the contents of the ExecutionContext as it allows mutable state to leak between " +
                "recipes, opening the door for difficult to debug recipe composition errors. " +
                "If you need to store state within the execution of a single recipe use Cursor messaging. " +
                "If you want to pass state between recipes, use a ScanningRecipe instead.";
    }

    private class ValidatingMessages extends AbstractMap<String, @Nullable Object> {
        private final Map<String, @Nullable Object> messages;

        ValidatingMessages(Map<String, @Nullable Object> messages) {
            this.messages = messages;
        }

        @Override
        public Set<Entry<String, @Nullable Object>> entrySet() {
            return unmodifiableMap(messages).entrySet();
        }

        @Override
        public @Nullable Object get(Object key) {
            return messages.get(key);
        }

        @Override
        public boolean containsKey(Object key) {
            return messages.containsKey(key);
        }

        @Override
        public @Nullable Object put(String key, @Nullable Object value) {
            assertMutationAllowed(key);
            return messages.put(key, value);
        }

        @Override
        public @Nullable Object remove(Object key) {
            assertMutationAllowed(String.valueOf(key));
            return messages.remove(key);
        }
    }
}
