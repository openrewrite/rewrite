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
     * Used to decide which of a project's own directly-requested dependencies seed the resolution of a given
     * target scope. Unlike {@link #transitiveOf}/{@link #isInClasspathOf}, which govern how a dependency's scope
     * degrades one hop further down its own transitive dependencies, this answers a single-hop question: does a
     * dependency declared with this scope belong in the dependency set being assembled for the given target scope.
     * <p>
     * There are only three real classpaths - compile, runtime, and test - "provided" is not itself a classpath,
     * but a scope that contributes to the compile and test classpaths while being withheld from the runtime
     * classpath. This matches the scope table at
     * <a href="https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#dependency-scope">Dependency Scope</a>.
     * <p>
     * this (rows) x target scope (columns):
     * <pre>
     *           Compile   Provided   Runtime   Test
     * Compile:    T         F          T         T
     * Provided:   T         T          F         T
     * Runtime:    F         F          T         T
     * Test:       F         F          F         T
     * </pre>
     *
     * @param scope The target scope whose dependency set is being assembled.
     * @return If a directly-requested dependency declared with this scope belongs in that target scope's
     * dependency set.
     */
    public boolean isDirectlyIncludedIn(Scope scope) {
        if (this == scope) {
            return true;
        }
        switch (this) {
            case Compile:
                return scope == Runtime || scope == Test;
            case Provided:
                return scope == Compile || scope == Test;
            case Runtime:
                return scope == Test;
            default:
                return false;
        }
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
