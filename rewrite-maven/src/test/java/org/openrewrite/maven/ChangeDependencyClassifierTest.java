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
        spec.recipe(new ChangeDependencyClassifier("org.ehcache", "ehcache", "jakarta"));
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
              </project>
              """
          )
        );
    }

    @Test
    void addClassifierUsingGlobsExpressions() {
        rewriteRun(
          spec -> spec.recipe(
            new ChangeDependencyClassifier("org.ehcache", "*", "jakarta")
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
              </project>
              """
          )
        );
    }

    @Test
    void classifierToNoClassifier() {
        rewriteRun(
          spec -> spec.recipe(new ChangeDependencyClassifier("org.ehcache", "ehcache", null)),
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
              </project>
              """
          )
        );
    }
}
