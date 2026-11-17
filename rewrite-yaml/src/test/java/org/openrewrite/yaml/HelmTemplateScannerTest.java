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
package org.openrewrite.yaml;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.yaml.HelmTemplateScanner.Span;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HelmTemplateScannerTest {

    @Nested
    class Actions {

        @Test
        void noActions() {
            assertActions("apiVersion: v1\nkind: Service\n");
        }

        @Test
        void actionsInText() {
            assertActions("name: {{ .Release.Name }}-{{ .Chart.Name }}\n",
              "{{ .Release.Name }}", "{{ .Chart.Name }}");
        }

        @Test
        void trimMarkers() {
            assertActions("a: {{- .x -}}\nb: {{-\t.y\t-}}\n", "{{- .x -}}", "{{-\t.y\t-}}");
        }

        @Test
        void emptyAction() {
            assertActions("{{}}", "{{}}");
        }

        @Test
        void actionSpanningLines() {
            String action = "{{- include \"mychart.labels\" (dict\n    \"context\" .\n    \"extra\" .Values.extra) | nindent 2 }}";
            assertActions("spec:\n  " + action + "\n  replicas: 1\n", action);
        }

        @Test
        void actionSpanningLinesSharingALineWithYaml() {
            String action = "{{ include \"mychart.fullname\"\n           . }}";
            assertActions("metadata:\n  name: " + action + "-config\n", action);
        }

        @Test
        void loneBracesOutsideAnActionAreText() {
            assertActions("a: {x}}\nb: { y }\n");
        }

        @Test
        void bracesInsideAnAction() {
            assertActions("{{ {a} }}{{{name}}}", "{{ {a} }}", "{{{name}}");
        }

        @Test
        void colonsOnOneLine() {
            assertActions("{{ $v := .x }}{{ x[1:3] }}{{ a: b }}", "{{ $v := .x }}", "{{ x[1:3] }}", "{{ a: b }}");
        }

        @Test
        void colonWithoutWhitespaceInActionSpanningLines() {
            String action = "{{ $v :=\n  x[1:3] }}";
            assertActions(action, action);
        }
    }

    @Nested
    class Literals {

        @Test
        void delimitersInsideAString() {
            assertActions("{{ printf \"{{ %s }}\" .x }} rest", "{{ printf \"{{ %s }}\" .x }}");
        }

        @Test
        void escapedQuoteInsideAString() {
            assertActions("{{ \"say \\\"}}\\\" \\\\\" }}", "{{ \"say \\\"}}\\\" \\\\\" }}");
        }

        @Test
        void braceInACharacterLiteral() {
            assertActions("{{ if eq .c '}' }}x{{ end }}", "{{ if eq .c '}' }}", "{{ end }}");
        }

        @Test
        void delimitersInsideASingleQuotedString() {
            assertActions("{{ format('}}{0}', x) }}", "{{ format('}}{0}', x) }}");
        }

        @Test
        void rawStringSpanningLinesWithDelimiters() {
            String action = "{{ `line one\n}} line two` | nindent 2 }}";
            assertActions(action + "\nk: v\n", action);
        }

        @Test
        void mappingKeyInsideALiteralInActionSpanningLines() {
            String action = "{{ printf \"a: %s\"\n  .x }}";
            assertActions(action, action);
        }
    }

    @Nested
    class Comments {

        @Test
        void comment() {
            assertActions("a{{/* note */}}b", "{{/* note */}}");
        }

        @Test
        void commentWithTrimMarkers() {
            assertActions("a {{- /* note */ -}} b", "{{- /* note */ -}}");
        }

        @Test
        void commentSpanningLinesHoldingBracesAndYaml() {
            String comment = "{{- /*\nA comment with {{ braces }} and a key: value\n*/ -}}";
            assertActions(comment + "\nname: x\n", comment);
        }

        @Test
        void commentEndsAtTheFirstClose() {
            assertActions("{{/* a */}} b */}}", "{{/* a */}}");
        }

        @Test
        void slashAfterDashWithoutWhitespaceIsNotAComment() {
            assertActions("{{-/* x */}}", "{{-/* x */}}");
        }
    }

    @Nested
    class OtherSyntaxes {

        @Test
        void githubActionsHyphenatedNames() {
            assertActions("java-version: ${{ matrix.java-version }}\nlink: ${{ steps.check.outputs.failed-check-link }}\n",
              "{{ matrix.java-version }}", "{{ steps.check.outputs.failed-check-link }}");
        }

        @Test
        void githubActionsExpressionSpanningLines() {
            String action = "{{ format('{0}/{1}',\n         matrix.base-url, inputs.path) }}";
            assertActions("url: $" + action + "\n", action);
        }

        @Test
        void yamlEscapedQuotesInASingleQuotedScalar() {
            assertActions("if: '${{ contains(github.event.pull_request.title, ''test'') }}'\n",
              "{{ contains(github.event.pull_request.title, ''test'') }}");
        }

        @Test
        void jinjaTrimMarkersWithoutWhitespace() {
            assertActions("{{-name-}} {{ count-1 }} {{ $x+2 }}", "{{-name-}}", "{{ count-1 }}", "{{ $x+2 }}");
        }

        @Test
        void jinjaFilterSpanningLines() {
            String action = "{{-name\n        | default('x') }}";
            assertActions("name: " + action + "\n", action);
        }

        @Test
        void mustacheSections() {
            assertActions("{{#items}}{{name}}{{/items}}{{> partial}}", "{{#items}}", "{{name}}", "{{/items}}", "{{> partial}}");
        }

        @Test
        void apostropheInProse() {
            assertActions("{{! don't change this }}", "{{! don't change this }}");
        }

        @Test
        void unclosedQuoteIsAnOrdinaryCharacter() {
            assertActions("{{ \"abc }} {{ 'x }}", "{{ \"abc }}", "{{ 'x }}");
        }

        @Test
        void unclosedBacktickIsAnOrdinaryCharacter() {
            assertActions("{{ `abc }}", "{{ `abc }}");
        }

        @Test
        void otherCharacters() {
            assertActions("{{ 3k — \u0001 ) ( }}", "{{ 3k — \u0001 ) ( }}");
        }
    }

    @Nested
    class Rejected {

        @ParameterizedTest
        @ValueSource(strings = {
          "{{",
          "{{ .x",
          "{{/* no end }}",
          "{{/* a */ }}",
          "{{/* a */x}}"
        })
        void unclosed(String source) {
            assertActions(source);
        }

        @Test
        void mappingKeyInActionSpanningLines() {
            assertActions("note: see {{ the docs\nname: web\nport: 80\ndesc: done }}\n");
        }

        @Test
        void mappingKeyAtEndOfLineInActionSpanningLines() {
            assertActions("note: {{ x\nspec:\n  y }}\n");
        }

        @Test
        void scanningCarriesOnAfterARejectedAction() {
            assertActions("a: {{ x\nb: {{ .b }}\nc: {{ .c }}\n", "{{ .b }}", "{{ .c }}");
        }
    }

    private static void assertActions(String source, String... expected) {
        List<String> actual = new ArrayList<>();
        for (Span span : HelmTemplateScanner.scan(source)) {
            actual.add(source.substring(span.getStart(), span.getEnd()));
        }
        assertThat(actual).as(source).containsExactly(expected);
    }
}
