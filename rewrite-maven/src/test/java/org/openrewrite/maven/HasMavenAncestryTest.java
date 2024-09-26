/*
 * Copyright 2024 the original author or authors.
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
import org.openrewrite.test.SourceSpec;

import static org.openrewrite.java.Assertions.mavenProject;
import static org.openrewrite.maven.Assertions.pomXml;

class HasMavenAncestryTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new HasMavenAncestry(
          "org.springframework.boot",
          "spring-boot-starter-parent",
          null
        ));
    }

    @Test
    void noParent() {
        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void directParentMatches() {
        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
              </parent>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              </project>
              """,
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
              	<!--HasMavenAncestry: org.springframework.boot:spring-boot-starter-parent-->
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void directParentMatchesFullGAV() {
        rewriteRun(
          spec -> spec.recipe(new HasMavenAncestry("org.springframework.boot", "spring-boot-starter-parent", "3.3.3")),
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
              </parent>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              </project>
              """,
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
              	<!--HasMavenAncestry: org.springframework.boot:spring-boot-starter-parent:3.3.3-->
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void directParentMatchesGAVMinorVersion() {
        rewriteRun(
          spec -> spec.recipe(new HasMavenAncestry("org.springframework.boot", "spring-boot-starter-parent", "3.3.x")),
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
              </parent>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              </project>
              """,
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
              	<!--HasMavenAncestry: org.springframework.boot:spring-boot-starter-parent:3.3.x-->
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void directParentMatchesGroupIdGlob() {
        rewriteRun(
          spec -> spec.recipe(new HasMavenAncestry("org.springframework.*", "spring-boot-starter-parent", null)),
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
              </parent>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              </project>
              """,
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
              	<!--HasMavenAncestry: org.springframework.*:spring-boot-starter-parent-->
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void directParentMatchesArtifactIdGlob() {
        rewriteRun(
          spec -> spec.recipe(new HasMavenAncestry("org.springframework.boot", "spring-*-parent", null)),
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
              </parent>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              </project>
              """,
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
              	<!--HasMavenAncestry: org.springframework.boot:spring-*-parent-->
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void indirectParentMatches() {
        rewriteRun(
          spec -> spec.recipe(new HasMavenAncestry("org.springframework.boot", "spring-boot-dependencies", null)),
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
              </parent>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              </project>
              """,
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
              	<!--HasMavenAncestry: org.springframework.boot:spring-boot-dependencies-->
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void indirectParentMatchesGAVPattern() {
        rewriteRun(
          spec -> spec.recipe(new HasMavenAncestry("*.springframework.*", "spring-*-dependencies", "3.x")),
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
              </parent>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              </project>
              """,
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
              	<!--HasMavenAncestry: *.springframework.*:spring-*-dependencies:3.x-->
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void multiModuleParentMatches() {
        rewriteRun(
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
              </parent>
              <modules>
              	<module>child</module>
              </modules>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              </project>
              """,
            SourceSpec::skip
          ),
          mavenProject("child",
            pomXml(
              //language=xml
              """
                <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                	<groupId>com.mycompany.app</groupId>
                	<artifactId>my-app</artifactId>
                	<version>1</version>
                </parent>
                
                <artifactId>child</artifactId>
                </project>
                """,
              //language=xml
              """
                <project>
                <modelVersion>4.0.0</modelVersion>
                
                <parent>
                	<!--HasMavenAncestry: org.springframework.boot:spring-boot-starter-parent-->
                	<groupId>com.mycompany.app</groupId>
                	<artifactId>my-app</artifactId>
                	<version>1</version>
                </parent>
                
                <artifactId>child</artifactId>
                </project>
                """
            )
          )
        );
    }

    @Test
    void groupIdDoesNotMatch() {
        rewriteRun(
          spec -> spec.recipe(new HasMavenAncestry("org.springframework.invalid", "spring-boot-starter-parent", null)),
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void artifactIdDoesNotMatch() {
        rewriteRun(
          spec -> spec.recipe(new HasMavenAncestry("org.springframework.boot", "spring-boot-starter-web", null)),
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void versionDoesNotMatch() {
        rewriteRun(
          spec -> spec.recipe(new HasMavenAncestry("org.springframework.boot", "spring-boot-starter-parent", "3.3.4")),
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void minorVersionDoesNotMatch() {
        rewriteRun(
          spec -> spec.recipe(new HasMavenAncestry("org.springframework.boot", "spring-boot-starter-parent", "3.3.x")),
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.0.5</version>
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
    void doesNotMatchGroupIdGlob() {
        rewriteRun(
          spec -> spec.recipe(new HasMavenAncestry("org.invalid.*", "spring-boot-starter-parent", null)),
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
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
    void doesNotMatchArtifactIdGlob() {
        rewriteRun(
          spec -> spec.recipe(new HasMavenAncestry("org.springframework.boot", "spring-boot-*-web", null)),
          pomXml(
            //language=xml
            """
              <project>
              <modelVersion>4.0.0</modelVersion>
              
              <parent>
              	<groupId>org.springframework.boot</groupId>
              	<artifactId>spring-boot-starter-parent</artifactId>
              	<version>3.3.3</version>
              </parent>
              
              <groupId>com.mycompany.app</groupId>
              <artifactId>my-app</artifactId>
              <version>1</version>
              </project>
              """
          )
        );
    }
}
