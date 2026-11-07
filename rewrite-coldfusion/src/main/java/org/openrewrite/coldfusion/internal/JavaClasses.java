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
package org.openrewrite.coldfusion.internal;

import org.jspecify.annotations.Nullable;
import org.openrewrite.java.internal.JavaReflectionTypeMapping;
import org.openrewrite.java.internal.JavaTypeCache;
import org.openrewrite.java.tree.JavaType;
import org.openrewrite.java.tree.TypeUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The JDK's classes, which CFML reaches with {@code createObject("java", "java.io.File")}, typed as Java types them.
 * Only {@code java.*} and {@code javax.*} classes count, from the platform's own class loader, so that what is typed
 * does not depend on the classpath of the process parsing.
 */
final class JavaClasses {
    private static final JavaReflectionTypeMapping MAPPING = new JavaReflectionTypeMapping(new JavaTypeCache());
    private static final Map<String, Optional<JavaType.FullyQualified>> TYPES = new ConcurrentHashMap<>();

    private JavaClasses() {
    }

    static JavaType.@Nullable FullyQualified type(String className) {
        String name = className.trim();
        if (!name.startsWith("java.") && !name.startsWith("javax.")) {
            return null;
        }
        return TYPES.computeIfAbsent(name, n -> {
            try {
                Class<?> loaded = Class.forName(n, false, ClassLoader.getSystemClassLoader().getParent());
                synchronized (MAPPING) {
                    return Optional.ofNullable(TypeUtils.asFullyQualified(MAPPING.type(loaded)));
                }
            } catch (ClassNotFoundException | LinkageError e) {
                return Optional.empty();
            }
        }).orElse(null);
    }

    static boolean isJava(@Nullable JavaType type) {
        JavaType.FullyQualified fullyQualified = TypeUtils.asFullyQualified(type);
        return fullyQualified != null && !(type instanceof JavaType.Unknown) && type(fullyQualified.getFullyQualifiedName()) != null;
    }

    /**
     * The method a Java type or one of its supertypes declares by a name, in any case as CFML calls it, that takes as
     * many arguments as a call passes. CFML's {@code init} on a class is its constructor.
     */
    static JavaType.@Nullable Method method(JavaType.FullyQualified type, String name, int arguments) {
        String wanted = "init".equalsIgnoreCase(name) ? "<constructor>" : name;
        Set<String> seen = new HashSet<>();
        Deque<JavaType.FullyQualified> types = new ArrayDeque<>();
        types.add(type);
        while (!types.isEmpty()) {
            JavaType.FullyQualified t = types.removeFirst();
            if (!seen.add(t.getFullyQualifiedName())) {
                continue;
            }
            for (JavaType.Method method : t.getMethods()) {
                if (method.getName().equalsIgnoreCase(wanted) && method.getParameterTypes().size() == arguments) {
                    return method;
                }
            }
            if ("<constructor>".equals(wanted)) {
                // a constructor is the class's own
                break;
            }
            if (t.getSupertype() != null) {
                types.add(t.getSupertype());
            }
            types.addAll(t.getInterfaces());
        }
        return null;
    }
}
