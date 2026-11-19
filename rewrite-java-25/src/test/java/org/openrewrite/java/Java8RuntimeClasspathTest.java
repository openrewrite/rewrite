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
import org.openrewrite.java.internal.JdkClassSharingTypeFactory;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.openrewrite.java.Assertions.java;
import static org.assertj.core.api.Assertions.assertThat;

class Java8RuntimeClasspathTest implements RewriteTest {
    @TempDir
    static Path temp;
    static Path rtJar;

    @BeforeAll
    static void java8Platform() throws Exception {
        rtJar = temp.resolve("rt.jar");
        // Repackage Java 8 platform signatures so the tests need no separate JDK 8 installation.
        try (ZipFile symbols = new ZipFile(Path.of(System.getProperty("java.home"), "lib", "ct.sym").toFile());
             ZipOutputStream jar = new ZipOutputStream(Files.newOutputStream(rtJar));
             ZipOutputStream jce = new ZipOutputStream(Files.newOutputStream(temp.resolve("jce.jar")));
             ZipOutputStream jsse = new ZipOutputStream(Files.newOutputStream(temp.resolve("jsse.jar")))) {
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
                ZipOutputStream target = className.startsWith("javax/crypto/") ? jce :
                  className.startsWith("javax/net/ssl/") ? jsse : jar;
                target.putNextEntry(new ZipEntry(className));
                try (var in = symbols.getInputStream(entry)) {
                    in.transferTo(target);
                }
                target.closeEntry();
            }
        }
    }

    @Test
    void hostAndJava8PlatformsDoNotShareJdkClasses() {
        JdkClassSharingTypeFactory.useForParsers();
        Java25Parser.Builder builder = Java25Parser.builder();
        assertThat(stringType(builder.build()).getMethods()).extracting(JavaType.Method::getName).contains("strip");
        assertThat(stringType(builder.java8Runtime(rtJar).build()).getMethods())
          .extracting(JavaType.Method::getName).doesNotContain("strip");
        assertThat(stringType(builder.java8Runtime(null).build()).getMethods())
          .extracting(JavaType.Method::getName).contains("strip");
    }

    private static JavaType.FullyQualified stringType(JavaParser parser) {
        J.CompilationUnit cu = (J.CompilationUnit) parser.parse("class PlatformTypes { String text; }").findFirst().orElseThrow();
        J.VariableDeclarations field = (J.VariableDeclarations) cu.getClasses().getFirst().getBody().getStatements().getFirst();
        return (JavaType.FullyQualified) field.getType();
    }

    @Test
    void rtJarOnTheDependencyClasspathDoesNotSelectThePlatform() {
        rewriteRun(
          spec -> spec.parser(Java25Parser.builder().classpath(List.of(rtJar))),
          java(
            """
              record CurrentPlatform(String text) {
                  String cleaned() {
                      return text.strip();
                  }
              }
              """
          )
        );
    }

    @Test
    void anExplicitPlatformDoesNotDependOnItsFilename() throws Exception {
        Path renamed = Files.copy(rtJar, temp.resolve("legacy-platform.jar"));
        rewriteRun(
          spec -> spec.parser(Java25Parser.builder().java8Runtime(renamed)),
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
    void includesTheSeparateJava8CryptoAndTlsLibraries() {
        rewriteRun(
          spec -> spec.parser(Java25Parser.builder().java8Runtime(rtJar)),
          java(
            """
              class LegacySecurity {
                  javax.crypto.spec.PBEKeySpec spec = new javax.crypto.spec.PBEKeySpec("password".toCharArray());
                  javax.crypto.SecretKeyFactory factory() throws Exception {
                      return javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                  }
                  javax.net.ssl.SSLContext context() throws Exception {
                      return javax.net.ssl.SSLContext.getDefault();
                  }
              }
              """
          )
        );
    }

    @Test
    void usesTheJava8PlatformConstructorSignature() {
        rewriteRun(
          spec -> spec.parser(Java25Parser.builder().java8Runtime(rtJar)),
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
          spec -> spec.parser(Java25Parser.builder().java8Runtime(rtJar)),
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
