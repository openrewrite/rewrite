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
import org.openrewrite.java.tree.JavaType;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Default implementation of {@link JavaTypeFactory}: each method looks up by
 * key in a single {@link JavaTypeCache}. On hit, the cached instance is
 * returned. On miss, the {@code compute*} family constructs a stub, registers
 * it, then runs the initializer; the {@code *For} family invokes the supplier
 * atomically and caches the result.
 */
public class DefaultJavaTypeFactory implements JavaTypeFactory {

    private final JavaTypeCache cache;

    public DefaultJavaTypeFactory(JavaTypeCache cache) {
        this.cache = cache;
    }

    /**
     * Protected lookup hook for subclasses that need to read the cache before
     * routing to the chain (e.g., the partition-backed factory's chain hit
     * pre-registration).
     */
    protected @Nullable <T> T lookup(String key) {
        return cache.get(key);
    }

    /**
     * Protected write hook for subclasses that pre-register chain instances
     * into the signature-keyed cache so subsequent {@code *For} calls return
     * canonical instances.
     */
    protected void register(String key, Object type) {
        cache.put(key, type);
    }

    // ---------------------------------------------------------------------
    // Stub-then-initialize ({@code compute*})
    // ---------------------------------------------------------------------

    @Override
    public JavaType.Class computeClass(String fqn, long flags,
                                       JavaType.FullyQualified.Kind kind,
                                       Consumer<JavaType.Class> initializer) {
        JavaType.Class cached = cache.get(fqn);
        if (cached != null) {
            return cached;
        }
        JavaType.Class stub = new JavaType.Class(
                null, flags, fqn, kind,
                null, null, null, null, null, null, null);
        cache.put(fqn, stub);
        initializer.accept(stub);
        return stub;
    }

    @Override
    public JavaType.Parameterized computeParameterized(String signature,
                                                       Consumer<JavaType.Parameterized> initializer) {
        JavaType.Parameterized cached = cache.get(signature);
        if (cached != null) {
            return cached;
        }
        JavaType.Parameterized stub = new JavaType.Parameterized(null, null, null);
        cache.put(signature, stub);
        initializer.accept(stub);
        return stub;
    }

    @Override
    public JavaType.GenericTypeVariable computeGenericTypeVariable(
            String signature, String name,
            JavaType.GenericTypeVariable.Variance variance,
            Consumer<JavaType.GenericTypeVariable> initializer) {
        JavaType.GenericTypeVariable cached = cache.get(signature);
        if (cached != null) {
            return cached;
        }
        JavaType.GenericTypeVariable stub = new JavaType.GenericTypeVariable(null, name, variance, null);
        cache.put(signature, stub);
        initializer.accept(stub);
        return stub;
    }

    // ---------------------------------------------------------------------
    // Atomic build ({@code *For})
    // ---------------------------------------------------------------------

    @Override
    public JavaType.Method methodFor(String signature, Supplier<JavaType.Method> stub,
                                     Consumer<JavaType.Method> initializer) {
        JavaType.Method cached = cache.get(signature);
        if (cached != null) {
            return cached;
        }
        // Cache the bare stub before running the initializer so a re-entrant
        // lookup on the same signature (e.g. the @AliasFor annotation-element
        // cycle) resolves to this in-progress instance instead of recursing.
        // Same cycle-breaking contract as computeClass.
        JavaType.Method method = stub.get();
        cache.put(signature, method);
        initializer.accept(method);
        return method;
    }

    @Override
    public JavaType.Variable variableFor(String signature, Supplier<JavaType.Variable> builder) {
        JavaType.Variable cached = cache.get(signature);
        if (cached != null) {
            return cached;
        }
        JavaType.Variable built = builder.get();
        cache.put(signature, built);
        return built;
    }

    @Override
    public JavaType.Array arrayFor(String signature, Supplier<JavaType.Array> builder) {
        JavaType.Array cached = cache.get(signature);
        if (cached != null) {
            return cached;
        }
        JavaType.Array built = builder.get();
        cache.put(signature, built);
        return built;
    }

    @Override
    public JavaType.Intersection intersectionFor(String signature,
                                                 Supplier<JavaType.Intersection> builder) {
        JavaType.Intersection cached = cache.get(signature);
        if (cached != null) {
            return cached;
        }
        JavaType.Intersection built = builder.get();
        cache.put(signature, built);
        return built;
    }
}
