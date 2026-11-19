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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.InMemoryExecutionContext;

import javax.tools.ToolProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.java;

class JavaClasspathSourcesTest implements RewriteTest {
    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void compiledDependencyWinsOverEmbeddedUnrelocatedSource(boolean sharedIndex, @TempDir Path temp) throws Exception {
        Path source = temp.resolve("Dependency.java");
        Files.writeString(source, "package fixture.shaded; public class Dependency { public static int answer() { return 42; } }");
        assertThat(ToolProvider.getSystemJavaCompiler().run(null, null, null,
          "-d", temp.toString(), source.toString())).isZero();
        Path jarPath = temp.resolve("shaded.jar");
        try (ZipOutputStream jar = new ZipOutputStream(Files.newOutputStream(jarPath))) {
            ZipEntry binary = new ZipEntry("fixture/shaded/Dependency.class");
            binary.setTime(1_500_000_000_000L);
            jar.putNextEntry(binary);
            Files.copy(temp.resolve("fixture/shaded/Dependency.class"), jar);
            jar.closeEntry();
            ZipEntry embeddedSource = new ZipEntry("fixture/shaded/Dependency.java");
            embeddedSource.setTime(1_600_000_000_000L);
            jar.putNextEntry(embeddedSource);
            jar.write("package fixture; public class Dependency {}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
            jar.closeEntry();
        }
        var ctx = new InMemoryExecutionContext();
        ctx.putMessage(JavaParser.SHARE_CLASSPATH_JAR_INDEX, sharedIndex);
        rewriteRun(
          spec -> spec.executionContext(ctx).parser(Java25Parser.builder().classpath(List.of(jarPath))),
          java(
            """
              class UseDependency {
                  int value = fixture.shaded.Dependency.answer();
              }
              """
          )
        );
    }
}
