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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Default implementation of {@link JavaTypeFactory}: each method looks up by
 * key in a single {@link JavaTypeCache}. On hit, the cached instance is
 * returned. On miss, the {@code compute*} family constructs a stub, registers
 * it, then runs the initializer; the {@code *For} family invokes the supplier
 * atomically and caches the result.
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class DefaultJavaTypeFactory implements JavaTypeFactory {

    private final JavaTypeCache cache;

    /**
     * FQNs whose {@link #computeClass} initializer is currently running. While
     * this set is non-empty an initialization is on the stack, so the thin-stub
     * upgrade path is suppressed: the parser breaks reference cycles (e.g. the
     * {@code @Retention}/{@code @Target} meta-annotation cycle) by relying on each
     * initializer running exactly once, and re-entering one mid-cycle would
     * recurse infinitely. Upgrades therefore happen only at top level.
     */
    private final Set<String> initializing = new HashSet<>();

    /**
     * Signatures whose {@link #methodFor} builder is currently running, mapped to
     * the placeholder handed out to re-entrant lookups. A method's attribution can
     * recurse back to its own signature — an annotation-element method whose own
     * annotations reference an annotation whose element is that same method (the
     * Spring {@code @AliasFor} shape: {@code value()} is {@code @AliasFor} and
     * {@code @AliasFor}'s element is {@code value()}). Without a pre-registered
     * placeholder the build recurses until the stack overflows.
     */
    private final Map<String, JavaType.Method> methodsInProgress = new HashMap<>();

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
            // Return the cached instance as-is when it is already attributed, or when
            // any initialization is in progress. The latter is essential: the parser's
            // cycle-breaking (e.g. the @Retention/@Target meta-annotation cycle) relies
            // on each initializer running exactly once, so an upgrade must never re-enter
            // an initializer while another is on the stack. We only upgrade at top level.
            if (isFullClass(cached) || !initializing.isEmpty()) {
                return cached;
            }
            // Top level, and the cache holds a thin stub left by an earlier placeholder
            // or partial attribution (e.g. an FQN-only "facade" call won the race). Run
            // this (potentially richer) initializer onto the SAME instance so that every
            // reference already handed out observes the upgrade in place.
            initializing.add(fqn);
            try {
                initializer.accept(cached);
            } finally {
                initializing.remove(fqn);
            }
            if (isFullClass(cached)) {
                // The upgrade produced real attribution, so adopt this call's flags
                // and kind too (a placeholder may have guessed e.g. PUBLIC/Class).
                cached.unsafeSet(flags, kind, fqn,
                        cached.getTypeParameters(), cached.getSupertype(), cached.getOwningClass(),
                        cached.getAnnotations(), cached.getInterfaces(), cached.getMembers(), cached.getMethods());
            }
            return cached;
        }
        JavaType.Class stub = new JavaType.Class(
                null, flags, fqn, kind,
                null, null, null, null, null, null, null);
        cache.put(fqn, stub);
        initializing.add(fqn);
        try {
            initializer.accept(stub);
        } finally {
            initializing.remove(fqn);
        }
        return stub;
    }

    /**
     * A class is a "thin stub" when it carries none of the structural attribution
     * that distinguishes a real type from a freshly-constructed placeholder: no
     * supertype, interfaces, members, methods, or type parameters. A fully
     * attributed type — even an empty {@code class Empty {}} — has at least a
     * supertype ({@code java.lang.Object}), so only markers with no members (e.g.
     * a marker interface) read as thin, in which case re-running the idempotent
     * initializer is harmless.
     */
    private static boolean isFullClass(JavaType.Class c) {
        return c.getSupertype() != null || !c.getMethods().isEmpty();
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
    public JavaType.Method methodFor(String signature, Supplier<JavaType.Method> builder) {
        JavaType.Method cached = cache.get(signature);
        if (cached != null) {
            return cached;
        }
        // Method attribution can recurse back to the same signature (see
        // methodsInProgress). Hand re-entrant lookups a placeholder, build the
        // method, then copy the result into that placeholder so every reference
        // already handed out resolves to one canonical, fully-populated instance.
        JavaType.Method inProgress = methodsInProgress.get(signature);
        if (inProgress != null) {
            return inProgress;
        }
        JavaType.Method stub = new JavaType.Method(
                null, 0, null, "", null, (List<String>) null, null, null, null, null, null);
        methodsInProgress.put(signature, stub);
        try {
            JavaType.Method built = builder.get();
            List<String> parameterNames = built.getParameterNames();
            List<JavaType> parameterTypes = built.getParameterTypes();
            List<JavaType> thrownExceptions = built.getThrownExceptions();
            List<JavaType.FullyQualified> annotations = built.getAnnotations();
            List<String> declaredFormalTypeNames = built.getDeclaredFormalTypeNames();
            stub.unsafeSet(built.getName(), built.getFlagsBitMap(), built.getDeclaringType(),
                    built.getReturnType(),
                    parameterNames.toArray(new String[0]),
                    parameterTypes.toArray(JavaType.EMPTY_JAVA_TYPE_ARRAY),
                    thrownExceptions.toArray(JavaType.EMPTY_JAVA_TYPE_ARRAY),
                    annotations.toArray(new JavaType.FullyQualified[0]),
                    built.getDefaultValue(),
                    declaredFormalTypeNames.toArray(new String[0]));
            cache.put(signature, stub);
            return stub;
        } finally {
            methodsInProgress.remove(signature);
        }
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
