/*
 * Copyright 2023 the original author or authors.
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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class IncrementProjectVersionTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new IncrementProjectVersion("*", "*", IncrementProjectVersion.SemverDigit.MINOR));
    }

    @Test
    void changeProjectVersion() {
        rewriteRun(
          pomXml(
           """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-maven</artifactId>
                  <version>8.40.1-SNAPSHOT</version>
              </project>
              """,
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-maven</artifactId>
                  <version>8.41.0-SNAPSHOT</version>
              </project>
              """
          )
        );
    }

    @Test
    void extraFourthDigit() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-maven</artifactId>
                  <version>8.4.1.1-SNAPSHOT</version>
              </project>
              """,
            """
              <project>
                  <groupId>org.openrewrite</groupId>
                  <artifactId>rewrite-maven</artifactId>
                  <version>8.5.0.0-SNAPSHOT</version>
              </project>
              """
          )
        );
    }

    @Test
    void incrementParentVersion() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.mycompany</groupId>
                  <artifactId>my-parent</artifactId>
                  <version>1.0.0</version>
              </project>
              """,
            """
              <project>
                  <groupId>com.mycompany</groupId>
                  <artifactId>my-parent</artifactId>
                  <version>1.1.0</version>
              </project>
              """
          ),
          mavenProject("my-child",
            pomXml(
              """
                <project>
                    <parent>
                        <groupId>com.mycompany</groupId>
                        <artifactId>my-parent</artifactId>
                        <version>1.0.0</version>
                    </parent>
                    <groupId>com.mycompany</groupId>
                    <artifactId>my-child</artifactId>
                </project>
                """,
              """
                <project>
                    <parent>
                        <groupId>com.mycompany</groupId>
                        <artifactId>my-parent</artifactId>
                        <version>1.1.0</version>
                    </parent>
                    <groupId>com.mycompany</groupId>
                    <artifactId>my-child</artifactId>
                </project>
                """
            )
          )
        );
    }
}
