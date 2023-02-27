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
package org.openrewrite.maven.cleanup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

public class RemoveCompileScopeTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveCompileScope());
    }

    @Test
    void removeCompileScope() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
              
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>28.2-jre</version>
                    <scope>compile</scope>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
              
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>28.2-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void multipleDependencies() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
              
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>28.2-jre</version>
                    <scope>compile</scope>
                  </dependency>
                  <dependency>
                   <groupId>org.springframework</groupId>
                    <artifactId>spring-web</artifactId>
                    <version>6.0.5</version>
                    <scope>compile</scope>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
              
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>28.2-jre</version>
                  </dependency>
                  <dependency>
                   <groupId>org.springframework</groupId>
                    <artifactId>spring-web</artifactId>
                    <version>6.0.5</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"provided","runtime","test","system"})
    void otherScopesShouldNotBeTouched(String scope) {
        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>28.2-jre</version>
                    <scope>%s</scope>
                  </dependency>
                </dependencies>
              </project>
              """.formatted(scope)
          )
        );
    }

    @Test
    void missingScopeShouldNotBeTouched() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>28.2-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void testDependenciesManagementIsUntouched() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>group-a</groupId>
                            <artifactId>artifact-a</artifactId>
                            <version>1.0</version>
                        </dependency>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>28.2-jre</version>
                            <scope>compile</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void overridenDependencyManagementScopeIsUntouched() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                <dependencyManagement>
                    <dependencies>
                        <dependency>
                            <groupId>com.google.guava</groupId>
                            <artifactId>guava</artifactId>
                            <version>28.2-jre</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </dependencyManagement>
                <dependencies>
                    <dependency>
                        <groupId>com.google.guava</groupId>
                        <artifactId>guava</artifactId>
                        <version>28.2-jre</version>
                        <scope>compile</scope>
                    </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }
}
