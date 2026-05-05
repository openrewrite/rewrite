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
package org.openrewrite.maven.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class AddProjectBuildOutputTimestampTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AddProjectBuildOutputTimestamp(null));
    }

    @DocumentExample
    @Test
    void addsDefaultTimestamp() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <project.build.outputTimestamp>1980-01-01T00:00:00Z</project.build.outputTimestamp>
                </properties>
              </project>
              """
          )
        );
    }

    @Test
    void respectsCustomTimestamp() {
        rewriteRun(
          spec -> spec.recipe(new AddProjectBuildOutputTimestamp("${git.commit.author.time}")),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <project.build.outputTimestamp>${git.commit.author.time}</project.build.outputTimestamp>
                </properties>
              </project>
              """
          )
        );
    }

    @Test
    void preservesExistingValue() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <project.build.outputTimestamp>2024-06-15T12:00:00Z</project.build.outputTimestamp>
                </properties>
              </project>
              """
          )
        );
    }

    @Test
    void addsToExistingPropertiesBlock() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <maven.compiler.release>21</maven.compiler.release>
                </properties>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <properties>
                  <maven.compiler.release>21</maven.compiler.release>
                  <project.build.outputTimestamp>1980-01-01T00:00:00Z</project.build.outputTimestamp>
                </properties>
              </project>
              """
          )
        );
    }
}
