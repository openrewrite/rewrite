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
package org.openrewrite.maven.search;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.maven.table.ManagedDependencyGraph;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.maven.Assertions.pomXml;

class EffectiveManagedDependenciesTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new EffectiveManagedDependencies());
    }

    @DocumentExample
    @Test
    void emitParentAndInheritedManagedDependencies() {
        rewriteRun(
          spec -> spec.dataTable(ManagedDependencyGraph.Row.class, rows ->
            assertThat(rows).containsExactlyInAnyOrder(
              new ManagedDependencyGraph.Row("com.example:child:1.0", "com.example:parent:1.0"),
              new ManagedDependencyGraph.Row("com.example:child:1.0", "com.google.guava:guava:33.0.0-jre"))),
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>parent</artifactId>
                  <version>1.0</version>
                  <packaging>pom</packaging>
                  <modules>
                      <module>child</module>
                  </modules>
                  <dependencyManagement>
                      <dependencies>
                          <dependency>
                              <groupId>com.google.guava</groupId>
                              <artifactId>guava</artifactId>
                              <version>33.0.0-jre</version>
                          </dependency>
                      </dependencies>
                  </dependencyManagement>
              </project>
              """
          ),
          pomXml(
            """
              <project>
                  <parent>
                      <groupId>com.example</groupId>
                      <artifactId>parent</artifactId>
                      <version>1.0</version>
                  </parent>
                  <artifactId>child</artifactId>
              </project>
              """,
            spec -> spec.path("child/pom.xml")
          )
        );
    }
}
