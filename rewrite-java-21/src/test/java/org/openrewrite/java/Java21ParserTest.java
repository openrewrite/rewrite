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

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.openrewrite.java.Assertions.java;

class Java21ParserTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        String filename = File.separator + ".rewrite" + File.separator + "classpath" + File.separator + "jackson-annotations-2.17.1.jar";
        String userHome = System.getProperty("user.home");
        Path filePath = Paths.get(userHome, filename);

        try {
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                System.out.println("File deleted successfully.");
            } else {
                System.out.println("File does not exist.");
            }
        } catch (IOException e) {
            System.err.println("Error deleting file: " + e.getMessage());
        }

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
