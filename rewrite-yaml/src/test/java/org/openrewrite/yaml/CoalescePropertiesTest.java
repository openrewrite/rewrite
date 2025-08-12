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
package org.openrewrite.yaml;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openrewrite.DocumentExample;
import org.openrewrite.Issue;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import java.util.List;

import static org.openrewrite.yaml.Assertions.yaml;

class CoalescePropertiesTest implements RewriteTest {
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new CoalesceProperties());
    }

    @DocumentExample
    @Test
    void fold() {
        rewriteRun(
          yaml(
            """
              management:
                  metrics:
                      enable.process.files: true
                  endpoint:
                      health:
                          show-components: always
                          show-details: always
              """,
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

    @Issue("https://github.com/openrewrite/rewrite/issues/1103")
    @Test
    void foldSequenceOfObjects() {
        rewriteRun(
          yaml(
            """
              foo:
                bar:
                  sequence:
                    - name: name
                      propertyA: fieldA
                      propertyB: fieldB
                    - name: name
                      propertyA: fieldA
                      propertyB: fieldB
                  scalar: value
              """,
            """
              foo.bar:
                sequence:
                  - name: name
                    propertyA: fieldA
                    propertyB: fieldB
                  - name: name
                    propertyA: fieldA
                    propertyB: fieldB
                scalar: value
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1133")
    @Test
    void foldSequenceOfObjectsFormattedWithDashDirectlyUnderMappingKey() {
        rewriteRun(
          yaml(
            """
              matrix:
                include:
                # comment-a
                # comment-b
                - name: entry-0-name # comment-c
                    # comment-d
                  value: entry-0-value
                  # comment-e
                - name: entry-1-name
                  value: entry-1-value
              """,
            """
              matrix.include:
              # comment-a
              # comment-b
              - name: entry-0-name # comment-c
                  # comment-d
                value: entry-0-value
                # comment-e
              - name: entry-1-name
                value: entry-1-value
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1103")
    @Test
    void foldSequence() {
        rewriteRun(
          yaml(
            """
                foo:
                  bar:
                    buz:
                      - item1
                      - item2
                      - item3
                    baz: value
              """,
            """
                foo.bar:
                  buz:
                    - item1
                    - item2
                    - item3
                  baz: value
              """
          )
        );
    }

    @Disabled
    @Test
    void group() {
        rewriteRun(
          yaml(
            """
              management.metrics.enable.process.files: true
              management.metrics.enable.jvm: true
              """,
            """
              management.metrics.enable:
                  process.files: true
                  jvm: true
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1125")
    @Test
    void foldWithCommentsInPrefix() {
        rewriteRun(
          yaml(
            """
              a:
                b:
                  # d-comment
                  d:
                    e.f: true
                  c: c-value
              """,
            """
              a.b:
                # d-comment
                d.e.f: true
                c: c-value
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1125")
    @Test
    void foldWithCommentsInPrefixWhenCommentsHaveDifferentIndentThanTheirElement() {
        rewriteRun(
          yaml(
            """
              a:
                b:
                # d-comment
                  d:
                    e.f: true
                 # c-comment
                  c:
                    d: d-value
              """,
            """
              a.b:
              # d-comment
                d.e.f: true
               # c-comment
                c.d: d-value
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1125")
    @Test
    void doNotFoldKeysWithCommentsInPrefix() {
        rewriteRun(
          yaml(
            """
              a:
                b:
                  d: # d-comment
                    e:
                      f: f-value # f-comment
                  c:
                    # g-comment
                    g:
                      h: h-value
              """,
            """
              a.b:
                d: # d-comment
                  e.f: f-value # f-comment
                c:
                  # g-comment
                  g.h: h-value
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1142")
    @Test
    void doNotShiftYamlCommentsInPrefixFollowingPreviousYamlObject() {
        rewriteRun(
          yaml(
            """
              a:
                b:
                  c: c-value  # c-comment
                  d: d-value # d-comment
                  e: e-value   # e-comment
                  f: f-value
                  g: g-value
              """,
            """
              a.b:
                c: c-value  # c-comment
                d: d-value # d-comment
                e: e-value   # e-comment
                f: f-value
                g: g-value
              """
          )
        );
    }

    @SuppressWarnings("YAMLUnusedAnchor")
    @Test
    void doNotCoalesceDocumentsHavingAnchorsAndAliases() {
        rewriteRun(
          yaml(
            """
              management:
                  metrics:
                      &id enable.process.files: true
                  endpoint:
                      health:
                          show-components: always
                          show-details: always
                          *id: false
              """
          )
        );
    }

    @Test
    void exclusion() {
        rewriteRun(
          spec -> spec.recipe(new CoalesceProperties(List.of("$..logging", "$..some"), null)),
          yaml(
            """
              a:
                first:
                  logging:
                    level:
                      com.company.extern.service: DEBUG
                      com.another.package: INFO
              some:
                things:
                  else: value
              """,
            """
              a.first:
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
    void applyTo() {
        rewriteRun(
          spec -> spec.recipe(new CoalesceProperties(null, List.of("$..logging", "$..some"))),
          yaml(
            """
              a:
                first:
                  logging:
                    level:
                      com.company.extern.service: DEBUG
                      com.another.package: INFO
              some:
                things:
                  else: value
              """,
            """
              a:
                first:
                  logging.level:
                    com.company.extern.service: DEBUG
                    com.another.package: INFO
              some.things.else: value
              """
          )
        );
    }

    @Test
    void applyToWithExclusion() {
        rewriteRun(
          spec -> spec.recipe(new CoalesceProperties(List.of("$..endpoint"), List.of("$.management2"))),
          yaml(
            """
              management:
                  metrics:
                      enable.process.files: true
                  endpoint:
                      health.show-components: always
              management2:
                  metrics:
                      enable.process.files: true
                  endpoint:
                      health.show-components: always
              """,
            """
              management:
                  metrics:
                      enable.process.files: true
                  endpoint:
                      health.show-components: always
              management2:
                  metrics.enable.process.files: true
                  endpoint:
                      health.show-components: always
              """
          )
        );
    }
}
