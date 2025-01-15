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
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class RemovePropertyTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new RemoveProperty("bla.version"));
    }

    @DocumentExample
    @Test
    void removeProperty() {
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
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
              
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              
                <properties>
                  <a.version>a</a.version>
                </properties>
              </project>
              """,
            sourceSpecs ->
                sourceSpecs.afterRecipe(d -> {
                    MavenResolutionResult resolution = d.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                    Map<String, String> properties = resolution.getPom().getRequested().getProperties();
                    assertThat(properties.get("a.version")).isEqualTo("a");
                    assertThat(properties.get("bla.version")).isNull();
                })
          )
        );
    }

    @Test
    void removeOnlyProperty() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
              
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              
                <properties>
                  <bla.version>b</bla.version>
                </properties>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
              
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              </project>
              """,
            sourceSpecs ->
                sourceSpecs.afterRecipe(d -> {
                    MavenResolutionResult resolution = d.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                    Map<String, String> properties = resolution.getPom().getRequested().getProperties();
                    assertThat(properties.isEmpty()).isTrue();
                })
          )
        );
    }

    @Test
    void removePropertyWithComment() {
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
                  <!-- I should remove this property -->
                  <bla.version>b</bla.version>
                </properties>
              </project>
              """,
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
              
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              
                <properties>
                  <a.version>a</a.version>
                </properties>
              </project>
              """,
            sourceSpecs ->
              sourceSpecs.afterRecipe(d -> {
                  MavenResolutionResult resolution = d.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                  Map<String, String> properties = resolution.getPom().getRequested().getProperties();
                  assertThat(properties.get("a.version")).isEqualTo("a");
                  assertThat(properties.get("bla.version")).isNull();
              })
          )
        );
    }

    @Test
    void removePropertyWithCommentAndEmptyParents() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
              
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              
                <properties>
                  <!-- I should remove this property -->
                  <bla.version>b</bla.version>
                </properties>
              </project>
              """,
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
    void removePropertyWithTwoComments() {
        rewriteRun(
          pomXml(
            """
              <project>
                <modelVersion>4.0.0</modelVersion>
              
                <groupId>com.mycompany.app</groupId>
                <artifactId>my-app</artifactId>
                <version>1</version>
              
                <properties>
                  <!-- And also remove this comment -->
                  <!-- I should remove this property -->
                  <bla.version>b</bla.version>
                </properties>
              </project>
              """,
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
}
