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
package org.openrewrite.yaml.tree;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.test.SourceSpec;
import org.openrewrite.yaml.YamlIsoVisitor;
import org.openrewrite.yaml.marker.TagBeforeAnchor;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.yaml.Assertions.yaml;

class NodePropertiesTest implements RewriteTest {

    @Test
    void tagBeforeAnchorOnMappingValue() {
        rewriteRun(
          yaml(
            """
              a: !!str &f six
              """,
            tagBeforeAnchor("f", "str")
          )
        );
    }

    @Test
    void anchorBeforeTagOnMappingValue() {
        rewriteRun(
          yaml(
            """
              a: &f !!str six
              """,
            anchorBeforeTag("f", "str")
          )
        );
    }

    @Test
    void tagBeforeAnchorOnMappingKey() {
        rewriteRun(
          yaml(
            """
              !!str &l key: v
              """,
            tagBeforeAnchor("l", "str")
          )
        );
    }

    @Test
    void tagBeforeAnchorOnDocumentRoot() {
        rewriteRun(
          yaml(
            """
              --- !!str &d x
              """,
            tagBeforeAnchor("d", "str")
          )
        );
    }

    @Test
    void tagBeforeAnchorOnSequenceEntry() {
        rewriteRun(
          yaml(
            """
              - !!str &s x
              """,
            tagBeforeAnchor("s", "str")
          )
        );
    }

    @Test
    void tagBeforeAnchorOnBlockScalar() {
        rewriteRun(
          yaml(
            """
              a: !!str &b |
                text
              """,
            tagBeforeAnchor("b", "str")
          )
        );
    }

    @Test
    void tagBeforeAnchorInFlowSequence() {
        rewriteRun(
          yaml(
            """
              f: [!!str &i x, y]
              """,
            tagBeforeAnchor("i", "str")
          )
        );
    }

    @Test
    void tagBeforeAnchorInFlowMapping() {
        rewriteRun(
          yaml(
            """
              f: { a: !!str &x b }
              """,
            tagBeforeAnchor("x", "str")
          )
        );
    }

    @Test
    void tagBeforeAnchorOnSeparateLines() {
        rewriteRun(
          yaml(
            """
              a: !!str
                &x six
              """,
            spec -> spec.afterRecipe(documents -> {
                Yaml.Scalar scalar = (Yaml.Scalar) anchored(documents, "x");
                assertThat(scalar.getValue()).isEqualTo("six");
                assertThat(scalar.getAnchor().getPrefix()).isEqualTo("\n  ");
                assertThat(scalar.getMarkers().findFirst(TagBeforeAnchor.class)).isPresent();
            })
          )
        );
    }

    @Test
    void aliasOfEmptyScalarWithTagBeforeAnchor() {
        rewriteRun(
          yaml(
            """
              a: !!str &x
              b: *x
              """,
            tagBeforeAnchor("x", "str")
          )
        );
    }

    @Test
    void tagBeforeAnchorOnBlockMapping() {
        rewriteRun(
          yaml(
            """
              a: !!map &m
                b: c
              """,
            tagBeforeAnchor("m", "map")
          )
        );
    }

    @Test
    void tagBeforeAnchorOnRootBlockMapping() {
        rewriteRun(
          yaml(
            """
              --- !!map &r
              a: b
              """,
            tagBeforeAnchor("r", "map")
          )
        );
    }

    @Test
    void tagBeforeAnchorOnBlockMappingInSequence() {
        rewriteRun(
          yaml(
            """
              - !!map &m
                a: b
              """,
            tagBeforeAnchor("m", "map")
          )
        );
    }

    @Test
    void tagBeforeAnchorOnBlockSequence() {
        rewriteRun(
          yaml(
            """
              a: !!seq &q
                - x
              """,
            tagBeforeAnchor("q", "seq")
          )
        );
    }

    @Test
    void tagBeforeAnchorOnBlockSequenceInSequence() {
        rewriteRun(
          yaml(
            """
              - !!seq &q
                - x
              """,
            tagBeforeAnchor("q", "seq")
          )
        );
    }

    @Test
    void tagOfFlowSequence() {
        rewriteRun(
          yaml(
            """
              f: !!seq [x, y]
              """,
            spec -> spec.afterRecipe(documents -> {
                Yaml.Sequence sequence = (Yaml.Sequence) firstValue(documents);
                assertThat(sequence.getTag()).isNotNull();
                assertThat(sequence.getTag().getName()).isEqualTo("seq");
                assertThat(sequence.getTag().getPrefix()).isEqualTo(" ");
                assertThat(sequence.getTag().getSuffix()).isEqualTo(" ");
                assertThat(sequence.getOpeningBracketPrefix()).isEmpty();
            })
          )
        );
    }

    @Test
    void tagOfRootFlowSequence() {
        rewriteRun(
          yaml(
            """
              !!seq [a]
              """,
            spec -> spec.afterRecipe(documents -> {
                Yaml.Sequence sequence = (Yaml.Sequence) documents.getDocuments().getFirst().getBlock();
                assertThat(sequence.getTag()).isNotNull();
                assertThat(sequence.getTag().getName()).isEqualTo("seq");
                assertThat(sequence.getOpeningBracketPrefix()).isEmpty();
            })
          )
        );
    }

