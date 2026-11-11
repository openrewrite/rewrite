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
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A type factory that takes the classes of {@code java.*} packages from a map shared by every parser of the same kind
 * in the JVM, and adds the ones it maps to that map. Test support turns it on.
 * <p>
 * A class is shared with every type keyed while mapping it, such as its methods, so a call of one of them has the
 * method the class declares. They are shared when the factory's outermost call returns, as none of them is a stub then,
 * and not at all if that call throws.
 */
public class JdkClassSharingTypeFactory extends DefaultJavaTypeFactory {
    private static volatile boolean forParsers;
    private static final Map<Object, Map<String, Object>> SHARED_BY_PARSER_KIND = new ConcurrentHashMap<>();

    private final SharingCache cache;
    private int depth;
    private boolean failed;

    private JdkClassSharingTypeFactory(SharingCache cache) {
        super(cache);
        this.cache = cache;
    }

    /**
     * Makes {@link #forParser} return a factory. Test support calls it.
     */
    public static void useForParsers() {
        forParsers = true;
    }

    /**
     * @param parserKind What tells apart parsers that map the same class differently, such as their builder's class.
     * @return A factory that shares JDK classes with the parsers of the same kind, once {@link #useForParsers()} has
     * been called, or else null.
     */
    public static @Nullable JavaTypeFactory forParser(JavaTypeCache cache, Object parserKind) {
        if (!forParsers) {
            return null;
        }
        return new JdkClassSharingTypeFactory(new SharingCache(cache,
                SHARED_BY_PARSER_KIND.computeIfAbsent(parserKind, k -> new ConcurrentHashMap<>())));
    }

    @Override
    public JavaType.Class computeClass(String fqn, long flags, JavaType.FullyQualified.Kind kind,
                                       Consumer<JavaType.Class> initializer) {
        return tracked(() -> {
            JavaType.Class known = cache.get(fqn);
            if (known != null) {
                return known;
            }
            boolean jdk = isJdkClass(fqn);
            if (jdk) {
                cache.jdkClassesBeingMapped++;
            }
            try {
                return super.computeClass(fqn, flags, kind, initializer);
            } finally {
                if (jdk) {
                    cache.jdkClassesBeingMapped--;
                }
            }
        });
    }

    /**
     * A source can declare a class in a new {@code java.*} package, so the class must also be one the JDK has.
     */
    private static boolean isJdkClass(String fqn) {
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
                cache.share(!failed);
                failed = false;
            }
        }
    }

    /**
     * A parser's cache that reads the types it doesn't hold from the shared map, and keeps what the parser keys while
     * mapping a JDK class for the factory to share.
     */
    private static class SharingCache extends JavaTypeCache {
        private final JavaTypeCache cache;
        private final Map<String, Object> shared;
        private final Map<String, Object> mapped = new HashMap<>();
        int jdkClassesBeingMapped;

        SharingCache(JavaTypeCache cache, Map<String, Object> shared) {
            this.cache = cache;
            this.shared = shared;
        }

        @Override
        public <T> @Nullable T get(String signature) {
            T type = cache.get(signature);
            if (type == null) {
                //noinspection unchecked
                type = (T) shared.get(signature);
                if (type != null) {
                    cache.put(signature, type);
                }
            }
            return type;
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

        void share(boolean complete) {
            if (complete) {
                for (Map.Entry<String, Object> type : mapped.entrySet()) {
                    shared.putIfAbsent(type.getKey(), type.getValue());
                }
            }
            mapped.clear();
        }
    }
}
