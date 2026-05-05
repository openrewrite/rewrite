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
import org.openrewrite.style.GeneralFormatStyle;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EditorConfigStylesTest {

    @Test
    void useTabCharacterTab() {
        Map<String, String> props = new HashMap<>();
        props.put("indent_style", "tab");
        assertThat(EditorConfigStyles.useTabCharacter(props)).isTrue();
    }

    @Test
    void useTabCharacterSpace() {
        Map<String, String> props = new HashMap<>();
        props.put("indent_style", "space");
        assertThat(EditorConfigStyles.useTabCharacter(props)).isFalse();
    }

    @Test
    void useTabCharacterUnset() {
        assertThat(EditorConfigStyles.useTabCharacter(new HashMap<>())).isNull();
    }

    @Test
    void indentSizeNumeric() {
        Map<String, String> props = new HashMap<>();
        props.put("indent_size", "4");
        assertThat(EditorConfigStyles.indentSize(props)).isEqualTo(4);
    }

    @Test
    void indentSizeTab() {
        Map<String, String> props = new HashMap<>();
        props.put("indent_size", "tab");
        props.put("tab_width", "8");
        assertThat(EditorConfigStyles.indentSize(props)).isEqualTo(8);
    }

    @Test
    void indentSizeUnset() {
        assertThat(EditorConfigStyles.indentSize(new HashMap<>())).isNull();
    }

    @Test
    void tabSizeExplicit() {
        Map<String, String> props = new HashMap<>();
        props.put("tab_width", "8");
        props.put("indent_size", "4");
        assertThat(EditorConfigStyles.tabSize(props)).isEqualTo(8);
    }

    @Test
    void tabSizeFallsBackToIndentSize() {
        Map<String, String> props = new HashMap<>();
        props.put("indent_size", "4");
        assertThat(EditorConfigStyles.tabSize(props)).isEqualTo(4);
    }

    @Test
    void tabSizeUnset() {
        assertThat(EditorConfigStyles.tabSize(new HashMap<>())).isNull();
    }

    @Test
    void generalFormatStyleCrlf() {
        Map<String, String> props = new HashMap<>();
        props.put("end_of_line", "crlf");
        GeneralFormatStyle style = EditorConfigStyles.generalFormatStyle(props);
        assertThat(style).isNotNull();
        assertThat(style.isUseCRLFNewLines()).isTrue();
    }

    @Test
    void generalFormatStyleLf() {
        Map<String, String> props = new HashMap<>();
        props.put("end_of_line", "lf");
        GeneralFormatStyle style = EditorConfigStyles.generalFormatStyle(props);
        assertThat(style).isNotNull();
        assertThat(style.isUseCRLFNewLines()).isFalse();
    }

    @Test
    void generalFormatStyleUnset() {
        assertThat(EditorConfigStyles.generalFormatStyle(new HashMap<>())).isNull();
    }

    @Test
    void invalidIndentSizeIgnored() {
        Map<String, String> props = new HashMap<>();
        props.put("indent_size", "abc");
        assertThat(EditorConfigStyles.indentSize(props)).isNull();
    }

    @Test
    void zeroIndentSizeIgnored() {
        Map<String, String> props = new HashMap<>();
        props.put("indent_size", "0");
        assertThat(EditorConfigStyles.indentSize(props)).isNull();
    }
}
