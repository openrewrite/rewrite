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
package org.openrewrite.xml.format;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.openrewrite.InMemoryExecutionContext;
import org.openrewrite.SourceFile;
import org.openrewrite.config.EditorConfigResolver;
import org.openrewrite.style.GeneralFormatStyle;
import org.openrewrite.style.NamedStyles;
import org.openrewrite.style.Style;
import org.openrewrite.tree.ParsingExecutionContextView;
import org.openrewrite.xml.XmlParser;
import org.openrewrite.xml.style.TabsAndIndentsStyle;
import org.openrewrite.xml.tree.Xml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class EditorConfigIntegrationTest {

    @TempDir
    Path tempDir;

    private ParsingExecutionContextView ctxWithEditorConfig() {
        ParsingExecutionContextView ctx = ParsingExecutionContextView.view(new InMemoryExecutionContext());
        ctx.setEditorConfigResolver(new EditorConfigResolver(tempDir));
        return ctx;
    }

    @Test
    void parsedDocumentHasEditorConfigStyle() throws IOException {
        Files.write(tempDir.resolve(".editorconfig"), Arrays.asList(
                "root = true",
                "",
                "[*]",
                "indent_style = space",
                "indent_size = 4"
        ));
        Files.write(tempDir.resolve("pom.xml"), Arrays.asList(
                "<project>",
                "  <groupId>com.example</groupId>",
                "</project>"
        ));

        List<SourceFile> results = new XmlParser()
                .parse(Collections.singletonList(tempDir.resolve("pom.xml")), tempDir, ctxWithEditorConfig())
                .collect(Collectors.toList());

        assertThat(results).hasSize(1);
        Xml.Document doc = (Xml.Document) results.get(0);
        TabsAndIndentsStyle style = Style.from(TabsAndIndentsStyle.class, doc);
        assertThat(style).isNotNull();
        assertThat(style.getUseTabCharacter()).isFalse();
        assertThat(style.getIndentSize()).isEqualTo(4);
    }

    @Test
    void xmlSpecificSectionOverridesWildcard() throws IOException {
        Files.write(tempDir.resolve(".editorconfig"), Arrays.asList(
                "root = true",
                "",
                "[*]",
                "indent_size = 4",
                "",
                "[*.xml]",
                "indent_size = 2"
        ));
        Files.write(tempDir.resolve("pom.xml"), Arrays.asList(
                "<project>",
                "    <groupId>com.example</groupId>",
                "</project>"
        ));

        List<SourceFile> results = new XmlParser()
                .parse(Collections.singletonList(tempDir.resolve("pom.xml")), tempDir, ctxWithEditorConfig())
                .collect(Collectors.toList());

        Xml.Document doc = (Xml.Document) results.get(0);
        TabsAndIndentsStyle style = Style.from(TabsAndIndentsStyle.class, doc);
        assertThat(style).isNotNull();
        assertThat(style.getIndentSize()).isEqualTo(2);
    }

    @Test
    void endOfLineFromEditorConfig() throws IOException {
        Files.write(tempDir.resolve(".editorconfig"), Arrays.asList(
                "root = true",
                "",
                "[*]",
                "end_of_line = crlf"
        ));
        Files.write(tempDir.resolve("pom.xml"), Arrays.asList(
                "<project>",
                "  <groupId>com.example</groupId>",
                "</project>"
        ));

        List<SourceFile> results = new XmlParser()
                .parse(Collections.singletonList(tempDir.resolve("pom.xml")), tempDir, ctxWithEditorConfig())
                .collect(Collectors.toList());

        Xml.Document doc = (Xml.Document) results.get(0);
        GeneralFormatStyle style = Style.from(GeneralFormatStyle.class, doc);
        assertThat(style).isNotNull();
        assertThat(style.isUseCRLFNewLines()).isTrue();
    }

    @Test
    void noEditorConfigResolverMeansNoStyleAttached() throws IOException {
        Files.write(tempDir.resolve(".editorconfig"), Arrays.asList(
                "root = true",
                "",
                "[*]",
                "indent_size = 4"
        ));
        Files.write(tempDir.resolve("pom.xml"), Arrays.asList(
                "<project>",
                "  <groupId>com.example</groupId>",
                "</project>"
        ));

        // Parse WITHOUT setting an EditorConfigResolver on the context
        List<SourceFile> results = new XmlParser()
                .parse(Collections.singletonList(tempDir.resolve("pom.xml")), tempDir, new InMemoryExecutionContext())
                .collect(Collectors.toList());

        Xml.Document doc = (Xml.Document) results.get(0);
        List<NamedStyles> namedStyles = doc.getMarkers().findAll(NamedStyles.class);
        assertThat(namedStyles).isEmpty();
    }

    @Test
    void tabIndentation() throws IOException {
        Files.write(tempDir.resolve(".editorconfig"), Arrays.asList(
                "root = true",
                "",
                "[*]",
                "indent_style = tab",
                "tab_width = 4"
        ));
        Files.write(tempDir.resolve("pom.xml"), Arrays.asList(
                "<project>",
                "\t<groupId>com.example</groupId>",
                "</project>"
        ));

        List<SourceFile> results = new XmlParser()
                .parse(Collections.singletonList(tempDir.resolve("pom.xml")), tempDir, ctxWithEditorConfig())
                .collect(Collectors.toList());

        Xml.Document doc = (Xml.Document) results.get(0);
        TabsAndIndentsStyle style = Style.from(TabsAndIndentsStyle.class, doc);
        assertThat(style).isNotNull();
        assertThat(style.getUseTabCharacter()).isTrue();
        assertThat(style.getTabSize()).isEqualTo(4);
    }
}
