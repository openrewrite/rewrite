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

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * The classes of {@code java.lang} in an earlier Java release, read from the {@code lib/ct.sym} of the running JDK,
 * the same data that {@code javac --release} compiles against.
 * <p>
 * Every class of {@code java.lang} is imported on demand into every compilation unit. When the JDK running the parser
 * is newer than the release the sources are written for, a class that {@code java.lang} only gained later, such as
 * {@code java.lang.Record} in Java 16, makes a type of the same simple name that the sources import on demand from
 * another package ambiguous, so it is left without a type. Hiding those classes resolves the name as the compiler of
 * that release does.
 */
public final class JavaLangReleaseClasses {
    private static final Map<Integer, Optional<Set<String>>> CLASSES_BY_RELEASE = new ConcurrentHashMap<>();

    private JavaLangReleaseClasses() {
    }

    /**
     * @param release The Java release the sources are written for, such as {@code 8}.
     * @return The binary names of the classes of {@code java.lang} in that release, such as
     * {@code java.lang.Character$UnicodeBlock}, or {@code null} when nothing needs to be hidden: the release is not
     * older than the running JDK, or the running JDK has no {@code ct.sym} that describes it.
     */
    public static @Nullable Set<String> forRelease(int release) {
        if (release >= runtimeRelease()) {
            return null;
        }
        return CLASSES_BY_RELEASE.computeIfAbsent(release, r -> Optional.ofNullable(read(r))).orElse(null);
    }

    private static @Nullable Set<String> read(int release) {
        Path ctSym = Paths.get(System.getProperty("java.home"), "lib", "ct.sym");
        if (!Files.isRegularFile(ctSym)) {
            return null;
        }
        // Each top-level directory of ct.sym holds the classes that are identical in a set of releases, and is named
        // for them, one character per release in base 36: "8" is Java 8, "A" Java 10, "P" Java 25.
        String releaseCode = Integer.toString(release, Character.MAX_RADIX).toUpperCase(Locale.ROOT);
        try (FileSystem fs = FileSystems.newFileSystem(ctSym, (ClassLoader) null);
             DirectoryStream<Path> sections = Files.newDirectoryStream(fs.getRootDirectories().iterator().next())) {
            Set<String> classes = new HashSet<>();
            for (Path section : sections) {
                String sectionName = section.getFileName().toString().replace("/", "");
                if (!sectionName.contains(releaseCode) || sectionName.contains("-")) {
                    continue;
                }
                // The ct.sym of JDK 17 groups the classes of a section by module, that of JDK 11 doesn't
                Path javaLang = section.resolve("java.base/java/lang");
                if (!Files.isDirectory(javaLang)) {
                    javaLang = section.resolve("java/lang");
                    if (!Files.isDirectory(javaLang)) {
                        continue;
                    }
                }
                try (Stream<Path> files = Files.list(javaLang)) {
                    files.map(file -> file.getFileName().toString())
                            .filter(name -> name.endsWith(".sig"))
                            .forEach(name -> classes.add("java.lang." + name.substring(0, name.length() - ".sig".length())));
                }
            }
            return classes.isEmpty() ? null : Collections.unmodifiableSet(classes);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    private static int runtimeRelease() {
        String version = System.getProperty("java.specification.version");
        try {
            return Integer.parseInt(version.startsWith("1.") ? version.substring(2) : version);
        } catch (NumberFormatException e) {
            return Integer.MAX_VALUE;
        }
    }
}
