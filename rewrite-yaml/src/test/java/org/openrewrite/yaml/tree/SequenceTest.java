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
import org.openrewrite.test.RewriteTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.yaml.Assertions.yaml;

class SequenceTest implements RewriteTest {

    @Test
    void blockSequence() {
        rewriteRun(
          yaml(
            """
              - apples
              - oranges
              """,
            spec -> spec.afterRecipe(y ->
                assertThat(((Yaml.Sequence) (y.getDocuments().getFirst().getBlock())).getEntries().stream()
                  .map(Yaml.Sequence.Entry::getBlock)
                  .map(block -> ((Yaml.Scalar) block).getValue()))
                  .containsExactly("apples", "oranges"))
          )
        );
    }

    @Test
    void blockSequenceOfMappings() {
        rewriteRun(
          yaml(
            """
                      - name: Fred
                        age: 45
                      - name: Barney
                        age: 25
              """
          )
        );
    }

    @Test
    void multiLineInlineSequenceWithFunnyIndentation() {
        rewriteRun(
          yaml(
            """
              [
                  a,
              b,
                      c,
              ]
              """
          )
        );
    }

    @Test
    void sequenceOfEmptyInlineSequence() {
        rewriteRun(yaml("- []"));
    }

    @Test
    void sequenceOfMapOfEmptyInlineSequence() {
        rewriteRun(yaml("- foo: []"));
    }

    @Test
    void sequenceOfInlineSequence() {
        rewriteRun(yaml("- [a, b]"));
    }

    @Test
    void sequenceOfMapOfInlineSequence() {
        rewriteRun(yaml("- foo: [a, b]"));
    }

    @Test
    void sequencesOfSequencesOfSequences() {
        rewriteRun(
          yaml(
            """
              [[],
              [1, 2, 3, []],
              [ [ ] ],]
              """
          )
        );
    }

    @Test
    void sequenceOfMixedSequences() {
        rewriteRun(
          yaml(
            """
                  - []
                  - [ 1 ]
                  - foo: []
                  - bar:
                  - baz: [
                      a]
              """
          )
        );
    }

    @Test
    void sequenceWithAnchors() {
        rewriteRun(
          yaml(
            """
            - &first
              A: 1
              B: 2
            - <<: *first
              A: 23
              C: 99
              D: 123
            """
          )
        );
    }

    @Test
    void anchoredInlineSequenceAsBlockSequenceEntry() {
        rewriteRun(
          yaml(
            """
              pairs:
                - &first [ 'alpha', 'beta' ]
                - &second [ 'gamma', 'delta' ]
              """
          )
        );
    }

    @Test
    void anchoredBlockSequenceAsBlockSequenceEntry() {
        rewriteRun(
          yaml(
            """
              groups:
                - &first
                  - alpha
                  - beta
                - &second
                  - gamma
              """
          )
        );
    }

    @Test
    void inlineSequenceWithWhitespaceBeforeCommas() {
        rewriteRun(
          yaml(
            "[1 ,2  ,0]",
            spec -> spec.afterRecipe(y -> {
                var seq = (Yaml.Sequence) y.getDocuments().getFirst().getBlock();
                assertThat(seq.getEntries().getFirst().getTrailingCommaPrefix()).isEqualTo(" ");
                assertThat(seq.getEntries().get(1).getTrailingCommaPrefix()).isEqualTo("  ");
                assertThat(seq.getEntries().get(2).getTrailingCommaPrefix()).isNull();
            })
          )
        );
    }

    @Test
    void commaAfterNestedSequence() {
        rewriteRun(
          yaml(
            "[[1] , 2]",
            spec -> spec.afterRecipe(y -> {
                var seq = (Yaml.Sequence) y.getDocuments().getFirst().getBlock();
                assertThat(seq.getEntries().getFirst().getTrailingCommaPrefix()).isEqualTo(" ");
                assertThat(seq.getEntries().get(1).getBlock().getPrefix()).isEqualTo(" ");
            })
          )
        );
    }

    @Test
    void commaAfterAlias() {
        rewriteRun(
          yaml(
            "[&x 1, *x , 2]",
            spec -> spec.afterRecipe(y -> {
                var seq = (Yaml.Sequence) y.getDocuments().getFirst().getBlock();
                assertThat(seq.getEntries().get(1).getTrailingCommaPrefix()).isEqualTo(" ");
                assertThat(seq.getEntries().get(2).getBlock().getPrefix()).isEqualTo(" ");
            })
          )
        );
    }

    @Test
    void commaAfterPair() {
        rewriteRun(
          yaml(
            "[a: 1, b: 2]",
            spec -> spec.afterRecipe(y -> {
                var seq = (Yaml.Sequence) y.getDocuments().getFirst().getBlock();
                assertThat(seq.getEntries().getFirst().getTrailingCommaPrefix()).isEmpty();
                var pair = (Yaml.Mapping) seq.getEntries().get(1).getBlock();
                assertThat(pair.getEntries().getFirst().getPrefix()).isEqualTo(" ");
            })
          )
        );
    }

    @Test
    void trailingCommaAfterNestedSequences() {
        rewriteRun(
          yaml(
            "[[1], [2], ]",
            spec -> spec.afterRecipe(y -> {
                var seq = (Yaml.Sequence) y.getDocuments().getFirst().getBlock();
                assertThat(seq.getEntries().getFirst().getTrailingCommaPrefix()).isEmpty();
                assertThat(seq.getEntries().get(1).getTrailingCommaPrefix()).isEmpty();
                assertThat(((Yaml.Sequence) seq.getEntries().get(1).getBlock()).getOpeningBracketPrefix()).isEqualTo(" ");
                assertThat(seq.getClosingBracketPrefix()).isEqualTo(" ");
            })
          )
        );
    }

    @Test
    void commaInCommentAfterNestedSequence() {
        rewriteRun(
          yaml(
            """
              [
                [1] # a, b
                , 2
              ]
              """,
            spec -> spec.afterRecipe(y -> {
                var seq = (Yaml.Sequence) y.getDocuments().getFirst().getBlock();
                assertThat(seq.getEntries().getFirst().getTrailingCommaPrefix()).isEqualTo(" # a, b\n  ");
                assertThat(seq.getEntries().get(1).getBlock().getPrefix()).isEqualTo(" ");
            })
          )
        );
    }

    @Test
    void commaAfterSupplementaryCharacterInComment() {
        rewriteRun(
          yaml(
            """
              [
                1 # \uD83D\uDE00
                , [2] # \uD83D\uDE00
                , a: 3 # \uD83D\uDE00
                , 4
              ]
              """
          )
        );
    }
}
