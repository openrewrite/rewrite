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
package org.openrewrite.xml.style;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.xml.table.XmlStyleReport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.xml.Assertions.xml;

class AutodetectDebugTest implements RewriteTest {

    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new AutodetectDebug());
    }

    @DocumentExample
    @Test
    void reportDetectedStyles() {
        rewriteRun(
          spec -> spec.dataTable(XmlStyleReport.Row.class, rows -> {
              assertThat(rows).hasSize(2);
              assertThat(rows).filteredOn(row -> "pom.xml".equals(row.getName())).singleElement().satisfies(row -> {
                  assertThat(row.isUseTabCharacter()).isFalse();
                  assertThat(row.getIndentSize()).isEqualTo(2);
                  assertThat(row.getIndentCount()).isEqualTo(4);
                  assertThat(row.getIndentsMatchingOwnStyle()).isEqualTo(4);
                  assertThat(row.getIndentsMatchingProjectStyle()).isEqualTo(4);
                  assertThat(row.getContinuationIndentCount()).isEqualTo(1);
              });
              assertThat(rows).filteredOn(row -> "Overall Project Style".equals(row.getName())).singleElement()
                .satisfies(row -> assertThat(row.getIndentSize()).isEqualTo(2));
          }),
          xml(
            """
              <project>
                <dependencies>
                  <dependency
                      scope="test">
                    <groupId>org.junit</groupId>
                  </dependency>
                </dependencies>
                <name>app</name>
              </project>
              """,
            spec -> spec.path("pom.xml")
          )
        );
    }
}
