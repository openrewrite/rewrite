/*
 * Copyright 2025 the original author or authors.
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
package org.openrewrite.java.lombok;

import org.jspecify.annotations.Nullable;
import org.openrewrite.internal.ReflectionUtils;

import javax.annotation.processing.Processor;
import java.io.File;
import java.lang.reflect.Constructor;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

public class LombokSupport {

    public static @Nullable Processor createLombokProcessor(ClassLoader parserClassLoader) throws ReflectiveOperationException {
        // https://projectlombok.org/contributing/lombok-execution-path
        List<String> overrideClasspath = new ArrayList<>();
        for (Path entry : ReflectionUtils.findClassPathEntriesFor("lombok/Getter.class", parserClassLoader)) {
            if (entry.getFileName().toString().contains("lombok") && !overrideClasspath.contains(entry.toString())) {
                overrideClasspath.add(entry.toString());
            }
        }

        // try to find `rewrite-java-lombok` using class loader
        for (Path entry : ReflectionUtils.findClassPathEntriesFor("org/openrewrite/java/lombok/OpenRewriteConfigurationKeysLoader.class", parserClassLoader)) {
            if (!overrideClasspath.contains(entry.toString())) {
                // make sure the rewrite-java-lombok dependency comes first
                overrideClasspath.add(0, entry.toString());
            }
        }
        // for IDE support, where the `rewrite-java-lombok` classes and resources could be in separate folders
        for (Path entry : ReflectionUtils.findClassPathEntriesFor("META-INF/services/lombok.core.configuration.ConfigurationKeysLoader", parserClassLoader)) {
            if (!overrideClasspath.contains(entry.toString())) {
                // make sure the rewrite-java-lombok dependency comes first
                overrideClasspath.add(0, entry.toString());
            }
        }

        if (overrideClasspath.isEmpty()) {
            return null;
        }

        String oldValue = System.setProperty("shadow.override.lombok", String.join(File.pathSeparator, overrideClasspath));

        try {
            // Attempt to carefully load the `lombok.launch.ShadowClassLoader` present in `rewrite-java-lombok`
            // but as a fallback attempt to load it through the parent class loader
            Class<?> shadowLoaderClass;
            try {
                shadowLoaderClass = Class.forName("lombok.launch.ShadowClassLoader", true, parserClassLoader);
            } catch (Exception e) {
                shadowLoaderClass = Class.forName("lombok.launch.ShadowClassLoader", true, parserClassLoader.getParent());
            }
            Constructor<?> shadowLoaderConstructor = shadowLoaderClass.getDeclaredConstructor(
                    Class.forName("java.lang.ClassLoader"),
                    Class.forName("java.lang.String"),
                    Class.forName("java.lang.String"),
                    Class.forName("java.util.List"),
                    Class.forName("java.util.List"));
            shadowLoaderConstructor.setAccessible(true);

            ClassLoader lombokShadowLoader = (ClassLoader) shadowLoaderConstructor.newInstance(
                    parserClassLoader,
                    "lombok",
                    null,
                    emptyList(),
                    singletonList("lombok.patcher.Symbols")
            );
            return (Processor) lombokShadowLoader.loadClass("lombok.core.AnnotationProcessor").getDeclaredConstructor().newInstance();
        } finally {
            if (oldValue != null) {
                System.setProperty("shadow.override.lombok", oldValue);
            } else {
                System.clearProperty("shadow.override.lombok");
            }
        }
    }
}
