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
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class AddManagedPluginTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        // the recipe uses the add AddPluginVisitor which is extensively tested inAddPluginTest, this test only contains the differing behavior
        spec.recipe(new AddManagedPlugin("org.openrewrite.maven", "rewrite-maven-plugin", "100.0", null, null, null, null));
    }

    @DocumentExample
    @Test
    void addPlugin() {
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
                <build>
                  <pluginManagement>
                    <plugins>
                      <plugin>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>rewrite-maven-plugin</artifactId>
                        <version>100.0</version>
                      </plugin>
                    </plugins>
                  </pluginManagement>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void donotInferBuildPlugins() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <artifactId>maven-compiler-plugin</artifactId>
                    </plugin>
                  </plugins>
                </build>
              </project>
              """,
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <artifactId>maven-compiler-plugin</artifactId>
                    </plugin>
                  </plugins>
                  <pluginManagement>
                    <plugins>
                      <plugin>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>rewrite-maven-plugin</artifactId>
                        <version>100.0</version>
                      </plugin>
                    </plugins>
                  </pluginManagement>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void dontDuplicate() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <pluginManagement>
                    <plugins>
                      <plugin>
                        <groupId>org.openrewrite.maven</groupId>
                        <artifactId>rewrite-maven-plugin</artifactId>
                        <version>100.0</version>
                      </plugin>
                    </plugins>
                  </pluginManagement>
                </build>
              </project>
              """
          )
        );
    }

    @Test
    void dontDuplicateMinimalCompiler() {
        rewriteRun(
          spec -> spec.recipe(new AddManagedPlugin("org.apache.maven.plugins", "maven-compiler-plugin", null, null, null, null, null)),
          pomXml(
            """
              <project>
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <pluginManagement>
                    <plugins>
                      <plugin>
                        <artifactId>maven-compiler-plugin</artifactId>
                      </plugin>
                    </plugins>
                  </pluginManagement>
                </build>
              </project>
              """
          )
        );
    }
}
