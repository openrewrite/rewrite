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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A type factory that shares the classes of {@code java.*} packages it maps with every parser of the same kind in the
 * JVM, which read them through the cache {@link #cacheForParser} gives. Test support turns it on.
 * <p>
 * A class is shared with every type keyed while mapping it, such as its methods, so a call of one of them has the
 * method the class declares. They are shared when the factory's outermost call returns, as none of them is a stub then,
 * and not at all if that call throws. One parser at a time maps classes that aren't shared yet, so each is shared once.
 */
public class JdkClassSharingTypeFactory extends DefaultJavaTypeFactory {
    private static volatile boolean forParsers;
    private static final Map<Object, Shared> SHARED_BY_PARSER_KIND = new ConcurrentHashMap<>();

    private final Shared shared;
    private final RecordingCache cache;
    private int depth;
    private boolean failed;
    private boolean locked;

    private JdkClassSharingTypeFactory(Shared shared, RecordingCache cache) {
        super(cache);
        this.shared = shared;
        this.cache = cache;
    }

    /**
     * Makes {@link #cacheForParser} and {@link #forParser} share JDK classes. Test support calls it.
     */
    public static void useForParsers() {
        forParsers = true;
    }

    /**
     * @param parserKind What tells apart parsers that map the same class differently, such as their builder's class.
     * @param configured Whether the parser builder's caller gave it a type cache or a type factory.
     * @return The cache a parser builder builds its parsers with: once {@link #useForParsers()} has been called, one
     * that also reads the JDK classes parsers of the same kind have shared, unless the caller configured the builder's
     * types.
     */
    public static JavaTypeCache cacheForParser(JavaTypeCache cache, Object parserKind, boolean configured) {
        return forParsers && !configured && !(cache instanceof ReadingCache) ?
                new ReadingCache(cache, SHARED_BY_PARSER_KIND.computeIfAbsent(parserKind, k -> new Shared())) :
                cache;
    }

    /**
     * @return A factory that shares the JDK classes it maps, when the cache is one {@link #cacheForParser} gave, or
     * else null.
     */
    public static @Nullable JavaTypeFactory forParser(JavaTypeCache cache) {
        if (!(cache instanceof ReadingCache)) {
            return null;
        }
        ReadingCache reading = (ReadingCache) cache;
        return new JdkClassSharingTypeFactory(reading.shared, new RecordingCache(reading));
    }

    @Override
    public JavaType.Class computeClass(String fqn, long flags, JavaType.FullyQualified.Kind kind,
                                       Consumer<JavaType.Class> initializer) {
        return tracked(() -> {
            JavaType.Class known = cache.get(fqn);
            if (known != null) {
                return known;
            }
            if (!isJdkClass(fqn)) {
                return super.computeClass(fqn, flags, kind, initializer);
            }
            if (!locked) {
                shared.lock.lock();
                locked = true;
                // another parser may have shared the class while this one waited
                known = cache.get(fqn);
                if (known != null) {
                    return known;
                }
            }
            cache.jdkClassesBeingMapped++;
            try {
                return super.computeClass(fqn, flags, kind, initializer);
            } finally {
                cache.jdkClassesBeingMapped--;
            }
        });
    }

    /**
     * A source can declare a class in a new {@code java.*} package, so the class must also be one the JDK has.
     */
    public static boolean isJdkClass(String fqn) {
        return fqn.startsWith("java.") && ClassLoader.getSystemResource(fqn.replace('.', '/') + ".class") != null;
    }

    @Override
    public JavaType.Parameterized computeParameterized(String signature, Consumer<JavaType.Parameterized> initializer) {
        return tracked(() -> super.computeParameterized(signature, initializer));
    }

    @Override
    public JavaType.GenericTypeVariable computeGenericTypeVariable(String signature, String name,
                                                                   JavaType.GenericTypeVariable.Variance variance,
                                                                   Consumer<JavaType.GenericTypeVariable> initializer) {
        return tracked(() -> super.computeGenericTypeVariable(signature, name, variance, initializer));
    }

    @Override
    public JavaType.MultiCatch computeMultiCatch(String signature, Consumer<JavaType.MultiCatch> initializer) {
        return tracked(() -> super.computeMultiCatch(signature, initializer));
    }

    @Override
    public JavaType.Method methodFor(String signature, Supplier<JavaType.Method> stub,
                                     Consumer<JavaType.Method> initializer) {
        return tracked(() -> super.methodFor(signature, stub, initializer));
    }

    @Override
    public JavaType.Variable variableFor(String signature, Supplier<JavaType.Variable> builder) {
        return tracked(() -> super.variableFor(signature, builder));
    }

    @Override
    public JavaType.Array arrayFor(String signature, Supplier<JavaType.Array> builder) {
        return tracked(() -> super.arrayFor(signature, builder));
    }

    @Override
    public JavaType.Intersection intersectionFor(String signature, Supplier<JavaType.Intersection> builder) {
        return tracked(() -> super.intersectionFor(signature, builder));
    }

    private <T> T tracked(Supplier<T> call) {
        depth++;
        try {
            return call.get();
        } catch (RuntimeException | Error e) {
            failed = true;
            throw e;
        } finally {
            if (--depth == 0) {
                try {
                    cache.share(shared.types, !failed);
                } finally {
                    failed = false;
                    if (locked) {
                        locked = false;
                        shared.lock.unlock();
                    }
                }
            }
        }
    }

    /**
     * The JDK classes parsers of one kind have shared, which one parser at a time adds to.
     */
    private static class Shared {
        final Map<String, Object> types = new ConcurrentHashMap<>();
        final ReentrantLock lock = new ReentrantLock();
    }

    /**
     * A parser builder's cache that also reads the JDK classes parsers of its kind have shared.
     */
    private static class ReadingCache extends HashMapJavaTypeCache {
        private final Shared shared;

        ReadingCache(JavaTypeCache base, Shared shared) {
            super(base);
            this.shared = shared;
        }

        @Override
        public <T> @Nullable T get(String signature) {
            T type = super.get(signature);
            //noinspection unchecked
            return type != null ? type : (T) shared.types.get(signature);
        }
    }

    /**
     * A parser's cache that keeps what the parser puts while it maps a JDK class, for the factory to share.
     */
    private static class RecordingCache extends JavaTypeCache {
        private final JavaTypeCache cache;
        private final Map<String, Object> mapped = new HashMap<>();
        int jdkClassesBeingMapped;

        RecordingCache(JavaTypeCache cache) {
            this.cache = cache;
        }

        @Override
        public <T> @Nullable T get(String signature) {
            return cache.get(signature);
        }

        @Override
        public void put(String signature, Object o) {
            cache.put(signature, o);
            if (jdkClassesBeingMapped > 0) {
                mapped.put(signature, o);
            }
        }

        @Override
        public void clear() {
            cache.clear();
            mapped.clear();
        }

        /**
         * Shares the classes after the other types, so that a parser that finds a class there also finds its methods.
         */
        void share(Map<String, Object> shared, boolean complete) {
            if (complete) {
                mapped.forEach((signature, type) -> {
                    if (!(type instanceof JavaType.Class)) {
                        shared.put(signature, type);
                    }
                });
                mapped.forEach((signature, type) -> {
                    if (type instanceof JavaType.Class) {
                        shared.put(signature, type);
                    }
                });
            }
            mapped.clear();
        }
    }
}