    @Test
    void tagOfFlowSequenceInFlowSequence() {
        rewriteRun(
          yaml(
            """
              f: [x, !!seq [y]]
              """,
            spec -> spec.afterRecipe(documents -> {
                Yaml.Sequence outer = (Yaml.Sequence) firstValue(documents);
                Yaml.Sequence inner = (Yaml.Sequence) outer.getEntries().get(1).getBlock();
                assertThat(inner.getTag()).isNotNull();
                assertThat(inner.getTag().getName()).isEqualTo("seq");
            })
          )
        );
    }

    @Test
    void tagBeforeAnchorOnFlowSequence() {
        rewriteRun(
          yaml(
            """
              f: !!seq &s [-1, 2]
              """,
            tagBeforeAnchor("s", "seq")
          )
        );
    }

    @Test
    void anchorBeforeTagOnFlowSequence() {
        rewriteRun(
          yaml(
            """
              f: &s !!seq [x, y]
              """,
            anchorBeforeTag("s", "seq")
          )
        );
    }

    @Test
    void tagOfFlowMapping() {
        rewriteRun(
          yaml(
            """
              f: !!map { a: b }
              """,
            spec -> spec.afterRecipe(documents -> {
                Yaml.Mapping mapping = (Yaml.Mapping) firstValue(documents);
                assertThat(mapping.getTag()).isNotNull();
                assertThat(mapping.getTag().getName()).isEqualTo("map");
                assertThat(mapping.getOpeningBracePrefix()).isEmpty();
            })
          )
        );
    }

    @Test
    void tagBeforeAnchorOnFlowMapping() {
        rewriteRun(
          yaml(
            """
              f: !!map &m { a: b }
              """,
            tagBeforeAnchor("m", "map")
          )
        );
    }

    @Test
    void anchorBeforeTagOnRootBlockMapping() {
        rewriteRun(
          yaml(
            """
              --- &r !!map
              a: b
              """,
            anchorBeforeTag("r", "map")
          )
        );
    }

    @Test
    void anchorBeforeTagOnBlockMappingIndentedByFour() {
        rewriteRun(
          yaml(
            """
              a: &m !!map
                  b: c
              """,
            anchorBeforeTag("m", "map")
          )
        );
    }

    @Test
    void anchorOnRootBlockMapping() {
        rewriteRun(
          yaml(
            """
              --- &r
              a: b
              """
          )
        );
    }

    @Test
    void anchorOnRootBlockSequence() {
        rewriteRun(
          yaml(
            """
              --- &r
              - x
              """
          )
        );
    }

    @Test
    void anchorOnFlowSequenceInFlowSequence() {
        rewriteRun(
          yaml(
            """
              f: [x, &a [y]]
              """
          )
        );
    }

    private static Consumer<SourceSpec<Yaml.Documents>> tagBeforeAnchor(String anchor, String tag) {
        return properties(anchor, tag, true);
    }

    private static Consumer<SourceSpec<Yaml.Documents>> anchorBeforeTag(String anchor, String tag) {
        return properties(anchor, tag, false);
    }

    private static Consumer<SourceSpec<Yaml.Documents>> properties(String anchor, String tag, boolean tagBeforeAnchor) {
        return spec -> spec.afterRecipe(documents -> {
            Yaml node = anchored(documents, anchor);
            assertThat(tagOf(node)).isNotNull();
            assertThat(tagOf(node).getName()).isEqualTo(tag);
            assertThat(node.getMarkers().findFirst(TagBeforeAnchor.class).isPresent()).isEqualTo(tagBeforeAnchor);
        });
    }

    private static Yaml.@Nullable Tag tagOf(Yaml node) {
        if (node instanceof Yaml.Scalar scalar) {
            return scalar.getTag();
        } else if (node instanceof Yaml.Mapping mapping) {
            return mapping.getTag();
        }
        return ((Yaml.Sequence) node).getTag();
    }

    private static Yaml anchored(Yaml.Documents documents, String key) {
        List<Yaml> found = new ArrayList<>();
        new YamlIsoVisitor<Integer>() {
            @Override
            public Yaml.Scalar visitScalar(Yaml.Scalar scalar, Integer p) {
                if (scalar.getAnchor() != null && key.equals(scalar.getAnchor().getKey())) {
                    found.add(scalar);
                }
                return super.visitScalar(scalar, p);
            }

            @Override
            public Yaml.Mapping visitMapping(Yaml.Mapping mapping, Integer p) {
                if (mapping.getAnchor() != null && key.equals(mapping.getAnchor().getKey())) {
                    found.add(mapping);
                }
                return super.visitMapping(mapping, p);
            }

            @Override
            public Yaml.Sequence visitSequence(Yaml.Sequence sequence, Integer p) {
                if (sequence.getAnchor() != null && key.equals(sequence.getAnchor().getKey())) {
                    found.add(sequence);
                }
                return super.visitSequence(sequence, p);
            }
        }.visit(documents, 0);
        assertThat(found).hasSize(1);
        return found.getFirst();
    }

    private static Yaml.Block firstValue(Yaml.Documents documents) {
        return ((Yaml.Mapping) documents.getDocuments().getFirst().getBlock()).getEntries().getFirst().getValue();
    }
}
