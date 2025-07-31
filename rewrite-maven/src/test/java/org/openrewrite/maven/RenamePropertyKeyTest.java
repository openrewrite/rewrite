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
package org.openrewrite.maven;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;

class RenamePropertyKeyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RenamePropertyKey("guava.version", "version.com.google.guava"));
    }

    @DocumentExample
    @Test
    void propertyInDependency() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <properties>
                  <guava.version>29.0-jre</guava.version>
                </properties>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>${guava.version}</version>
                  </dependency>
                </dependencies>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <properties>
                  <version.com.google.guava>29.0-jre</version.com.google.guava>
                </properties>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencies>
                  <dependency>
                    <groupId>com.google.guava</groupId>
                    <artifactId>guava</artifactId>
                    <version>${version.com.google.guava}</version>
                  </dependency>
                </dependencies>
              </project>
              """
          )
        );
    }

    @Test
    void propertyInDependencyManagement() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <properties>
                  <guava.version>29.0-jre</guava.version>
                </properties>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>${guava.version}</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <properties>
                  <version.com.google.guava>29.0-jre</version.com.google.guava>
                </properties>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <dependencyManagement>
                  <dependencies>
                    <dependency>
                      <groupId>com.google.guava</groupId>
                      <artifactId>guava</artifactId>
                      <version>${version.com.google.guava}</version>
                    </dependency>
                  </dependencies>
                </dependencyManagement>
              </project>
              """
          )
        );
    }

    @Test
    void propertyInProperty() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <properties>
                  <guava.version>29.0-jre</guava.version>
                  <abc>${guava.version}</abc>
                  <def>prefix ${guava.version}</def>
                  <xyz>${guava.version} suffix ${abc}</xyz>
                </properties>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <properties>
                  <version.com.google.guava>29.0-jre</version.com.google.guava>
                  <abc>${version.com.google.guava}</abc>
                  <def>prefix ${version.com.google.guava}</def>
                  <xyz>${version.com.google.guava} suffix ${abc}</xyz>
                </properties>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void renamePropertyAndValue() {
        String yamlRecipe = """
          ---
          type: specs.openrewrite.org/v1beta/recipe
          name: org.openrewrite.RenamePropertyAndValue
          displayName: RenamePropertyAndValue
          description: RenamePropertyAndValue description.
          recipeList:
            - org.openrewrite.maven.RenamePropertyKey:
                  oldKey: "abc"
                  newKey: "def"
            - org.openrewrite.maven.ChangePropertyValue:
                  key: "def"
                  newValue: "2.0"
          """;
        rewriteRun(
          spec -> spec.recipeFromYaml(yamlRecipe, "org.openrewrite.RenamePropertyAndValue"),
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <properties>
                  <abc>1.0</abc>
                </properties>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <properties>
                  <def>2.0</def>
                </properties>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """
          )
        );
    }

    @Test
    void nothingToRename() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>

                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>

                <properties>
                  <a.version>a</a.version>
                  <bla.version>b</bla.version>
                </properties>
              </project>
              """
          )
        );
    }

}
