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

import java.util.List;
import java.util.function.Consumer;

/**
 * Default implementation of {@link JavaTypeFactory}: each {@code computeXxx}
 * looks up by signature, returns the cached instance on hit, or otherwise
 * constructs a fresh stub via the standard {@link JavaType} constructor,
 * registers it in the cache, and runs the initializer to populate it.
 */
public class DefaultJavaTypeFactory implements JavaTypeFactory {

    private final JavaTypeCache cache;

    public DefaultJavaTypeFactory(JavaTypeCache cache) {
        this.cache = cache;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T> T get(String key) {
        return cache.get(key);
    }

    @Override
    public void put(String key, Object type) {
        cache.put(key, type);
    }

    @Override
    public JavaType.Class computeClass(String signature, String fqn, long flags,
                                        JavaType.FullyQualified.Kind kind,
                                        @Nullable Object source,
                                        Consumer<JavaType.Class> initializer) {
        JavaType.Class cached = get(signature);
        if (cached != null) {
            return cached;
        }
        JavaType.Class stub = new JavaType.Class(
                null, flags, fqn, kind,
                null, null, null, null, null, null, null);
        put(signature, stub);
        initializer.accept(stub);
        return stub;
    }

    @Override
    public JavaType.Method computeMethod(String signature, long flags, String name,
                                          String @Nullable [] paramNames,
                                          @Nullable List<String> defaultValues,
                                          String @Nullable [] formalTypeNames,
                                          @Nullable Object source,
                                          Consumer<JavaType.Method> initializer) {
        JavaType.Method cached = get(signature);
        if (cached != null) {
            return cached;
        }
        JavaType.Method stub = new JavaType.Method(
                null, flags, null, name, null,
                paramNames, null, null, null,
                defaultValues, formalTypeNames);
        put(signature, stub);
        initializer.accept(stub);
        return stub;
    }

    @Override
    public JavaType.Variable computeVariable(String signature, long flags, String name,
                                              @Nullable Object source,
                                              Consumer<JavaType.Variable> initializer) {
        JavaType.Variable cached = get(signature);
        if (cached != null) {
            return cached;
        }
        JavaType.Variable stub = new JavaType.Variable(
                null, flags, name, null, null, null);
        put(signature, stub);
        initializer.accept(stub);
        return stub;
    }

    @Override
    public JavaType.Intersection computeIntersection(String signature, @Nullable Object source,
                                                      Consumer<JavaType.Intersection> initializer) {
        JavaType.Intersection cached = get(signature);
        if (cached != null) {
            return cached;
        }
        JavaType.Intersection stub = new JavaType.Intersection(null);
        put(signature, stub);
        initializer.accept(stub);
        return stub;
    }

    @Override
    public JavaType.Array computeArray(String signature, @Nullable Object source,
                                        Consumer<JavaType.Array> initializer) {
        JavaType.Array cached = get(signature);
        if (cached != null) {
            return cached;
        }
        JavaType.Array stub = new JavaType.Array(null, null, null);
        put(signature, stub);
        initializer.accept(stub);
        return stub;
    }

    @Override
    public JavaType.GenericTypeVariable computeGenericTypeVariable(
            String signature, String name,
            JavaType.GenericTypeVariable.Variance variance,
            @Nullable Object source,
            Consumer<JavaType.GenericTypeVariable> initializer) {
        JavaType.GenericTypeVariable cached = get(signature);
        if (cached != null) {
            return cached;
        }
        JavaType.GenericTypeVariable stub = new JavaType.GenericTypeVariable(null, name, variance, null);
        put(signature, stub);
        initializer.accept(stub);
        return stub;
    }

    @Override
    public JavaType.Parameterized computeParameterized(String signature, @Nullable Object source,
                                                        Consumer<JavaType.Parameterized> initializer) {
        JavaType.Parameterized cached = get(signature);
        if (cached != null) {
            return cached;
        }
        JavaType.Parameterized stub = new JavaType.Parameterized(null, null, null);
        put(signature, stub);
        initializer.accept(stub);
        return stub;
    }
}
