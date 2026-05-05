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
package org.openrewrite.xml.style;

import org.junit.jupiter.api.Test;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.NamedStyles;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EditorConfigStyleMapperTest {

    @Test
    void emptyPropertiesReturnsNull() {
        assertThat(EditorConfigStyleMapper.fromEditorConfig(new HashMap<>())).isNull();
    }

    @Test
    void irrelevantPropertiesReturnsNull() {
        Map<String, String> props = new HashMap<>();
        props.put("charset", "utf-8");
        assertThat(EditorConfigStyleMapper.fromEditorConfig(props)).isNull();
    }

    @Test
    void indentStyleSpace() {
        Map<String, String> props = new HashMap<>();
        props.put("indent_style", "space");
        props.put("indent_size", "4");
        NamedStyles styles = EditorConfigStyleMapper.fromEditorConfig(props);
        assertThat(styles).isNotNull();
        TabsAndIndentsStyle tabsStyle = styles.getStyle(TabsAndIndentsStyle.class);
        assertThat(tabsStyle.getUseTabCharacter()).isFalse();
        assertThat(tabsStyle.getIndentSize()).isEqualTo(4);
        assertThat(tabsStyle.getContinuationIndentSize()).isEqualTo(8);
    }

    @Test
    void indentStyleTab() {
        Map<String, String> props = new HashMap<>();
        props.put("indent_style", "tab");
        props.put("tab_width", "4");
        NamedStyles styles = EditorConfigStyleMapper.fromEditorConfig(props);
        assertThat(styles).isNotNull();
        TabsAndIndentsStyle tabsStyle = styles.getStyle(TabsAndIndentsStyle.class);
        assertThat(tabsStyle.getUseTabCharacter()).isTrue();
        assertThat(tabsStyle.getTabSize()).isEqualTo(4);
    }

    @Test
    void endOfLineCrlf() {
        Map<String, String> props = new HashMap<>();
        props.put("end_of_line", "crlf");
        NamedStyles styles = EditorConfigStyleMapper.fromEditorConfig(props);
        assertThat(styles).isNotNull();
        GeneralFormatStyle generalStyle = styles.getStyle(GeneralFormatStyle.class);
        assertThat(generalStyle.isUseCRLFNewLines()).isTrue();
    }

    @Test
    void endOfLineLf() {
        Map<String, String> props = new HashMap<>();
        props.put("end_of_line", "lf");
        NamedStyles styles = EditorConfigStyleMapper.fromEditorConfig(props);
        assertThat(styles).isNotNull();
        GeneralFormatStyle generalStyle = styles.getStyle(GeneralFormatStyle.class);
        assertThat(generalStyle.isUseCRLFNewLines()).isFalse();
    }

    @Test
    void allPropertiesCombined() {
        Map<String, String> props = new HashMap<>();
        props.put("indent_style", "space");
        props.put("indent_size", "2");
        props.put("tab_width", "2");
        props.put("end_of_line", "lf");
        NamedStyles styles = EditorConfigStyleMapper.fromEditorConfig(props);
        assertThat(styles).isNotNull();
        TabsAndIndentsStyle tabsStyle = styles.getStyle(TabsAndIndentsStyle.class);
        assertThat(tabsStyle.getUseTabCharacter()).isFalse();
        assertThat(tabsStyle.getIndentSize()).isEqualTo(2);
        assertThat(tabsStyle.getTabSize()).isEqualTo(2);
        assertThat(tabsStyle.getContinuationIndentSize()).isEqualTo(4);
        GeneralFormatStyle generalStyle = styles.getStyle(GeneralFormatStyle.class);
        assertThat(generalStyle.isUseCRLFNewLines()).isFalse();
    }
}
