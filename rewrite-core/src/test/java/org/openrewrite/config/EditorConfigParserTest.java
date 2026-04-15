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
package org.openrewrite.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class EditorConfigParserTest {

    private final EditorConfigParser parser = new EditorConfigParser();

    @Test
    void emptyFile() {
        EditorConfigParser.EditorConfigFile result = parser.parse(Collections.emptyList());
        assertThat(result.isRoot()).isFalse();
        assertThat(result.getSections()).isEmpty();
    }

    @Test
    void rootTrue() {
        EditorConfigParser.EditorConfigFile result = parser.parse(Arrays.asList(
                "root = true",
                "",
                "[*]",
                "indent_style = space"
        ));
        assertThat(result.isRoot()).isTrue();
        assertThat(result.getSections()).hasSize(1);
    }

    @Test
    void rootFalseByDefault() {
        EditorConfigParser.EditorConfigFile result = parser.parse(Arrays.asList(
                "[*]",
                "indent_size = 4"
        ));
        assertThat(result.isRoot()).isFalse();
    }

    @Test
    void multipleSections() {
        EditorConfigParser.EditorConfigFile result = parser.parse(Arrays.asList(
                "[*]",
                "indent_style = space",
                "indent_size = 4",
                "",
                "[*.xml]",
                "indent_size = 2"
        ));
        assertThat(result.getSections()).hasSize(2);
        assertThat(result.getSections().get(0).getPattern()).isEqualTo("*");
        assertThat(result.getSections().get(0).getProperties()).containsEntry("indent_style", "space");
        assertThat(result.getSections().get(0).getProperties()).containsEntry("indent_size", "4");
        assertThat(result.getSections().get(1).getPattern()).isEqualTo("*.xml");
        assertThat(result.getSections().get(1).getProperties()).containsEntry("indent_size", "2");
    }

    @Test
    void commentsAreSkipped() {
        EditorConfigParser.EditorConfigFile result = parser.parse(Arrays.asList(
                "# This is a comment",
                "; This is also a comment",
                "[*]",
                "# inline comment line",
                "indent_style = space"
        ));
        assertThat(result.getSections()).hasSize(1);
        assertThat(result.getSections().get(0).getProperties()).hasSize(1);
    }

    @Test
    void whitespaceAroundEquals() {
        EditorConfigParser.EditorConfigFile result = parser.parse(Arrays.asList(
                "[*]",
                "indent_style=tab",
                "indent_size =\t4",
                "  tab_width  =  8  "
        ));
        assertThat(result.getSections().get(0).getProperties())
                .containsEntry("indent_style", "tab")
                .containsEntry("indent_size", "4")
                .containsEntry("tab_width", "8");
    }

    @Test
    void malformedLinesAreSkipped() {
        EditorConfigParser.EditorConfigFile result = parser.parse(Arrays.asList(
                "[*]",
                "no_equals_sign",
                "indent_size = 4"
        ));
        assertThat(result.getSections().get(0).getProperties()).hasSize(1);
        assertThat(result.getSections().get(0).getProperties()).containsEntry("indent_size", "4");
    }

    @Test
    void keysAreLowercased() {
        EditorConfigParser.EditorConfigFile result = parser.parse(Arrays.asList(
                "[*]",
                "Indent_Style = Space",
                "INDENT_SIZE = 4"
        ));
        assertThat(result.getSections().get(0).getProperties())
                .containsEntry("indent_style", "space")
                .containsEntry("indent_size", "4");
    }

    @Test
    void braceExpansionPattern() {
        EditorConfigParser.EditorConfigFile result = parser.parse(Arrays.asList(
                "[*.{xml,xsl}]",
                "indent_size = 2"
        ));
        assertThat(result.getSections().get(0).getPattern()).isEqualTo("*.{xml,xsl}");
    }
}
