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
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.java.marker.JavaSourceSet;
import org.openrewrite.test.RewriteTest;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class DirtyJavaSourceSetsProducerTest implements RewriteTest {

    @Test
    void removeDependencyMarksProjectDirty() {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("junit", "junit", null))
            .executionContext(ctx),
          mavenProject("my-app-project",
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                    </dependency>
                    <dependency>
                      <groupId>junit</groupId>
                      <artifactId>junit</artifactId>
                      <version>4.13.1</version>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </project>
                """,
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                    </dependency>
                  </dependencies>
                </project>
                """
            )
          )
        );

        Set<String> dirty = JavaSourceSet.dirtyProjects(ctx);
        assertThat(dirty)
            .as("UpdateMavenModel should mark the project dirty after a real dep removal")
            .isNotNull()
            .contains("my-app-project");
    }

    @Test
    void noopRemoveDependencyDoesNotMark() {
        InMemoryExecutionContext ctx = new InMemoryExecutionContext();
        rewriteRun(
          spec -> spec.recipe(new RemoveDependency("does.not", "exist", null))
            .executionContext(ctx),
          mavenProject("my-app-project",
            pomXml(
              """
                <project>
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>com.example</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                </project>
                """
            )
          )
        );

        Set<String> dirty = JavaSourceSet.dirtyProjects(ctx);
        assertThat(dirty).isNullOrEmpty();
    }
}
