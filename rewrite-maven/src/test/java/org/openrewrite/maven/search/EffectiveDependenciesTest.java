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
import org.openrewrite.maven.table.DependencyGraph;
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.openrewrite.maven.Assertions.pomXml;

class EffectiveDependenciesTest implements RewriteTest {

    @DocumentExample
    @Test
    void emitDirectAndTransitiveDependencies() {
        rewriteRun(
          spec -> spec.recipe(new EffectiveDependencies("compile"))
            .dataTable(DependencyGraph.Row.class, rows ->
              assertThat(rows)
                .extracting(DependencyGraph.Row::getSourceSet, DependencyGraph.Row::getFrom, DependencyGraph.Row::getTo)
                .containsExactly(
                  tuple("main", "com.example:app:1.0", "junit:junit:4.13.2"),
                  tuple("main", "junit:junit:4.13.2", "org.hamcrest:hamcrest-core:1.3"))),
          pomXml(
            """
              <project>
                  <groupId>com.example</groupId>
                  <artifactId>app</artifactId>
                  <version>1.0</version>
                  <dependencies>
                      <dependency>
                          <groupId>junit</groupId>
                          <artifactId>junit</artifactId>
                          <version>4.13.2</version>
                      </dependency>
                      <dependency>
                          <groupId>org.slf4j</groupId>
                          <artifactId>slf4j-api</artifactId>
                          <version>2.0.16</version>
                          <scope>test</scope>
                      </dependency>
                  </dependencies>
              </project>
              """
          )
        );
    }
}
