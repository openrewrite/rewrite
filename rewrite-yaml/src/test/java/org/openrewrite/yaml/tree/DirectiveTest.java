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
package org.openrewrite.yaml.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.marker.Markers;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.yaml.YamlVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.yaml.Assertions.yaml;

class DirectiveTest implements RewriteTest {

    @Test
    void yamlDirective() {
        rewriteRun(
          yaml(
            """
              %YAML 1.2
              ---
              key: value
              """,
            spec -> spec.afterRecipe(y -> {
                // given
                Yaml.Document doc = y.getDocuments().getFirst();

                // then
                assertThat(doc.getDirectives()).hasSize(1);
                Yaml.Directive directive = doc.getDirectives().getFirst();
                assertThat(directive.getValue()).isEqualTo("YAML 1.2");
            })
          )
        );
    }

    @Test
    void tagDirective() {
        rewriteRun(
          yaml(
            """
              %TAG !yaml! tag:yaml.org,2002:
              ---
              key: value
              """,
            spec -> spec.afterRecipe(y -> {
                // given
                Yaml.Document doc = y.getDocuments().getFirst();

                // then
                assertThat(doc.getDirectives()).hasSize(1);
                Yaml.Directive directive = doc.getDirectives().getFirst();
                assertThat(directive.getValue()).isEqualTo("TAG !yaml! tag:yaml.org,2002:");
            })
          )
        );
    }

    @Test
    void multipleDirectives() {
        rewriteRun(
          yaml(
            """
              %YAML 1.2
              %TAG !yaml! tag:yaml.org,2002:
              ---
              key: value
              """,
            spec -> spec.afterRecipe(y -> {
                // given
                Yaml.Document doc = y.getDocuments().getFirst();

                // then
                assertThat(doc.getDirectives()).hasSize(2);
                assertThat(doc.getDirectives().get(0).getValue()).isEqualTo("YAML 1.2");
                assertThat(doc.getDirectives().get(1).getValue()).isEqualTo("TAG !yaml! tag:yaml.org,2002:");
            })
          )
        );
    }

    @Test
    void directiveWithWhitespace() {
        rewriteRun(
          yaml(
            "\n%YAML 1.2\n---\nkey: value\n",
            spec -> spec.afterRecipe(y -> {
                // given
                Yaml.Document doc = y.getDocuments().getFirst();

                // then - verify directive is parsed correctly (round-trip already verified by test framework)
                assertThat(doc.getDirectives()).hasSize(1);
                Yaml.Directive directive = doc.getDirectives().getFirst();
                assertThat(directive.getValue()).isEqualTo("YAML 1.2");
                // Note: the leading newline may be in document prefix or directive prefix depending on parsing
            })
          )
        );
    }

    @Test
    void documentWithoutDirectives() {
        rewriteRun(
          yaml(
            """
              ---
              key: value
              """,
            spec -> spec.afterRecipe(y -> {
                // given
                Yaml.Document doc = y.getDocuments().getFirst();

                // then
                assertThat(doc.getDirectives()).isEmpty();
            })
          )
        );
    }

    @Test
    void directiveCopyPaste() {
        // given
        Yaml.Directive original = new Yaml.Directive(
                UUID.randomUUID(),
                "",
                Markers.EMPTY,
                "YAML 1.2",
                "\n"
        );

        // when
        Yaml.Directive copy = original.copyPaste();

        // then
        assertThat(copy.getId()).isNotEqualTo(original.getId());
        assertThat(copy.getPrefix()).isEqualTo(original.getPrefix());
        assertThat(copy.getValue()).isEqualTo(original.getValue());
        assertThat(copy.getSuffix()).isEqualTo(original.getSuffix());
    }

    @Test
    void directiveVisitor() {
        // given
        List<String> visited = new ArrayList<>();
        YamlVisitor<Integer> visitor = new YamlVisitor<>() {
            @Override
            public Yaml visitDirective(Yaml.Directive directive, Integer p) {
                visited.add(directive.getValue());
                return super.visitDirective(directive, p);
            }
        };

        Yaml.Directive directive = new Yaml.Directive(
                UUID.randomUUID(),
                "",
                Markers.EMPTY,
                "YAML 1.2",
                "\n"
        );
        Yaml.Document.End end = new Yaml.Document.End(UUID.randomUUID(), "", Markers.EMPTY, false);
        Yaml.Mapping block = new Yaml.Mapping(UUID.randomUUID(), Markers.EMPTY, null, emptyList(), null, null, null);
        Yaml.Document document = new Yaml.Document(
                UUID.randomUUID(),
                "",
                Markers.EMPTY,
                singletonList(directive),
                true,
                block,
                end
        );

        // when
        visitor.visit(document, 0);

        // then
        assertThat(visited).containsExactly("YAML 1.2");
    }
}
