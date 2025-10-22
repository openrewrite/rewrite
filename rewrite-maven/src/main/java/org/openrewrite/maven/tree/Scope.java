/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven.tree;

import org.jspecify.annotations.Nullable;

public enum Scope {
    None, // the root of a resolution tree
    Compile,
    Provided,
    Runtime,
    Test,
    System,
    Import,
    Invalid;

    /**
     * @param scope The scope to test
     * @return If a dependency in this scope would be in the classpath of the tested scope.
     */
    public boolean isInClasspathOf(@Nullable Scope scope) {
        return this.transitiveOf(scope) != null;
    }

    /**
     * See the table at <a href="Dependency Scope">https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#dependency-scope</a>.
     * <code>this</code> represents the scope on the top row of the table.
     *
     * @param scope The scope on the left column of the table.
     * @return The scope inside the table.
     */
    public @Nullable Scope transitiveOf(@Nullable Scope scope) {
        if (scope == null) {
            return this;
        }

        switch (scope) {
            case None:
                return this;
            case Compile:
                switch (this) {
                    case Compile:
                        return Compile;
                    case Runtime:
                        return Runtime;
                    case Provided:
                    case Test:
                    default:
                        return null;
                }
            case Provided:
                switch (this) {
                    case Compile:
                    case Runtime:
                        return Provided;
                    case Provided:
                    case Test:
                    default:
                        return null;
                }
            case Runtime:
                switch (this) {
                    case Compile:
                    case Runtime:
                        return Runtime;
                    case Provided:
                    case Test:
                    default:
                        return null;
                }
            case Test:
                switch (this) {
                    case Compile:
                    case Runtime:
                        return Test;
                    case Provided:
                    case Test:
                    default:
                        return null;
                }
            default:
                return null;
        }
    }

    public static Scope fromName(@Nullable String scope) {
        if (scope == null) {
            return Compile;
        }
        switch (scope.toLowerCase()) {
            case "compile":
                return Compile;
            case "provided":
                return Provided;
            case "runtime":
                return Runtime;
            case "test":
                return Test;
            case "system":
                return System;
            case "import":
                return Import;
            default:
                return Invalid;
        }
    }

    /**
     * Give two scopes, returns the scope with the highest precedence.
     *
     * @return Scope with the higher precedence.
     */
    public static @Nullable Scope maxPrecedence(@Nullable Scope scope1, @Nullable Scope scope2) {
        if (scope1 == null) {
            return scope2;
        } else if (scope2 == null) {
            return scope1;
        } else if (scope1.ordinal() <= scope2.ordinal()) {
            return scope1;
        } else {
            return scope2;
        }
    }

    /**
     * There are more, and more complex, gradle dependency configurations than there are maven scopes.
     * <p/>
     * In Maven you place a dependency that should be available at runtime but not at compile time into the "runtime" scope.
     * In Gradle you place a dependency that should be available at runtime but not at compile time into the "runtimeOnly" configuration.
     * In this situation you should use {@link Scope#asConsumableGradleConfigurationName}.
     * <p/>
     * In Maven you read the runtime classpath from the "runtime" scope.
     * In Gradle you read the runtime classpath from the "runtimeClasspath" configuration.
     * In this situation you should use {@link Scope#asResolvableGradleConfigurationName}.
     * <p/>
     * This method is deprecated because its name is ambiguous about which of these possible mappings it is referring to.
     */
    @Deprecated
    public static @Nullable String asGradleConfigurationName(@Nullable Scope scope) {
        return asConsumableGradleConfigurationName(scope);
    }

    /**
     * Use when you want to know which Gradle configuration you should *add* an entry to in order to most closely match a given Maven scope.
     */
    public static @Nullable String asConsumableGradleConfigurationName(@Nullable Scope scope) {
        if(scope == null) {
            return null;
        }
        switch (scope) {
            case Compile:
                return "implementation";
            case Provided:
                return "compileOnly";
            case Runtime:
                return "runtimeOnly";
            case Test:
                return "testImplementation";
            case System:
                return "system";
            default:
                return null;
        }
    }

    /**
     * Use when you want to know which Gradle configuration you should *read* to get contents similar to a given Maven scope.
     */
    @SuppressWarnings("unused")
    public static @Nullable String asResolvableGradleConfigurationName(@Nullable Scope scope) {
        if(scope == null) {
            return null;
        }
        switch (scope) {
            case Compile:
                return "compileClasspath";
            case Provided:
                return "compileOnly";
            case Runtime:
                return "runtimeClasspath";
            case Test:
                return "testRuntimeClasspath";
            case System:
                return "system";
            default:
                return null;
        }
    }
}
