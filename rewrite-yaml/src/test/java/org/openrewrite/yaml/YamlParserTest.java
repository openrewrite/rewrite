/*
 * Copyright 2023 the original author or authors.
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

import org.intellij.lang.annotations.Language;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.Issue;
import org.openrewrite.SourceFile;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.tree.ParseError;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.openrewrite.yaml.Assertions.yaml;

class YamlParserTest implements RewriteTest {

    @Test
    void ascii() {
        List<SourceFile> yamlSources = YamlParser.builder().build().parse("a: b\n").toList();
        assertThat(yamlSources).singleElement().isInstanceOf(Yaml.Documents.class);

        var documents = (Yaml.Documents) yamlSources.getFirst();
        Yaml.Document document = documents.getDocuments().getFirst();

        // Assert that end is parsed correctly
        assertThat(document.getEnd().getPrefix()).isEqualTo("\n");

        // Assert that the title is parsed correctly
        var mapping = (Yaml.Mapping) document.getBlock();
        Yaml.Mapping.Entry entry = mapping.getEntries().getFirst();
        var title = (Yaml.Scalar) entry.getValue();
        assertThat(title.getValue()).isEqualTo("b");
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/2062")
    @Test
    void fourBytesUnicode() {
        rewriteRun(
          yaml(
            """
              root:
                - value1: 🛠
                  value2: check
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/4176")
    @Test
    void listsAndListsOfLists() {
        rewriteRun(
          yaml(
            """
              root:
                normalListOfScalars:
                - a
                -  b
                normalListOfScalarsWithIndentation:
                  -  a
                  - b
                normalListOfMappings:
                  - a: b
                    c:  d
                  - e:  f
                normalListOfSquareBracketLists:
                  -   [ mno, pqr]
                  -  [stu , vwx]
                squareList: [x, y, z]
                listOfListsOfScalars:
                - - a
                  -  b
                listOfListsOfScalarsWithIndentation:
                  - - a
                    -  b
                listOfListsOfMappings:
                  - - a:  b
                      c: d
                    - e:  f
                listOfListsOfSquareBracketLists:
                  - - [mno, pqr ]
                    -  [stu , vwx]
              """
          )
        );
    }

    @ParameterizedTest
    @SuppressWarnings("OptionalGetWithoutIsPresent")
    @ValueSource(strings = {
      "b",
      " 🛠",
      " 🛠🛠",
      "🛠 🛠",
      "hello🛠world",
      "你好世界",
      "你好🛠世界"
    })
    void parseYamlWithUnicode(String input) {
        Stream<SourceFile> yamlSources = YamlParser.builder().build().parse("a: %s\n".formatted(input));
        SourceFile sourceFile = yamlSources.findFirst().get();
        assertThat(sourceFile).isNotInstanceOf(ParseError.class);

        var documents = (Yaml.Documents) sourceFile;
        Yaml.Document document = documents.getDocuments().getFirst();

        // Assert that end is parsed correctly
        var mapping = (Yaml.Mapping) document.getBlock();
        Yaml.Mapping.Entry entry = mapping.getEntries().getFirst();
        var title = (Yaml.Scalar) entry.getValue();
        assertThat(title.getValue()).isEqualTo(input.trim());
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "🛠 :  🛠",
      "你a好b🛠世c界d :  你a🛠好b🛠世c🛠界d"
    })
    void unicodeCharacterSpanningMultipleBytes(@Language("yml") String input) {
        rewriteRun(
          yaml(input)
        );
    }

    @Test
    void newlinesCombinedWithUnniCode() {
        rewriteRun(
          yaml(
            """
              {
                "data": {
                  "pro🛠metheus.y🛠ml": "global:\\n  scrape_🛠interval: 10s🛠\\n  sc🛠rape_timeout: 9s"
                }
              }
              """
          )
        );
    }

    @Test
    void unicodeEscapes() {
        rewriteRun(
          yaml(
            """
              root:
                "nul": "\\u0000"
                "reverse-solidus": "\\u005c"
              """
          )
        );
    }

    @Test
    void troublesomeYaml() {
        rewriteRun(
          yaml(
            """
              configDefinitions:
                appConfig:
                  description: "App config for consumer."
                  resolutionPaths:
                    - default: "/envProfile"
                  properties:
                    container:
                      description: "Container to use to the cosmos client."
                      type: "STRING"
                      kind: "SINGLE"
                      defaultValue: "UUIDItem"
                      rules:
                        possibleValues: []
                    database:
                      description: "Database to connect and use."
                      type: "STRING"
                      kind: "SINGLE"
                      defaultValue: "ForkliftPocDB"
                      rules:
                        possibleValues: []
                appConfig2:
                  description: "App config for consumer."
                  resolutionPaths:
                    - default: "/envProfile"
                  properties:
                    container:
                      description: "Container to use to the cosmos client."
                      type: "STRING"
                      kind: "SINGLE"
                      defaultValue: "CosmosSDKTest"
                      rules:
                        possibleValues: []
                    database:
                      description: "Database to connect and use."
                      type: "STRING"
                      kind: "SINGLE"
                      rules:
                        possibleValues: []
              """
          )
        );
    }

    @Test
    void atSymbols() {
        rewriteRun(
          yaml(
            // BTW, the @ sign is forbidden as the first character of a scalar value by the YAML spec:
            // https://github.com/yaml/yaml-spec/blob/1b1a1be43bd6e0cfec45caf0e40af3b5d2bb7f8a/spec/1.2.2/spec.md#L1877
            """
              root:
                specifier: npm:@testing-library/vue@5.0.4
                date: @build.timestamp@
                version: @project.version@
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1463")
    @Test
    void asteriskPlaceholders() {
        rewriteRun(
          yaml(
            """
              database:
                password: *** REMOVED ***
                apiKey: **REDACTED**
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1463")
    @Test
    void asteriskPlaceholdersWithAnchors() {
        // Ensure real anchors/aliases still work alongside asterisk placeholders
        rewriteRun(
          yaml(
            """
              defaults: &defaults
                timeout: 30
              production:
                <<: *defaults
                password: *** REMOVED ***
              """
          )
        );
    }

    @Test
    void pipeLiteralInASequenceWithDoubleQuotes() {
        rewriteRun(
          yaml(
               """
               - "one": |
                   two
                 "three": "four"
               """
          )
        );
    }

    @Test
    void spaceBeforeColon() {
        rewriteRun(
          yaml(
            """
            index_patterns : []
            """
          )
        );
    }

    @Test
    void tagsAsInCloudFormation() {
        rewriteRun(
          yaml(
            """
            AttributeDefinitions: !Dynamo
              - AttributeName: Title
            """
          )
        );
    }

    @Test
    void tagsAsInScalar() {
        rewriteRun(
          yaml(
            """
            AttributeDefinitions: !Dynamo Title
            """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5179")
    @Test
    void tagsInSequences() {
        rewriteRun(
          yaml(
            """
            Conditions:
              IsPollingFrequencyInMinutesSingular: !Equals [!Ref PollingFrequencyInMinutes, 1]
            """
          )
        );
    }

    @Test
    void globalTags() {
        rewriteRun(
          yaml(
            """
            age: !!int "42"
            pi: !!float "3.14159"
            is_valid: !!bool "true"
            names: !!seq
              - Alice
              - Bob
              - Charlie
            person: !!map
              name: John Doe
              age: 30
            """
          )
        );
    }

    @Test
    void parseTagInMapping() {
        // given
        @Language("yml") String code =
          """
          person: !!map
            name: Jonah Mathews
          """;

        // when
        var parsed = (Yaml.Documents) YamlParser.builder().build().parse(code).toList().getFirst();

        // test
        Yaml.Document document = parsed.getDocuments().getFirst();
        var topMapping = (Yaml.Mapping) document.getBlock();
        Yaml.Mapping.Entry person = topMapping.getEntries().getFirst();
        assertEquals("person", person.getKey().getValue());
        var withinPerson = (Yaml.Mapping) person.getValue();
        assertEquals("map", withinPerson.getTag().getName());
        assertEquals(Yaml.Tag.Kind.IMPLICIT_GLOBAL, withinPerson.getTag().getKind());
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5099")
    @Test
    void parseFlowSequenceAtBufferBoundary() {
        // May change over time in SnakeYaml, rendering this test fragile
        var snakeYamlEffectiveStreamReaderBufferSize = 1024 - 1;

        @Language("yml")
        var yaml = "a: " + "x".repeat(1000) + "\n" + "b".repeat(16) + ": []";
        assertEquals(snakeYamlEffectiveStreamReaderBufferSize - 1, yaml.lastIndexOf('['));

        rewriteRun(
          // Could be whatever recipe, it just proves the `IndexOutOfBoundsException` is not thrown,
          // thus proving the parser can handle a flow-style sequence ending at the boundary of the internal buffer used by SnakeYaml StreamReader.
          spec -> spec.recipe(new DeleteKey(".nonexistent","*")),
          yaml(yaml)
        );
    }

    @Test
    void withUnicodeCharacters() {
        rewriteRun(
          yaml(
            """
            - name: Elephant
            - #🦍COMMENT: unicode
            - action: Do something
            """
          )
        );
    }

    @Test
    void withUnicodeCharactersInSingleLine() {
        rewriteRun(
          yaml(
            """
            - name: Elephant
            - #🦍COMMENT: 🐶unicode
            - action: Do something
            """
          )
        );
    }

    @Test
    void withoutUnicodeCharacters() {
        rewriteRun(
          yaml(
            """
            - name: Elephant
            - #COMMENT: unicode
            - action: Do something
            """
          )
        );
    }

    @Test
    void withMultipleUnicodeCharacters() {
        rewriteRun(
          yaml(
            """
            - name: Rat
            - #🐀COMMENT: unicode
            - color: Black
            - #🦍COMMENT: unicode
            - action: Escape
            """
          )
        );
    }

    @Test
    void withMultipleUnicodeCharactersPerLine() {
        rewriteRun(
          yaml(
            """
            - name: Rat
            - #🐀COMMENT: 🦍unicode
            - color: Black
            - #🦍COMMENT: 🎱unicode
            - action: Escape
            """
          )
        );
    }

    @Test
    void withAnchorScalar() {
        rewriteRun(
          yaml(
            """
              anchored_content: &anchor_name This string will appear as the value.
              other_anchor: *anchor_name
              """
          )
        );
    }

    @Test
    void withAnchorMap() {
        rewriteRun(
          yaml(
            """
              anchored_content: &anchor_name
                anchor_key: 1
                another_anchor_key: 2
              other_anchor: *anchor_name
              """
          )
        );
    }

    @Test
    void withAnchorSequence() {
        rewriteRun(
          yaml(
            """
              anchored_content: &anchor
                - item1
                - item2
              other_anchor: *anchor
              """
          )
        );
    }

    @Issue("https://github.com/openrewrite/rewrite/issues/5553")
    @Test
    void withAnchorSequenceOnRootLevel() {
        rewriteRun(
          yaml(
            """
              anchored_content: &anchor
              - item1
              - item2
              other_anchor: *anchor
              """
          )
        );
    }

    @Test
    void parseTagsCorrectlyOnFirstLineOfMappingEntry() {
        rewriteRun(
          yaml(
            """
              - !SOMETAG
                a: b
              """,
            spec -> spec.afterRecipe(docs -> {
                var sequence = (Yaml.Sequence) docs.getDocuments().getFirst().getBlock();
                var mapping = (Yaml.Mapping) sequence.getEntries().getFirst().getBlock();
                assertThat(mapping.getTag().getName()).isEqualTo("SOMETAG");
            })
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/1471")
    @Test
    void yamlWithDocumentEndMarker() {
        rewriteRun(
          yaml(
            """
              ---
              applications:
                - name: modified-app-name
                  memory: 1G
              ...
              """,
            SourceSpec::noTrim
          )
        );
    }

    @Test
    void helmTemplateMatchingDocumentEndParsesCorrectly() {
        rewriteRun(
          yaml(
            """
              # ${{ looks.like.helm.before }}
              jobs:
                # ${{ looks.like.helm.middle }}
                steps:
                  # ${{ looks.like.helm.sequence }}
                  - items1: []
                  # ${{ looks.like.helm.in-sequence }}
                  - items2: []
                  # ${{ looks.like.helm.end-sequence }}
              # ${{ looks.like.helm.end }}
              """
          ),
          yaml(
            """
              jobs:
                steps:
                  - items1: []
                  # ${{ looks.like.helm.sequence }}
                  - items2: []
              """
          ),
          yaml(
            """
              jobs:
                steps:
                  # ${{ looks.like.helm.sequence }}
                  - items1: []
                  - items2: []
              """
          ),
          yaml(
            """
              jobs:
                steps:
                  - items1: []
                  - items2: []
              # ${{ looks.like.helm.end }}
              """
          )
        );
    }

    @Test
    void flowStyleMappingsInSequences() {
        rewriteRun(
          yaml(
            """
              tasks:
                - {"task_type": "Shell"}
                - { "task_type": "Shell2"}
              """
          ),
          yaml(
            """
              items:
                - name: block-style
                  type: mapping
                - {"name": "flow-style", "type": "mapping"}
                - key: another-block
              """
          ),
          yaml(
            """
              items:
                - {}
                - {"key": "value"}
              """
          ),
          yaml(
            """
              data:
                - {"list": [1, 2, 3], "map": {"nested": "value"}}
                - {"array": [{"inner": "map"}]}
              """
          ),
          yaml(
            """
              items:
                - {
                    "key": "value",
                    "another": "test"
                  }
              """
          ),
          yaml(
            """
              items:
                - {"key": "value",}
              """
          ),
          yaml(
            """
              defaults: &defaults {"type": "default", "enabled": true}
              items:
                - *defaults
                - {"type": "custom"}
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/850")
    @Test
    void anchorInSequencePreservesDash() {
        rewriteRun(
          yaml(
            """
              generalTasks:
              - &createTask
                name: createTask
                type: manipulator
                stereoType: creater
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/850")
    @Test
    void anchorInSequenceAtRootLevel() {
        // The issue shows:
        // generalTasks:
        // - &createTask   <- dash being removed
        // This tests anchor on a sequence entry at root level
        rewriteRun(
          yaml(
            """
              generalTasks:
              - &createTask
                name: createTask
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/850")
    @Test
    void emptyYamlFileWithNewline() {
        // File containing just a single newline should preserve it
        rewriteRun(
          yaml(
            "\n",
            SourceSpec::noTrim
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/850")
    @Test
    void emptyYamlFileWithMultipleNewlines() {
        // File containing multiple newlines should preserve them
        rewriteRun(
          yaml(
            "\n\n\n",
            SourceSpec::noTrim
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/850")
    @Test
    void flowStyleJsonLikeSequence() {
        // Issue: Colons being added out of the blue in JSON-like flow content
        // application/json:
        //   {
        //     "MV7",    <- becomes "MV7":,
        //     "7J04"    <- becomes "7J04":
        //   }
        rewriteRun(
          yaml(
            """
              example:
                application/json:
                  {
                    "MV7",
                    "7J04"
                  }
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/850")
    @Test
    void singleBraceTemplateSyntaxQuoted() {
        // Single-brace placeholders like {C App} look like flow mapping start
        // When quoted, it's valid YAML
        rewriteRun(
          yaml(
            """
              swagger: '2.0'
              host: "{C App}.colruyt.int/{C App}"
              """
          )
        );
    }

    @Issue("https://github.com/moderneinc/customer-requests/issues/850")
    @Test
    void singleBraceTemplateSyntaxUnquoted() {
        // Unquoted {C App} looks like flow mapping start but should be handled
        // as a placeholder template, similar to Helm templates
        rewriteRun(
          yaml(
            """
              swagger: '2.0'
              host: {C App}.colruyt.int/{C App}
              """
          )
        );
    }

    @Test
    void anchorOnMappingInSequenceEntry() {
        rewriteRun(
          yaml(
            """
              - k: v
              - &b
                k2: v2
              """
          )
        );
    }

    @Test
    void literalScalarTrailingNewlineInValue() {
        // Literal (|) and folded (>) scalars should keep trailing newlines in their value
        // The next entry's prefix should be just indentation, not include the newline
        rewriteRun(
          yaml(
            """
              parent:
                message: |
                  line1
                  line2
                next: value
              """,
            spec -> spec.afterRecipe(docs -> new YamlIsoVisitor<Integer>() {
                @Override
                public Yaml.Scalar visitScalar(Yaml.Scalar scalar, Integer ctx) {
                    if (scalar.getStyle() == Yaml.Scalar.Style.LITERAL) {
                        // Literal scalar value should end with newline
                        assertThat(scalar.getValue()).endsWith("\n");
                    }
                    return super.visitScalar(scalar, ctx);
                }

                @Override
                public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, Integer ctx) {
                    if ("next".equals(entry.getKey().getValue())) {
                        // Entry following literal scalar has prefix with just indentation (no leading newline)
                        assertThat(entry.getPrefix()).isEqualTo("  ");
                    }
                    return super.visitMappingEntry(entry, ctx);
                }
            }.visit(docs, 0))
          )
        );
    }

    @Test
    void foldedScalarTrailingNewlineInValue() {
        // Folded (>) scalars should also keep trailing newlines in their value
        rewriteRun(
          yaml(
            """
              parent:
                description: >
                  This is a folded
                  multiline string.
                status: active
              """,
            spec -> spec.afterRecipe(docs -> new YamlIsoVisitor<Integer>() {
                @Override
                public Yaml.Scalar visitScalar(Yaml.Scalar scalar, Integer ctx) {
                    if (scalar.getStyle() == Yaml.Scalar.Style.FOLDED) {
                        // Folded scalar value should end with newline
                        assertThat(scalar.getValue()).endsWith("\n");
                    }
                    return super.visitScalar(scalar, ctx);
                }

                @Override
                public Yaml.Mapping.Entry visitMappingEntry(Yaml.Mapping.Entry entry, Integer ctx) {
                    if ("status".equals(entry.getKey().getValue())) {
                        // Entry following folded scalar has prefix with just indentation (no leading newline)
                        assertThat(entry.getPrefix()).isEqualTo("  ");
                    }
                    return super.visitMappingEntry(entry, ctx);
                }
            }.visit(docs, 0))
          )
        );
    }
}
