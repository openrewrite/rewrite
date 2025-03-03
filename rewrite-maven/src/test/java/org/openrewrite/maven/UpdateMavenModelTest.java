/*
 * Copyright 2025 the original author or authors.
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
import org.openrewrite.java.JavaParser;
import org.openrewrite.maven.tree.MavenResolutionResult;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class UpdateMavenModelTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.parser(JavaParser.fromJavaVersion()
          .classpath("guava", "jackson-databind"));
    }

    @Test
    void mavenUserPropertiesExistAfterUpdateMavenModel() {
        rewriteRun(
          spec -> spec
            .parser(MavenParser.builder()
                .property("revision", "1.0.0"))
                .recipes(
              new AddDependency("com.google.guava", "guava","29.0-jre", null, null, true, null, null, null,false, null, null),
              new AddDependency("com.fasterxml.jackson.module", "jackson-module-afterburner","2.10.5", null, null, true, null, null, null,false, null, null)
            ),
          pomXml(
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
              </project>
              """,
            """
              <project>
                  <groupId>com.mycompany.app</groupId>
                  <artifactId>my-app</artifactId>
                  <version>1</version>
                  <dependencies>
                      <dependency>
                          <groupId>com.fasterxml.jackson.module</groupId>
                          <artifactId>jackson-module-afterburner</artifactId>
                          <version>2.10.5</version>
                      </dependency>
                      <dependency>
                          <groupId>com.google.guava</groupId>
                          <artifactId>guava</artifactId>
                          <version>29.0-jre</version>
                      </dependency>
                  </dependencies>
              </project>
              """,
            spec -> spec.afterRecipe(p -> {
                var results = p.getMarkers().findFirst(MavenResolutionResult.class).orElseThrow();
                assertThat(results.getUserProperties().get("revision")).isEqualTo("1.0.0");
            })
          )
        );
    }
}
