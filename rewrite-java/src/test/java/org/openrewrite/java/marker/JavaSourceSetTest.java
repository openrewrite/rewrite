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
package org.openrewrite.java.marker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.java.tree.JavaType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class JavaSourceSetTest {

    @TempDir
    Path tempDir;

    @Test
    void skipsMetaInfEntriesInJar() throws Exception {
        Path jarFile = tempDir.resolve("multi-release.jar");
        byte[] dummyClassBytes = new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE};
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarFile))) {
            writeEntry(jos, "net/bytebuddy/asm/Advice.class", dummyClassBytes);
            writeEntry(jos, "META-INF/versions/9/net/bytebuddy/asm/Advice.class", dummyClassBytes);
            writeEntry(jos, "META-INF/versions/11/net/bytebuddy/asm/Advice.class", dummyClassBytes);
            writeEntry(jos, "META-INF/some/Stray.class", dummyClassBytes);
        }

        List<JavaType.FullyQualified> types = JavaSourceSet.typesFromPath(jarFile, null);

        assertThat(types)
          .extracting(JavaType.FullyQualified::getFullyQualifiedName)
          .containsExactly("net.bytebuddy.asm.Advice");
    }

    @Test
    void skipsMetaInfEntriesUnderDirectoryClasspath() throws Exception {
        byte[] dummyClassBytes = new byte[]{(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE};
        Files.createDirectories(tempDir.resolve("net/bytebuddy/asm"));
        Files.write(tempDir.resolve("net/bytebuddy/asm/Advice.class"), dummyClassBytes);
        Files.createDirectories(tempDir.resolve("META-INF/versions/9/net/bytebuddy/asm"));
        Files.write(tempDir.resolve("META-INF/versions/9/net/bytebuddy/asm/Advice.class"), dummyClassBytes);

        List<JavaType.FullyQualified> types = JavaSourceSet.typesFromPath(tempDir, null);

        assertThat(types)
          .extracting(JavaType.FullyQualified::getFullyQualifiedName)
          .containsExactly("net.bytebuddy.asm.Advice");
    }

    private static void writeEntry(JarOutputStream jos, String name, byte[] content) throws java.io.IOException {
        JarEntry entry = new JarEntry(name);
        jos.putNextEntry(entry);
        jos.write(content);
        jos.closeEntry();
    }
}
