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

import org.openrewrite.internal.ToBeRemoved;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.java.tree.JavaType;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * Compatibility shim for {@link JavaSourceSet#findClasspathType} and
 * {@link JavaSourceSet#classpathTypesInPackage}, which were added in
 * rewrite-java 8.82.0.
 * <p>
 * Recipe code that ships with rewrite-java &ge; 8.82 may call those methods
 * directly only if it can require an equally-new host. Recipes that may run
 * on older hosts should route through this shim, which probes for the
 * methods at class-init time and falls back to a streaming scan on the
 * existing {@link JavaSourceSet#getClasspath()} when they are absent.
 * <p>
 * Once 8.82.0 is the established CLI/SaaS floor for all consumers, the
 * call sites can be inlined back to direct method invocations and this
 * class deleted.
 */
@ToBeRemoved(after = "2026-06-30", reason = "Inline call sites and delete once rewrite-java 8.82.0 is the established CLI/SaaS floor.")
public final class JavaSourceSetCompat {

    private static final boolean HAS_FAST_PATH;

    static {
        boolean ok;
        try {
            JavaSourceSet.class.getMethod("findClasspathType", String.class);
            JavaSourceSet.class.getMethod("classpathTypesInPackage", String.class);
            ok = true;
        } catch (NoSuchMethodException e) {
            ok = false;
        }
        HAS_FAST_PATH = ok;
    }

    private JavaSourceSetCompat() {
    }

    public static Optional<JavaType.FullyQualified> findClasspathType(JavaSourceSet jss, String fqn) {
        if (HAS_FAST_PATH) {
            return jss.findClasspathType(fqn);
        }
        return jss.getClasspath().stream()
                .filter(t -> fqn.equals(t.getFullyQualifiedName()))
                .findFirst();
    }

    public static Stream<JavaType.FullyQualified> classpathTypesInPackage(JavaSourceSet jss, String packageName) {
        if (HAS_FAST_PATH) {
            return jss.classpathTypesInPackage(packageName);
        }
        return jss.getClasspath().stream()
                .filter(t -> packageName.equals(t.getPackageName()));
    }
}
