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
import org.junitpioneer.jupiter.ExpectedToFail;
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
          spec -> spec.recipe(new UnfoldProperties(List.of("$..[logging.level][?(@property.match(/.*/))]", "$..[enable.process.files]"))),
          yaml(
            """
              spring.application.name: my-app
              logging.level:
                root: INFO
                org.springframework.web: DEBUG
              management:
                metrics.enable.process.files: true
                endpoint.health:
                  show-components: always
                  show-details: always
              """,
            """
              spring:
                application:
                  name: my-app
              logging:
                level:
                  root: INFO
                  org.springframework.web: DEBUG
              management:
                metrics:
                  enable.process.files: true
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
                        11.12: false # Comment 4
              # Comment 5
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
                                  12: false # Comment 4
              # Comment 5
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
          spec -> spec.recipe(new UnfoldProperties(List.of(
            "$..['metrics.enable.process.files']",
            "$..[\"endpoint.health\"]",
            "$..[show.details]",
            "$.management.endpoint.health[show.controllers]",
            "$.management.endpoint.health.show.views"
          ))),
          yaml(
            """
              management:
                metrics.enable.process.files: true
                endpoint.health:
                  show-components: always
                  show.details: always
                  show.controllers: always
                  show.views: always
              """
          )
        );
    }

    @Test
    void exclusionWithSubSetOfKey() {
        rewriteRun(
          spec -> spec.recipe(new UnfoldProperties(List.of("$..['com.service']"))),
          yaml(
            """
              logging.level.com.service.A: DEBUG
              logging.level.com.service.B: DEBUG
              """,
            """
              logging:
                level:
                  com.service:
                    A: DEBUG
                    B: DEBUG
              """
          )
        );
    }

    @Test
    void exclusionWithRegex() {
        rewriteRun(
          spec -> spec.recipe(new UnfoldProperties(List.of("$..[?(@property.match(/some.*/))]"))),
          yaml(
            """
              A.B:
                com.some.service: DEBUG
              """,
            """
             A:
               B:
                 com:
                   some.service: DEBUG
             """
          )
        );
    }

    @Test
    void exclusionWithParentAndRegex() {
        rewriteRun(
          spec -> spec.recipe(new UnfoldProperties(List.of("$..[logging.level][?(@property.match(/^com.*/))]"))),
          yaml(
            """
              first:
                logging.level:
                  com.company.extern.service: DEBUG
                  com.another.package: INFO
              some.things.else: value
              """,
            """
              first:
                logging:
                  level:
                    com.company.extern.service: DEBUG
                    com.another.package: INFO
              some:
                things:
                  else: value
              """
          )
        );
    }

    @Test
    void mergeDuplicatedSections() {
        rewriteRun(
          yaml(
            """
              logging.level:
                com.first.package: INFO
              some.thing: else
              logging.level.com.second.package: DEBUG
              other: thing
              logging.level.com.third.package: DEBUG
              logging.level.com:
                fourth.package: DEBUG
              """,
            """
              logging:
                level:
                  com:
                    first:
                      package: INFO
                    second:
                      package: DEBUG
                    third:
                      package: DEBUG
                    fourth:
                      package: DEBUG
              some:
                thing: else
              other: thing
              """
          )
        );
    }

    @Test
    // TODO implement handling of comments, remove this test and enable the `mergeDuplicatedSectionsWitComments` test
    void mergeDuplicatedSectionsCommentsAreIgnored() {
        rewriteRun(
          yaml(
            """
              logging.level:
                 # unaltered comment
                com.first.package: INFO
              logging.level.com.second.package: DEBUG # comment 1
              logging.level.com.third.package: DEBUG # comment 2
              """,
            """
              logging:
                level:
                  # unaltered comment
                  com:
                    first:
                      package: INFO
              logging:
                level:
                  com:
                    second:
                      package: DEBUG # comment 1
              logging:
                level:
                  com:
                    third:
                      package: DEBUG # comment 2
              """
          )
        );
    }

    @Test
    @ExpectedToFail("Comments are not supported yet")
    void mergeDuplicatedSectionsWitComments() {
        rewriteRun(
          yaml(
            """
              # comment 1
              logging.level:
                # comment 4
                com.first.package: INFO # comment 5
              some.thing: else
              # comment 2
              logging.level.com.second.package: DEBUG  # comment 6
              other: thing
              # comment 3
              logging.level.com.third.package: DEBUG  # comment 7
              logging.level.com:
                fourth.package: DEBUG  # comment 8
              """,
            """
              # comment 1
              # comment 2
              # comment 3
              logging:
                level:
                  # comment 4
                  com:
                    first:
                      package: INFO # comment 5
                    second:
                      package: DEBUG # comment 6
                    third:
                      package: DEBUG # comment 7
              some:
                thing: else
              other: thing
              """
          )
        );
    }
}
