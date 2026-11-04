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
package org.openrewrite.maven.cleanup;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class NoSystemScopeDependenciesTest implements RewriteTest {


    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new NoSystemScopeDependencies());
    }


    @Test
    void removesSystemScopeAndSystemPath() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0-SNAPSHOT</version>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <scope>system</scope>
                    <systemPath>${project.basedir}/lib/guava-29.0-jre.jar</systemPath>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0-SNAPSHOT</version>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void removesSystemScopeWithoutSystemPath() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0-SNAPSHOT</version>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <scope>system</scope>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0-SNAPSHOT</version>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void removesSystemScopeInDependencyManagement() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0-SNAPSHOT</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                      <scope>system</scope>
                      <systemPath>${project.basedir}/lib/guava-29.0-jre.jar</systemPath>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """,
            """
              <project>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0-SNAPSHOT</version>
                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>29.0-jre</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void removesSystemScopeWhenVersionIsProperty() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0-SNAPSHOT</version>
                <properties>
                  <guava.version>29.0-jre</guava.version>
                </properties>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>${guava.version}</version>
                    <scope>system</scope>
                    <systemPath>${project.basedir}/lib/guava-29.0-jre.jar</systemPath>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0-SNAPSHOT</version>
                <properties>
                  <guava.version>29.0-jre</guava.version>
                </properties>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>${guava.version}</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void doesNotRemoveSystemScopeWhenNotInRepo() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0-SNAPSHOT</version>
                <dependencies>
                  <dependency>
                    <groupId>com.example.local</groupId>
                    <artifactId>proprietary-lib</artifactId>
                    <version>1.0</version>
                    <scope>system</scope>
                    <systemPath>${project.basedir}/lib/proprietary-lib-1.0.jar</systemPath>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void doesNotModifyNonSystemScope() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0-SNAPSHOT</version>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                    <scope>test</scope>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void doesNotModifyDependencyWithoutScope() {
        rewriteRun(
          pomXml(
            """
              <project>
                <groupId>test</groupId>
                <artifactId>test</artifactId>
                <version>1.0-SNAPSHOT</version>
                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>29.0-jre</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }
}
