/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.internal;

import org.openrewrite.DelegatingExecutionContext;
import org.openrewrite.ExecutionContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Typed view over {@link ExecutionContext} for shared state used by
 * Python dependency management recipes.
 * <p>
 * Lock file state is shared across all recipes in the same execution so that
 * sequential recipes in a composite correctly build on each other's lock
 * regeneration results.
 */
public class PythonDependencyExecutionContextView extends DelegatingExecutionContext {

    private static final String UPDATED_LOCK_FILES = "org.openrewrite.python.updatedLockFiles";
    private static final String EXISTING_LOCK_CONTENTS = "org.openrewrite.python.existingLockContents";

    private PythonDependencyExecutionContextView(ExecutionContext delegate) {
        super(delegate);
    }

    public static PythonDependencyExecutionContextView view(ExecutionContext ctx) {
        if (ctx instanceof PythonDependencyExecutionContextView) {
            return (PythonDependencyExecutionContextView) ctx;
        }
        return new PythonDependencyExecutionContextView(ctx);
    }

    /**
     * Regenerated uv.lock contents keyed by pyproject.toml source path.
     * Updated by recipe visitors after lock regeneration so that subsequent
     * recipes seed with the latest lock content.
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getUpdatedLockFiles() {
        return computeMessageIfAbsent(UPDATED_LOCK_FILES, k -> new HashMap<String, String>());
    }

    /**
     * Existing uv.lock contents keyed by pyproject.toml source path.
     * Populated during the scanning phase from on-disk lock files and updated
     * after each lock regeneration so that the next recipe seeds with the
     * latest lock content.
     */
    @SuppressWarnings("unchecked")
    public Map<String, String> getExistingLockContents() {
        return computeMessageIfAbsent(EXISTING_LOCK_CONTENTS, k -> new HashMap<String, String>());
    }
}
