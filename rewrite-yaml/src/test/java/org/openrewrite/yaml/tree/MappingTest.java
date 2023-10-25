/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.yaml.tree;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.yaml.tree.Yaml.Scalar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.yaml.Assertions.yaml;

@SuppressWarnings("YAMLUnusedAnchor")
class MappingTest implements RewriteTest {

    @Issue("https://github.com/openrewrite/rewrite/issues/423")
    @Test
    void emptyObject() {
        rewriteRun(
          yaml("workflow_dispatch: {}"));
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/423")
    @Test
    void flowStyleMapping() {
        rewriteRun(
          yaml(
            """
              {
                "data": {
                  "prometheus.yml": "global:\\n  scrape_interval: 10s\\n  scrape_timeout: 9s"
                }
              }
              """
          )
        );
    }

    @Test
    void multipleEntries() {
        rewriteRun(
          yaml(
            """
              type : specs.openrewrite.org/v1beta/visitor # comment with colon :
              name : org.openrewrite.text.ChangeTextToJon
              """,
            spec -> spec.afterRecipe(y -> {
                assertThat(((Yaml.Mapping) (y.getDocuments().get(0).getBlock())).getEntries().stream()
                  .map(e -> (Scalar) e.getKey())
                  .map(Scalar::getValue)).containsExactly("type", "name");
            })
          )
        );
    }

    @Test
    void deep() {
        rewriteRun(
          yaml(
            """
              type:
                      name: org.openrewrite.text.ChangeTextToJon
              """,
            spec -> spec.afterRecipe(y -> {
                Yaml.Mapping mapping = (Yaml.Mapping) y.getDocuments().get(0).getBlock();
                assertThat(mapping.getEntries().stream()
                  .map(e -> (Scalar) e.getKey())
                  .map(Scalar::getValue)).containsExactly("type");
                assertThat(mapping.getEntries().get(0).getValue()).isInstanceOf(Yaml.Mapping.class);
            })
          )
        );
    }

    @Test
    void largeScalar() {
        rewriteRun(
          yaml(
            """
              spring:
                cloud:
                  config:
                    server:
                      composite:
                        - type: git
                          uri: git@gitserver.com:team/repo1.git
                          ignoreLocalSshSettings: true
                          greater-than-block-text-password: >
                            {cipher}d1b2458ccede07c856ff952bd841638ff4dd12ed1d36812663c3c7262d57bf46
                          privateKey: "-----BEGIN RSA PRIVATE KEY-----\\nMIIEpAIBAAKCAQEAoqyz6YaYMTr7L8GLPSQpAQXaM04gRx4CCsGK2kfLQdw4BlqI\\n2U7EeNwxq1I1L3Ag6E7wH4BHLHd4TKaZR6agFkn8oomz71yZPGjuZQ==\\n-----END RSA PRIVATE KEY-----"
                          repos:
                            repo1:
                              uri: git@gitserver.com:team/repo2.git
                              hostKey: someHostKey
                              hostKeyAlgorithm: ssh-rsa
                              privateKey: |
                                -----BEGIN RSA PRIVATE KEY-----
                                MIIEpgIBAAKCAQEAx4UbaDzY5xjW6hc9jwN0mX33XpTDVW9WqHp5AKaRbtAC3DqX
                                69pcVH/4rmLbXdcmNYGm6iu+MlPQk4BUZknHSmVHIFdJ0EPupVaQ8RHT
                                -----END RSA PRIVATE KEY-----
              """
          )
        );
    }

    @Test
    void mappingContainingSequence() {
        rewriteRun(
          yaml(
            """
              foo:
                - bar: qwer
                  asdf: hjkl
              """
          )
        );
    }

    @Test
    void commentWithColon() {
        rewriteRun(
          yaml(
            """
              for: bar
              # Comment with a colon:
              baz: foo
              """,
            spec -> spec.afterRecipe(documents -> {
                var doc = documents.getDocuments().get(0);
                Yaml.Mapping mapping = (Yaml.Mapping) doc.getBlock();
                assertThat(mapping.getEntries().size()).isEqualTo(2);

                var bazFooEntry = mapping.getEntries().get(1);
                assertThat(bazFooEntry.getPrefix()).isEqualTo("\n# Comment with a colon:\n");
            })
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/1469")
    @Test
    void emptyDocument() {
        rewriteRun(
          yaml(
            "",
            spec -> spec.afterRecipe(documents -> assertThat(documents.getDocuments()).isNotEmpty())
          )
        );
    }

    @Test
    void multiDocOnlyComments() {
        rewriteRun(
          yaml(
            """
              # doc-1-pre
              ---
              # doc-1-end
              ...
              ---
              # doc-2-pre
              """,
            spec -> spec.afterRecipe(docs -> {
                assertThat(docs.getDocuments().size()).isEqualTo(2);
                var doc = docs.getDocuments().get(0);
                assertThat(doc.getPrefix()).isEqualTo("# doc-1-pre\n");
                assertThat(doc.getEnd().getPrefix()).isEqualTo("\n# doc-1-end\n");
                var doc2 = docs.getDocuments().get(1);
                assertThat(doc2.getEnd().getPrefix()).isEqualTo("\n# doc-2-pre");
            })
          )
        );
    }

    @Test
    void singleDocOnlyComments() {
        rewriteRun(
          yaml(
            """
              # doc-1-pre
              """,
            spec -> spec.afterRecipe(docs -> {
                assertThat(docs.getDocuments().size()).isEqualTo(1);
                var doc = docs.getDocuments().get(0);
                assertThat(doc.getPrefix()).isEqualTo("# doc-1-pre");
            })
          )
        );
    }

    @Issue("https://github.com/spring-projects/spring-boot/issues/8438")
    @Test
    void valueStartsWithAt() {
        rewriteRun(
          yaml(
            """
              date: @build.timestamp@
              version: @project.version@
              """
          )
        );
    }


    @Test
    void suffixBeforeColon() {
        rewriteRun(
          yaml(
            """
                    data :
                      test : 0
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/3632")
    @Test
    void multilineScalar() {
        rewriteRun(
          yaml(
            """
              data: &anc |
                  @this is a long string@
              bar: *anc
              """
          )
        );
    }

    @Test
    void literals() {
        rewriteRun(
          yaml(
            """
              data:
                prometheus.yml: |-
                  global:
                    scrape_interval: 10s
                    scrape_timeout: 9s
                    evaluation_interval: 10s
              """
          )
        );
    }

    @Test
    void scalarValue() {
        rewriteRun(
          yaml(
            """
              default: &default test
              stage: *default
              """
          )
        );
    }

    @Test
    void scalarValueInBrackets() {
        rewriteRun(
          yaml(
            """
              defaults: [&first A, &stage test, &last Z]
              config: [first: *first, stage: *stage, last: *last]
              """
          )
        );
    }

    @Test
    void scalarKeyAnchor() {
        rewriteRun(
          yaml(
            """
              foo:
                - start: start
                - &anchor buz: buz
                - *anchor: baz
                - end: end
              """
          )
        );
    }

    @Test
    void scalarEntryValue() {
        rewriteRun(
          yaml(
            """
              foo:
               - start: start
               - buz: &anchor ooo
               - fuz: *anchor
               - end: end
              """
          )
        );
    }

    @Test
    void aliasEntryKey() {
        rewriteRun(
          yaml(
            """
              bar:
                &abc yo: friend
              baz:
                *abc: friendly
              """
          )
        );
    }

    @Test
    void scalarKeyAnchorInBrackets() {
        rewriteRun(
          yaml("foo: [start: start, &anchor buz: buz, *anchor: baz, end: end]"));
    }

    @Test
    void scalarEntryValueAnchorInBrackets() {
        rewriteRun(
          yaml("foo: [start: start, &anchor buz: buz, baz: *anchor, end: end]"));
    }

    @Test
    void sequenceAnchor() {
        rewriteRun(
          yaml(
            """
              defaults: &defaults
                - A: 1
                - B: 2
              key: *defaults
              """
          )
        );
    }

    @Test
    void sequenceAnchorFlowStyle() {
        rewriteRun(
          yaml(
            """
              defaults: &defaults [A:1, B:2] # comment
              key: *defaults
              """
          )
        );
    }

    @Test
    void sequenceAnchorWithComments() {
        rewriteRun(
          yaml(
            """
              defaults: &defaults # sequence start comment
                - A: 1 # A comment
                - B: 2 # B comment
              key: *defaults
              """
          )
        );
    }

    @Test
    void sequenceAnchorInBrackets() {
        rewriteRun(
          yaml(
            """
              defaults: &defaults [A: 1, B: 2]
              mapping: *defaults
              """
          )
        );
    }

    @Test
    void mappingAnchor() {
        rewriteRun(
          yaml(
            """
              defaults: &defaults
                A: 1
                B: 2
              mapping:
                << : *defaults
                A: 23
                C: 99
              """
          )
        );
    }

    // https://github.com/yaml/yaml-grammar/blob/master/yaml-spec-1.2.txt#L1914
    @ParameterizedTest
    @ValueSource(strings = {
      " '\\n' ",
      " '\n' ",
      " \n ",
      " \"\\0\" ",
      " \"\\0\" ",
      " \"\\a\" ",
      " \"\\a\" ",
      " \"\\b\" ",
      " \"\\t\" ",
      " \"\t\" ",
      " \"\\n\" ",
      " \"\n\" ",
      " \"\\v\" ",
      " \"\\f\" ",
      " \"\\r\" ",
      " \"\r\" ",
      " \"\\e\" ",
      " \"\\\\\" ",
      " \"\\\"\" ",
      " \"\\N\" ",
      " \"\\_\" ",
      " \"\\L\" ",
      " \"\\P\" ",
    })
    void escapeSequences(String str) {
        rewriteRun(
          yaml("escaped-value: " + str)
        );
    }
}
