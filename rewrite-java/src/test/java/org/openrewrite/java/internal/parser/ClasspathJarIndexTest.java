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
package org.openrewrite.java.internal.parser;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class ClasspathJarIndexTest {

    @TempDir
    Path dir;

    @Test
    void indexesOnlyTheJarsAfterTheLastDirectoryAndPathingJar() throws IOException {
        Path first = jar("first.jar", null, "a/A.class");
        Path classes = Files.createDirectories(dir.resolve("classes"));
        Path pathing = jar("pathing.jar", "first.jar", "b/B.class");
        Path last = jar("last.jar", null, "c/C.class");

        assertThat(jars(asList(first, classes, last))).containsExactly(last);

        assertThat(jars(asList(first, pathing, last))).containsExactly(last);
    }

    @Test
    void listsAPackageOrItsSubpackagesButNotMetaInf() throws IOException {
        ClasspathJarIndex index = index(jar("lib.jar", null, "a/A.class", "a/b/C.class", "a/D.java",
                "META-INF/versions/11/a/A.class"));

        assertThat(binaryNames(index, "a", false)).containsExactly("a.A", "a.D");

        assertThat(binaryNames(index, "", true)).containsExactlyInAnyOrder("a.A", "a.b.C", "a.D");
    }

    @Test
    void indexesAJarAgainOnceItIsReplaced() throws IOException {
        Path lib = jar("lib.jar", null, "a/A.class");
        assertThat(binaryNames(index(lib), "a", false)).containsExactly("a.A");

        jar("lib.jar", null, "a/A.class", "a/B.class");
        assertThat(binaryNames(index(lib), "a", false)).containsExactly("a.A", "a.B");
    }

    private List<Path> jars(List<Path> classpath) {
        List<Path> jars = new ArrayList<>();
        for (ClasspathJarIndex index : ClasspathJarIndex.indexTrailingJars(classpath)) {
            jars.add(index.getJar());
        }
        return jars;
    }

    private static ClasspathJarIndex index(Path jar) {
        return ClasspathJarIndex.indexTrailingJars(singletonList(jar)).get(0);
    }

    private static List<String> binaryNames(ClasspathJarIndex index, String packageName, boolean recurse) {
        List<JavaFileObject> files = new ArrayList<>();
        index.list(packageName, EnumSet.of(JavaFileObject.Kind.CLASS, JavaFileObject.Kind.SOURCE), recurse,
                StandardCharsets.UTF_8, files);
        List<String> names = new ArrayList<>();
        for (JavaFileObject file : files) {
            names.add(ClasspathJarIndex.binaryName(file));
        }
        return names;
    }

    private Path jar(String name, @Nullable String manifestClassPath, String... entries) throws IOException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        if (manifestClassPath != null) {
            manifest.getMainAttributes().put(Attributes.Name.CLASS_PATH, manifestClassPath);
        }
        Path jar = dir.resolve(name);
        try (OutputStream out = Files.newOutputStream(jar);
             JarOutputStream jarOut = new JarOutputStream(out, manifest)) {
            for (String entry : entries) {
                jarOut.putNextEntry(new JarEntry(entry));
                jarOut.write(new byte[]{1});
                jarOut.closeEntry();
            }
        }
        return jar;
    }
}
