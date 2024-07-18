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
package org.openrewrite.maven.trait;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.marker.SearchResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.maven.Assertions.pomXml;
import static org.openrewrite.maven.trait.Traits.mavenPlugin;

class MavenPluginTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(RewriteTest.toRecipe(() -> mavenPlugin().asVisitor(plugin ->
          SearchResult.found(plugin.getTree(), plugin.getGroupId() + ":" + plugin.getArtifactId()))));
    }

    @DocumentExample
    @Test
    void findMavenPlugin() {
        rewriteRun(
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <build>
                      <plugins>
                          <plugin>
                              <groupId>io.quarkus</groupId>
                              <artifactId>quarkus-bootstrap-maven-plugin</artifactId>
                              <version>3.0.0.Beta1</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """,
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <build>
                      <plugins>
                          <!--~~(io.quarkus:quarkus-bootstrap-maven-plugin)~~>--><plugin>
                              <groupId>io.quarkus</groupId>
                              <artifactId>quarkus-bootstrap-maven-plugin</artifactId>
                              <version>3.0.0.Beta1</version>
                          </plugin>
                      </plugins>
                  </build>
              </project>
              """
          )
        );
    }
}
