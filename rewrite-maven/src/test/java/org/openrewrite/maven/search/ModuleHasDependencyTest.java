/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.*;
import static org.openrewrite.maven.Assertions.pomXml;

class ModuleHasDependencyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ModuleHasDependency("org.openrewrite", "rewrite-maven", null, "8.52.0", null));
    }

    @DocumentExample
    @Test
    void findDependency() {
        rewriteRun(
          mavenProject("multi-project-build",
            mavenProject("project-uses-rewrite-maven",
              pomXml("""
                <project>
                    <groupId>org.openrewrite.example</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                    <dependencies>
                        <dependency>
                            <groupId>org.openrewrite</groupId>
                            <artifactId>rewrite-maven</artifactId>
                            <version>8.52.0</version>
                        </dependency>
                    </dependencies>
                </project>
                """,
                """
               <!--~~(Module has dependency: org.openrewrite:rewrite-maven:8.52.0)~~>--><project>
                   <groupId>org.openrewrite.example</groupId>
                   <artifactId>my-app</artifactId>
                   <version>1</version>
                   <dependencies>
                       <dependency>
                           <groupId>org.openrewrite</groupId>
                           <artifactId>rewrite-maven</artifactId>
                           <version>8.52.0</version>
                       </dependency>
                   </dependencies>
               </project>
               """
              ),
              srcMainJava(
                java("""
                    class A {}
                    """,
                  """
                    /*~~(Module has dependency: org.openrewrite:rewrite-maven:8.52.0)~~>*/class A {}
                    """)
              )
            ),
            mavenProject("other-project",
              pomXml(
                """
                  <project>
                      <groupId>org.openrewrite.example</groupId>
                      <artifactId>other-project</artifactId>
                      <version>1</version>
                      <build>
                          <plugins>
                              <plugin>
                                  <groupId>org.openrewrite</groupId>
                                  <artifactId>not-rewrite-maven</artifactId>
                                  <version>3.0.0</version>
                              </plugin>
                          </plugins>
                      </build>
                  </project>
                  """
              ),
              srcMainJava(
                java("""
                  class B {}
                  """)
              )
            )
          )
        );
    }
}
