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
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class ExcludeDependencyTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new ExcludeDependency("org.junit.vintage", "junit-vintage-engine", null));
    }

    @Test
    void excludeJUnitVintageEngineSpringBoot2_3() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>2.3.6.RELEASE</version>
                </parent>
                
                <groupId>com.example</groupId>
                <artifactId>demo</artifactId>
                <version>0.0.1-SNAPSHOT</version>
                <dependencies>
                  <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter</artifactId>
                  </dependency>
                  <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-test</artifactId>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>2.3.6.RELEASE</version>
                </parent>
                
                <groupId>com.example</groupId>
                <artifactId>demo</artifactId>
                <version>0.0.1-SNAPSHOT</version>
                <dependencies>
                  <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter</artifactId>
                  </dependency>
                  <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-test</artifactId>
                    <scope>test</scope>
                    <exclusions>
                      <exclusion>
                        <groupId>org.junit.vintage</groupId>
                        <artifactId>junit-vintage-engine</artifactId>
                      </exclusion>
                    </exclusions>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void jUnitVintageEngineDoesntNeedExclusionFromSpringBoot2_4() {
        rewriteRun(
          pomXml(
            """
              <project>
                <parent>
                  <groupId>org.springframework.boot</groupId>
                  <artifactId>spring-boot-starter-parent</artifactId>
                  <version>2.4.0</version>
                </parent>
                
                <groupId>com.example</groupId>
                <artifactId>demo</artifactId>
                <dependencies>
                  <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-test</artifactId>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/407")
    @Test
    void dontExcludeADependencyFromItself() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeDependency("junit", "junit", "compile")),
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>0.0.1-SNAPSHOT</version>
                  <dependencies>
                      <dependency>
                          <groupId>junit</groupId>
                          <artifactId>junit</artifactId>
                          <version>4.13.2</version>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void excludeJUnitInCompileScope() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeDependency("junit", "junit", "compile")),
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>0.0.1-SNAPSHOT</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.neo4j</groupId>
                          <artifactId>neo4j-ogm-core</artifactId>
                          <version>3.2.21</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>0.0.1-SNAPSHOT</version>
                  <dependencies>
                      <dependency>
                          <groupId>org.neo4j</groupId>
                          <artifactId>neo4j-ogm-core</artifactId>
                          <version>3.2.21</version>
                          <exclusions>
                              <exclusion>
                                  <groupId>junit</groupId>
                                  <artifactId>junit</artifactId>
                              </exclusion>
                          </exclusions>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void dontExcludeJUnitWhenItIsTransitiveTestDependency() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeDependency("junit", "junit", null)),
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>demo</artifactId>
                  <version>0.0.1-SNAPSHOT</version>
                  <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot</artifactId>
                    <version>2.4.10</version>
                  </dependency>
              </project>
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1886")
    @Test
    void respectTransitiveDependencyOriginalScopeWhenDeterminingIfExclusionIsNecessary() {
        rewriteRun(
          spec -> spec.recipe(new ExcludeDependency("junit", "junit", null)),
          pomXml(
             """
          <project>
            <parent>
              <groupId>org.springframework.boot</groupId>
              <artifactId>spring-boot-starter-parent</artifactId>
              <version>2.5.14</version>
            </parent>
            <groupId>com.example</groupId>
            <artifactId>demo</artifactId>
            <dependencies>
              <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-actuator</artifactId>
              </dependency>
            </dependencies>
          </project>
          """)
        );
    }
}
