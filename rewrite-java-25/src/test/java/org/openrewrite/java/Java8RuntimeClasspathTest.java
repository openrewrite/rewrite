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
package org.openrewrite.java;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.openrewrite.java.Assertions.java;

class Java8RuntimeClasspathTest implements RewriteTest {
    @TempDir
    static Path temp;
    static Path rtJar;

    @BeforeAll
    static void java8Platform() throws Exception {
        rtJar = temp.resolve("rt.jar");
        // Repackage Java 8 platform signatures so the tests need no separate JDK 8 installation.
        try (ZipFile symbols = new ZipFile(Path.of(System.getProperty("java.home"), "lib", "ct.sym").toFile());
             ZipOutputStream jar = new ZipOutputStream(Files.newOutputStream(rtJar))) {
            var entries = symbols.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String name = entry.getName();
                int firstSlash = name.indexOf('/');
                if (!name.endsWith(".sig") || !name.substring(0, firstSlash).contains("8")) {
                    continue;
                }
                String className = name.substring(name.indexOf('/', firstSlash + 1) + 1)
                  .replace(".sig", ".class");
                jar.putNextEntry(new ZipEntry(className));
                try (var in = symbols.getInputStream(entry)) {
                    in.transferTo(jar);
                }
                jar.closeEntry();
            }
        }
    }

    @Test
    void usesTheJava8PlatformConstructorSignature() {
        rewriteRun(
          spec -> spec.parser(Java25Parser.builder().classpath(List.of(rtJar))),
          java(
            """
              class LegacyTable extends javax.swing.JTable {
                  LegacyTable(java.util.Vector<?> rows, java.util.Vector<?> columns) {
                      super(rows, columns);
                  }
              }
              """
          )
        );
    }

    @Test
    void underscoreIdentifiersAreAttributed() {
        rewriteRun(
          spec -> spec.parser(Java25Parser.builder().classpath(List.of(rtJar))),
          java(
            """
              class LegacyNames {
                  String _(String value) {
                      return value;
                  }
                  String value = _("text");
              }
              """
          )
        );
    }
}
