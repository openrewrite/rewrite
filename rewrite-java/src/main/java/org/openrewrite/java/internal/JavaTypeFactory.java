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
package org.openrewrite.java.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.Incubating;
import org.openrewrite.java.tree.JavaType;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Consumer;

/**
 * Factory for constructing {@link JavaType} instances during parsing.
 * <p>
 * Provides cache lookup ({@link #get}) and registration ({@link #put}) so that
 * TypeMapping classes need only a single dependency (the factory) rather than
 * both factory and cache. Partition-backed implementations can override {@link #get}
 * to return proxy types from pre-built partitions.
 * <p>
 * The construction surface is the {@code compute*} family. Each
 * {@code computeXxx(signature, ..., initializer)} returns the cached instance
 * if one already exists, otherwise allocates a stub, registers it in the cache
 * <em>before</em> invoking the initializer, then runs the initializer to fill
 * the stub's fields. Registering the stub up front is what makes recursive
 * resolution during the initializer safe: a recursive lookup for the same
 * signature finds the stub instead of looping. The initializer typically ends
 * by calling {@code unsafeSet} on the stub to populate its fields.
 */
@Incubating(since = "8.82.0")
public interface JavaTypeFactory {

    /**
     * Creates a {@link JavaTypeFactory} for a given classpath context.
     * <p>
     * The provider owns cache creation internally &mdash; callers don't configure
     * {@link org.openrewrite.java.internal.JavaTypeCache} separately. Partition-backed
     * implementations use the classpath to determine which pre-built partitions to load.
     */
    @FunctionalInterface
    interface Provider {
        JavaTypeFactory create(List<Path> classpath, @Nullable Path jdkHome);
    }

    /**
     * Look up a type by signature/key. Returns null on cache miss.
     * Partition-backed implementations may return proxy types from pre-built partitions.
     */
    @Nullable <T> T get(String key);

    /**
     * Store a type by key.
     */
    void put(String key, Object type);

    // source: the compiler-internal object (e.g. Symbol) the type is being
    // constructed from. Implementations may inspect it via reflection to
    // determine provenance (e.g. which JAR a class was loaded from).

    /**
     * Cache-or-build for {@link JavaType.Class}. Returns the cached instance
     * if one is registered under {@code signature}; otherwise creates a stub,
     * registers it, and runs {@code initializer} to populate it. The
     * initializer typically ends by calling {@link JavaType.Class#unsafeSet}
     * to populate the stub's fields.
     */
    JavaType.Class computeClass(String signature, String fqn, long flags,
                                 JavaType.FullyQualified.Kind kind,
                                 @Nullable Object source,
                                 Consumer<JavaType.Class> initializer);

    /** Cache-or-build for {@link JavaType.Method}. See {@link #computeClass}. */
    JavaType.Method computeMethod(String signature, long flags, String name,
                                   String @Nullable [] paramNames,
                                   @Nullable List<String> defaultValues,
                                   String @Nullable [] formalTypeNames,
                                   @Nullable Object source,
                                   Consumer<JavaType.Method> initializer);

    /** Cache-or-build for {@link JavaType.Variable}. See {@link #computeClass}. */
    JavaType.Variable computeVariable(String signature, long flags, String name,
                                       @Nullable Object source,
                                       Consumer<JavaType.Variable> initializer);

    /** Cache-or-build for {@link JavaType.Intersection}. See {@link #computeClass}. */
    JavaType.Intersection computeIntersection(String signature, @Nullable Object source,
                                               Consumer<JavaType.Intersection> initializer);

    /** Cache-or-build for {@link JavaType.Array}. See {@link #computeClass}. */
    JavaType.Array computeArray(String signature, @Nullable Object source,
                                 Consumer<JavaType.Array> initializer);

    /** Cache-or-build for {@link JavaType.GenericTypeVariable}. See {@link #computeClass}. */
    JavaType.GenericTypeVariable computeGenericTypeVariable(
            String signature, String name,
            JavaType.GenericTypeVariable.Variance variance,
            @Nullable Object source,
            Consumer<JavaType.GenericTypeVariable> initializer);

    /** Cache-or-build for {@link JavaType.Parameterized}. See {@link #computeClass}. */
    JavaType.Parameterized computeParameterized(String signature, @Nullable Object source,
                                                 Consumer<JavaType.Parameterized> initializer);
}
