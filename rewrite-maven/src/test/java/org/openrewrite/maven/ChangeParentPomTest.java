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
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

public class ChangeParentPomTest implements RewriteTest {

    @Test
    void changeParent() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            "com.fasterxml.jackson",
            "spring-boot-starter-parent",
            "jackson-parent",
            "2.12",
            null,
            false
          )),
          pomXml(
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>1.5.12.RELEASE</version>
                    </parent>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """,
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <parent>
                      <groupId>com.fasterxml.jackson</groupId>
                      <artifactId>jackson-parent</artifactId>
                      <version>2.12</version>
                    </parent>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """
          )
        );
    }

    @Test
    void upgradeVersion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            null,
            "spring-boot-starter-parent",
            null,
            "~1.5",
            null,
            false
          )),
          pomXml(
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>1.5.12.RELEASE</version>
                      <relativePath/> <!-- lookup parent from repository -->
                    </parent>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """,
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>1.5.22.RELEASE</version>
                      <relativePath/> <!-- lookup parent from repository -->
                    </parent>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """
          )
        );
    }

    @Test
    void upgradeToExactVersion() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            null,
            "spring-boot-starter-parent",
            null,
            "1.5.22.RELEASE",
            null,
            false
          )),
          pomXml(
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>1.5.12.RELEASE</version>
                      <relativePath/> <!-- lookup parent from repository -->
                    </parent>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """,
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>1.5.22.RELEASE</version>
                      <relativePath/> <!-- lookup parent from repository -->
                    </parent>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """
          )
        );
    }

    @Test
    void doNotDowngradeToLowerVersionWhenArtifactsAreTheSame() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            null,
            "spring-boot-starter-parent",
            null,
            "1.5.12.RELEASE",
            null,
            false
          )),
          pomXml(
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>1.5.22.RELEASE</version>
                      <relativePath/> <!-- lookup parent from repository -->
                    </parent>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """
          )
        );
    }

    @Test
    void downgradeToLowerVersionWhenFlagisSet() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.springframework.boot",
            null,
            "spring-boot-starter-parent",
            null,
            "1.5.12.RELEASE",
            null,
            true
          )),
          pomXml(
            """
                    <project>
                      <modelVersion>4.0.0</modelVersion>
                      
                      <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>1.5.22.RELEASE</version>
                        <relativePath/> <!-- lookup parent from repository -->
                      </parent>
                      
                      <groupId>com.mycompany.app</groupId>
                      <artifactId>my-app</artifactId>
                      <version>1</version>
                    </project>
              """,
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>1.5.12.RELEASE</version>
                      <relativePath/> <!-- lookup parent from repository -->
                    </parent>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """
          )
        );
    }

    @Test
    void wildcardVersionUpdate() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "*",
            null,
            "*",
            null,
            "~1.5",
            null,
            false
          )),
          pomXml(
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>1.5.12.RELEASE</version>
                      <relativePath/> <!-- lookup parent from repository -->
                    </parent>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """,
            """
                  <project>
                    <modelVersion>4.0.0</modelVersion>
                    
                    <parent>
                      <groupId>org.springframework.boot</groupId>
                      <artifactId>spring-boot-starter-parent</artifactId>
                      <version>1.5.22.RELEASE</version>
                      <relativePath/> <!-- lookup parent from repository -->
                    </parent>
                    
                    <groupId>com.mycompany.app</groupId>
                    <artifactId>my-app</artifactId>
                    <version>1</version>
                  </project>
              """
          )
        );
    }

    @Test
    void removesRedundantExplicitVersionsMatchingOldParent() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.junit",
            null,
            "junit-bom",
            null,
            "5.9.1",
            null,
            false
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.junit</groupId>
                  <artifactId>junit-bom</artifactId>
                  <version>5.9.0</version>
                  <relativePath/>
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <version>5.9.0</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.junit</groupId>
                  <artifactId>junit-bom</artifactId>
                  <version>5.9.1</version>
                  <relativePath/>
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void removesRedundantExplicitVersionsMatchingNewParent() {
        rewriteRun(
          spec -> spec.recipe(new ChangeParentPom(
            "org.junit",
            null,
            "junit-bom",
            null,
            "5.9.1",
            null,
            false
          )),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.junit</groupId>
                  <artifactId>junit-bom</artifactId>
                  <version>5.9.0</version>
                  <relativePath/>
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                    <version>5.9.1</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                  <groupId>org.junit</groupId>
                  <artifactId>junit-bom</artifactId>
                  <version>5.9.1</version>
                  <relativePath/>
                </parent>
                
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
                
                <dependencies>
                  <dependency>
                    <groupId>org.junit.jupiter</groupId>
                    <artifactId>junit-jupiter-api</artifactId>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }
}
