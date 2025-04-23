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
package org.openrewrite.yaml;

import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.yaml.Assertions.yaml;

class UnfoldPropertiesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new UnfoldProperties(null));
    }

    @DocumentExample
    @Test
    void unfold() {
        rewriteRun(
          yaml(
            """
              management:
                metrics.enable.process.files: true
                endpoint.health:
                  show-components: always
                  show-details: always
              """,
            """
              management:
                metrics:
                  enable:
                    process:
                      files: true
                endpoint:
                  health:
                    show-components: always
                    show-details: always
              """
          )
        );
    }

    @Test
    void multipleLayersWithComments() {
        rewriteRun(
          yaml(
            """
              a:
                b.c:
                  d.e: true
              1:
                2:
                  3:
                    # Comment
                    4.5.6:
                      # Comment 2
                      7.8.10:
                        # Comment 3
                        11.12: false
              add.more: value
              """,
            """
              a:
                b:
                  c:
                    d:
                      e: true
              1:
                2:
                  3:
                    # Comment
                    4:
                      5:
                        6:
                          # Comment 2
                          7:
                            8:
                              10:
                                # Comment 3
                                11:
                                  12: false
              add:
                more: value
              """
          )
        );
    }

    @Test
    void unfoldFromRoot() {
        rewriteRun(
          yaml(
            """
              management.test:
                a.b:
                  value: c
              """,
            """
              management:
                test:
                  a:
                    b:
                      value: c
              """
          )
        );
    }

    @Test
    void exclusions() {
        rewriteRun(
          spec -> spec.recipe(new UnfoldProperties(List.of("metrics.enable.process.files", "endpoint.health"))),
          yaml(
            """
              management:
                metrics.enable.process.files: true
                endpoint.health:
                  show-components: always
                  show-details: always
              """
          )
        );
    }

    @Test
    void exclusionWithSubSetOfKey() {
        rewriteRun(
          spec -> spec.recipe(new UnfoldProperties(List.of("com.service"))),
          yaml(
            """
              logging.level.com.service.A: DEBUG
              """,
            """
              logging:
                level:
                  com.service:
                    A: DEBUG
              """
          )
        );
    }

    @Test
    void exclusionWithRegex() {
        rewriteRun(
          spec -> spec.recipe(new UnfoldProperties(List.of("com.*"))),
          yaml(
            """
              A.B:
                com.some.service: DEBUG
                com.another.package: INFO
              """,
            """
             A:
               B:
                 com.some.service: DEBUG
                 com.another.package: INFO
             """
          )
        );
    }

    @Test
    void exclusionWithParentAndRegex() {
        rewriteRun(
          spec -> spec.recipe(new UnfoldProperties(List.of("[logging.level]->.*"))),
          yaml(
            """
              logging.level:
                com.desjardins.financementengagementcreditrotatif.externe.sic.service: DEBUG
                com.another.package: INFO
              """,
            """
              logging:
                level:
                  com.desjardins.financementengagementcreditrotatif.externe.sic.service: DEBUG
                  com.another.package: INFO
              """
          )
        );
    }

    @Test
    void sameBlocks() {
        rewriteRun(
          yaml(
            """
              logging.level:
                com.first.package: INFO
              
              logging.level.com.another.package: DEBUG
              """,
            """
              logging:
                level:
                  com:
                    first:
                      package: INFO
                    another:
                      package: DEBUG
              """
          )
        );
    }
}
