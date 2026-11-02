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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpecs;

import static org.openrewrite.maven.Assertions.pomXml;

class RemoveInvalidParentRelativePathTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveInvalidParentRelativePath());
    }

    private static SourceSpecs parent() {
        return pomXml(
          """
            <project>
                <groupId>com.example</groupId>
                <artifactId>parent</artifactId>
                <version>1.0.0</version>
                <packaging>pom</packaging>
            </project>
            """
        );
    }

    @DocumentExample
    @Test
    void replacesCoordinatesWithEmptyRelativePath() {
        rewriteRun(
          parent(),
          pomXml(
            """
              <project>
                  <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0.0</version>
                      <relativePath>com.example:parent:1.0.0</relativePath>
                  </parent>
                  <artifactId>child</artifactId>
              </project>
              """,
            """
              <project>
                  <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0.0</version>
                      <relativePath/>
                  </parent>
                  <artifactId>child</artifactId>
              </project>
              """,
            spec -> spec.path("child/pom.xml")
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"C:\\parent\\pom.xml", "parent?", "&lt;parent&gt;", "&quot;../pom.xml&quot;"})
    void replacesOtherReservedCharacters(String relativePath) {
        rewriteRun(
          parent(),
          pomXml(
            """
              <project>
                  <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0.0</version>
                      <relativePath>%s</relativePath>
                  </parent>
                  <artifactId>child</artifactId>
              </project>
              """.formatted(relativePath),
            """
              <project>
                  <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0.0</version>
                      <relativePath/>
                  </parent>
                  <artifactId>child</artifactId>
              </project>
              """,
            spec -> spec.path("child/pom.xml")
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"<relativePath>../pom.xml</relativePath>", "<relativePath>..\\parent</relativePath>", "<relativePath/>"})
    void keepsValidRelativePath(String relativePath) {
        rewriteRun(
          parent(),
          pomXml(
            """
              <project>
                  <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0.0</version>
                      %s
                  </parent>
                  <artifactId>child</artifactId>
              </project>
              """.formatted(relativePath),
            spec -> spec.path("child/pom.xml")
          )
        );
    }

    @Test
    void keepsRelativePathOutsideParent() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>1.0.0</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>com.example</groupId>
                              <artifactId>example-maven-plugin</artifactId>
                              <version>1.0.0</version>
                              <configuration>
                                  <relativePath>com.example:parent:1.0.0</relativePath>
                              </configuration>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          )
        );
    }
}
