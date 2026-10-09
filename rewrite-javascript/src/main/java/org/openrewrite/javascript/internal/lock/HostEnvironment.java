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
package org.openrewrite.javascript.internal.lock;

import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.Map;

/**
 * The environment a package manager runs in, as far as the host knows it: its own variables count as unset unless the
 * host says otherwise, and a config file's {@code ${VAR}} is cannot-know where the host does not know it.
 */
public final class HostEnvironment {

    public static final HostEnvironment UNKNOWN = new HostEnvironment(Collections.emptyMap(), false);

    private final Map<String, String> variables;
    private final boolean complete;

    private HostEnvironment(Map<String, String> variables, boolean complete) {
        this.variables = variables;
        this.complete = complete;
    }

    /**
     * @param variables the process environment; null when the host does not know it
     */
    public static HostEnvironment of(@Nullable Map<String, String> variables) {
        return variables == null ? UNKNOWN : new HostEnvironment(variables, true);
    }

    /**
     * The variables known, with those a project's file sets (Bun's .env files) beside the process's.
     */
    public HostEnvironment with(Map<String, String> known) {
        return new HostEnvironment(known, complete);
    }

    public Map<String, String> known() {
        return variables;
    }

    /**
     * A variable the package manager reads by its own name ({@code BUN_CONFIG_REGISTRY}, {@code YARN_*}).
     */
    public @Nullable String setting(String name) {
        return variables.get(name);
    }

    /**
     * A variable a config file names ({@code ${NAME}}), whose value reaches the lock.
     * @throws CannotKnow when the host does not know the environment and nothing else sets the variable
     */
    public @Nullable String reference(String name) {
        String value = variables.get(name);
        if (value == null && !complete) {
            throw new CannotKnow("the environment variable " + name + " a config file names (the host does not " +
                                 "know the environment)");
        }
        return value;
    }
}
