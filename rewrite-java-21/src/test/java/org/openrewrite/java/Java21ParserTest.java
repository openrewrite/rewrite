/*
 * Copyright 2024 the original author or authors.
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

import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.openrewrite.java.Assertions.java;

class Java21ParserTest implements RewriteTest {

    @Override
    @SneakyThrows
    public void defaults(RecipeSpec spec) {
        String userHome = System.getProperty("user.home");
        Path filePath = Paths.get(userHome, ".rewrite", "classpath", "jackson-annotations-2.17.1.jar");
        Files.delete(filePath);

        spec.parser(JavaParser.fromJavaVersion().classpathFromResources(new InMemoryExecutionContext(), "jackson-annotations"));
    }

    @Test
    void shouldCompile() {
        rewriteRun(
          //language=java
          java(
            """
              import com.fasterxml.jackson.annotation.JsonInclude;
              
              class Test {
                  @JsonInclude(value = JsonInclude.Include.NON_NULL)
                  String text;
              }
              """
          )
        );
    }
}
