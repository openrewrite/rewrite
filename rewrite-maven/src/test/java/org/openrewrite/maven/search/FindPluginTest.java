/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.maven.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class FindPluginTest implements RewriteTest {

    @DocumentExample
    @Test
    void findProperty() {
        rewriteRun(
          spec -> spec.recipe(new FindPlugin("org.openrewrite.maven", "rewrite-maven-plugin")),
          pomXml(
            """
              <project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <plugin>
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                      <version>4.0.0</version>
                    </plugin>
                  </plugins>
                </build>
                <reporting>
                  <plugins>
                    <plugin>
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                      <version>4.0.0</version>
                    </plugin>
                  </plugins>
                </reporting>
              </project>
              """,
            """
              <project>
                <groupId>org.openrewrite.example</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <build>
                  <plugins>
                    <!--~~>--><plugin>
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                      <version>4.0.0</version>
                    </plugin>
                  </plugins>
                </build>
                <reporting>
                  <plugins>
                    <!--~~>--><plugin>
                      <groupId>org.openrewrite.maven</groupId>
                      <artifactId>rewrite-maven-plugin</artifactId>
                      <version>4.0.0</version>
                    </plugin>
                  </plugins>
                </reporting>
              </project>
              """
          )
        );
    }

    @Test
    void multiModulePrecondition() throws Exception {
        rewriteRun(
          spec ->  spec.recipeFromYaml("""
            ---
            type: specs.openrewrite.org/v1beta/recipe
            name: org.openrewrite.MultiModuleTest
            description: Test.
            preconditions:
              - org.openrewrite.maven.search.FindPlugin:
                  groupId: org.apache.maven.plugins
                  artifactId: maven-compiler-plugin
            recipeList:
              - org.openrewrite.maven.RemoveDependency:
                    groupId: com.google.guava
                    artifactId: guava
            """, "org.openrewrite.MultiModuleTest"),
          pomXml(
            """
                  <project>
                      <groupId>org.example</groupId>
                      <artifactId>mvn_multi_module_test</artifactId>
                      <version>1.0-SNAPSHOT</version>
                      <packaging>pom</packaging>
                      <modules>
                          <module>submodule</module>
                      </modules>
                      <build>
                          <plugins>
                              <plugin>
                                  <groupId>org.openrewrite.maven</groupId>
                                  <artifactId>rewrite-maven-plugin</artifactId>
                                  <version>5.28.0</version>
                              </plugin>
                          </plugins>
                      </build>
                  </project>
                  """,
                sourceSpecs -> sourceSpecs.path("pom.xml")),
          pomXml(
            """
                <project>
                    <parent>
                        <groupId>org.example</groupId>
                        <artifactId>mvn_multi_module_test</artifactId>
                        <version>1.0-SNAPSHOT</version>
                    </parent>
                    <artifactId>submodule</artifactId>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.codehaus.mojo</groupId>
                                <artifactId>findbugs-maven-plugin</artifactId>
                                <version>3.0.5</version>
                            </plugin>
                        </plugins>
                    </build>
                </project>
                """,
                sourceSpecs -> sourceSpecs.path("submodule.pom.xml")
          )
        );
    }
}
