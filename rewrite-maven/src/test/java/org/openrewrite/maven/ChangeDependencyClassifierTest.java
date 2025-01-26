/*
 * Copyright 2022 the original author or authors.
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

class ChangeDependencyClassifierTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ChangeDependencyClassifier("org.ehcache", "ehcache", "jakarta", false));
    }

    @DocumentExample
    @Test
    void noClassifierToClassifier() {
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
                    <groupId>org.ehcache</groupId>
                    <artifactId>ehcache</artifactId>
                    <version>3.10.0</version>
                  </dependency>
                </dependencies>
              
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.ehcache</groupId>
                      <artifactId>ehcache</artifactId>
                      <version>3.10.0</version>
                      <!-- classifier not added for managed dependencies by default -->
                    </dependency>
                  </dependencies>
                </dependencyManagement>
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
                    <groupId>org.ehcache</groupId>
                    <artifactId>ehcache</artifactId>
                    <version>3.10.0</version>
                    <classifier>jakarta</classifier>
                  </dependency>
                </dependencies>
              
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.ehcache</groupId>
                      <artifactId>ehcache</artifactId>
                      <version>3.10.0</version>
                      <!-- classifier not added for managed dependencies by default -->
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void noClassifierToClassifierManagedDependency() {
        rewriteRun(
          spec -> spec.recipe(
            new ChangeDependencyClassifier("org.ehcache", "ehcache", "jakarta", true)
          ),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
              
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              
                <dependencies>
                  <dependency>
                    <groupId>org.ehcache</groupId>
                    <artifactId>ehcache</artifactId>
                    <version>3.10.0</version>
                  </dependency>
                </dependencies>
              
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.ehcache</groupId>
                      <artifactId>ehcache</artifactId>
                      <version>3.10.0</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
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
                    <groupId>org.ehcache</groupId>
                    <artifactId>ehcache</artifactId>
                    <version>3.10.0</version>
                    <classifier>jakarta</classifier>
                  </dependency>
                </dependencies>
              
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.ehcache</groupId>
                      <artifactId>ehcache</artifactId>
                      <version>3.10.0</version>
                      <classifier>jakarta</classifier>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void addClassifierUsingGlobsExpressions() {
        rewriteRun(
          spec -> spec.recipe(
            new ChangeDependencyClassifier("org.ehcache", "*", "jakarta", false)
          ),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
              
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              
                <dependencies>
                  <dependency>
                    <groupId>org.ehcache</groupId>
                    <artifactId>ehcache</artifactId>
                    <version>3.10.0</version>
                  </dependency>
                  <dependency>
                    <groupId>org.ehcache</groupId>
                    <artifactId>ehcache-transactions</artifactId>
                    <version>3.10.0</version>
                  </dependency>
                </dependencies>
              
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.ehcache</groupId>
                      <artifactId>ehcache</artifactId>
                      <version>3.10.0</version>
                    </dependency>
                    <dependency>
                      <groupId>org.ehcache</groupId>
                      <artifactId>ehcache-transactions</artifactId>
                      <version>3.10.0</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
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
                    <groupId>org.ehcache</groupId>
                    <artifactId>ehcache</artifactId>
                    <version>3.10.0</version>
                    <classifier>jakarta</classifier>
                  </dependency>
                  <dependency>
                    <groupId>org.ehcache</groupId>
                    <artifactId>ehcache-transactions</artifactId>
                    <version>3.10.0</version>
                    <classifier>jakarta</classifier>
                  </dependency>
                </dependencies>
              
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.ehcache</groupId>
                      <artifactId>ehcache</artifactId>
                      <version>3.10.0</version>
                    </dependency>
                    <dependency>
                      <groupId>org.ehcache</groupId>
                      <artifactId>ehcache-transactions</artifactId>
                      <version>3.10.0</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void addClassifierUsingGlobsExpressionsManagedDependency() {
        rewriteRun(
          spec -> spec.recipe(
            new ChangeDependencyClassifier("org.ehcache", "*", "jakarta", true)
          ),
          pomXml(
            """
              <project>
                  <modelVersion>4.0.0</modelVersion>
              
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
              
                  <dependencies>
                      <dependency>
                          <groupId>org.ehcache</groupId>
                          <artifactId>ehcache</artifactId>
                          <version>3.10.0</version>
                      </dependency>
                      <dependency>
                          <groupId>org.ehcache</groupId>
                          <artifactId>ehcache-transactions</artifactId>
                          <version>3.10.0</version>
                      </dependency>
                  </dependencies>
              
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.ehcache</groupId>
                              <artifactId>ehcache</artifactId>
                              <version>3.10.0</version>
                          </dependency>
                          <dependency>
                              <groupId>org.ehcache</groupId>
                              <artifactId>ehcache-transactions</artifactId>
                              <version>3.10.0</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
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
                          <groupId>org.ehcache</groupId>
                          <artifactId>ehcache</artifactId>
                          <version>3.10.0</version>
                          <classifier>jakarta</classifier>
                      </dependency>
                      <dependency>
                          <groupId>org.ehcache</groupId>
                          <artifactId>ehcache-transactions</artifactId>
                          <version>3.10.0</version>
                          <classifier>jakarta</classifier>
                      </dependency>
                  </dependencies>
              
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>org.ehcache</groupId>
                              <artifactId>ehcache</artifactId>
                              <version>3.10.0</version>
                              <classifier>jakarta</classifier>
                          </dependency>
                          <dependency>
                              <groupId>org.ehcache</groupId>
                              <artifactId>ehcache-transactions</artifactId>
                              <version>3.10.0</version>
                              <classifier>jakarta</classifier>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void classifierToClassifier() {
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
                    <groupId>org.ehcache</groupId>
                    <artifactId>ehcache</artifactId>
                    <version>3.10.0</version>
                    <classifier>javax</classifier>
                  </dependency>
                </dependencies>
              
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.ehcache</groupId>
                      <artifactId>ehcache</artifactId>
                      <version>3.10.0</version>
                      <classifier>javax</classifier>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
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
                    <groupId>org.ehcache</groupId>
                    <artifactId>ehcache</artifactId>
                    <version>3.10.0</version>
                    <classifier>jakarta</classifier>
                  </dependency>
                </dependencies>
              
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.ehcache</groupId>
                      <artifactId>ehcache</artifactId>
                      <version>3.10.0</version>
                      <classifier>javax</classifier>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void classifierToClassifierManagedDependency() {
        rewriteRun(
          spec -> spec.recipe(
            new ChangeDependencyClassifier("org.ehcache", "ehcache", "jakarta", true)
          ),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
              
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              
                <dependencies>
                  <dependency>
                    <groupId>org.ehcache</groupId>
                    <artifactId>ehcache</artifactId>
                    <version>3.10.0</version>
                    <classifier>javax</classifier>
                  </dependency>
                </dependencies>
              
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.ehcache</groupId>
                      <artifactId>ehcache</artifactId>
                      <version>3.10.0</version>
                      <classifier>javax</classifier>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
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
                    <groupId>org.ehcache</groupId>
                    <artifactId>ehcache</artifactId>
                    <version>3.10.0</version>
                    <classifier>jakarta</classifier>
                  </dependency>
                </dependencies>
              
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.ehcache</groupId>
                      <artifactId>ehcache</artifactId>
                      <version>3.10.0</version>
                      <classifier>jakarta</classifier>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void classifierToNoClassifier() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier("org.ehcache", "ehcache", null, false)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
              
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              
                <dependencies>
                  <dependency>
                    <groupId>org.ehcache</groupId>
                    <artifactId>ehcache</artifactId>
                    <version>3.10.0</version>
                    <classifier>jakarta</classifier>
                  </dependency>
                </dependencies>
              
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.ehcache</groupId>
                      <artifactId>ehcache</artifactId>
                      <version>3.10.0</version>
                      <classifier>jakarta</classifier>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
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
                    <groupId>org.ehcache</groupId>
                    <artifactId>ehcache</artifactId>
                    <version>3.10.0</version>
                  </dependency>
                </dependencies>
              
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.ehcache</groupId>
                      <artifactId>ehcache</artifactId>
                      <version>3.10.0</version>
                      <classifier>jakarta</classifier>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void classifierToNoClassifierManagedDependency() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier("org.ehcache", "ehcache", null, true)),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
              
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              
                <dependencies>
                  <dependency>
                    <groupId>org.ehcache</groupId>
                    <artifactId>ehcache</artifactId>
                    <version>3.10.0</version>
                    <classifier>jakarta</classifier>
                  </dependency>
                </dependencies>
              
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.ehcache</groupId>
                      <artifactId>ehcache</artifactId>
                      <version>3.10.0</version>
                      <classifier>jakarta</classifier>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
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
                    <groupId>org.ehcache</groupId>
                    <artifactId>ehcache</artifactId>
                    <version>3.10.0</version>
                  </dependency>
                </dependencies>
              
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>org.ehcache</groupId>
                      <artifactId>ehcache</artifactId>
                      <version>3.10.0</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }
}
