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
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EditorConfigResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void noEditorConfig() {
        EditorConfigResolver resolver = new EditorConfigResolver(tempDir);
        Map<String, String> props = resolver.resolve(tempDir.resolve("pom.xml"));
        assertThat(props).isEmpty();
    }

    @Test
    void singleEditorConfigAtRoot() throws IOException {
        Files.write(tempDir.resolve(".editorconfig"), Arrays.asList(
                "root = true",
                "",
                "[*]",
                "indent_style = space",
                "indent_size = 4"
        ));
        EditorConfigResolver resolver = new EditorConfigResolver(tempDir);
        Map<String, String> props = resolver.resolve(tempDir.resolve("pom.xml"));
        assertThat(props)
                .containsEntry("indent_style", "space")
                .containsEntry("indent_size", "4");
    }

    @Test
    void xmlSpecificSection() throws IOException {
        Files.write(tempDir.resolve(".editorconfig"), Arrays.asList(
                "root = true",
                "",
                "[*]",
                "indent_size = 4",
                "",
                "[*.xml]",
                "indent_size = 2"
        ));
        EditorConfigResolver resolver = new EditorConfigResolver(tempDir);
        Map<String, String> props = resolver.resolve(tempDir.resolve("pom.xml"));
        assertThat(props).containsEntry("indent_size", "2");
    }

    @Test
    void childOverridesParent() throws IOException {
        Files.write(tempDir.resolve(".editorconfig"), Arrays.asList(
                "root = true",
                "",
                "[*]",
                "indent_size = 4"
        ));
        Path subDir = Files.createDirectories(tempDir.resolve("src"));
        Files.write(subDir.resolve(".editorconfig"), Arrays.asList(
                "[*]",
                "indent_size = 2"
        ));
        EditorConfigResolver resolver = new EditorConfigResolver(tempDir);
        Map<String, String> props = resolver.resolve(subDir.resolve("pom.xml"));
        assertThat(props).containsEntry("indent_size", "2");
    }

    @Test
    void childInheritsFromParent() throws IOException {
        Files.write(tempDir.resolve(".editorconfig"), Arrays.asList(
                "root = true",
                "",
                "[*]",
                "indent_style = space",
                "indent_size = 4"
        ));
        Path subDir = Files.createDirectories(tempDir.resolve("src"));
        Files.write(subDir.resolve(".editorconfig"), Arrays.asList(
                "[*.xml]",
                "indent_size = 2"
        ));
        EditorConfigResolver resolver = new EditorConfigResolver(tempDir);
        Map<String, String> props = resolver.resolve(subDir.resolve("pom.xml"));
        assertThat(props)
                .containsEntry("indent_style", "space")
                .containsEntry("indent_size", "2");
    }

    @Test
    void rootTrueStopsWalking() throws IOException {
        Files.write(tempDir.resolve(".editorconfig"), Arrays.asList(
                "root = true",
                "",
                "[*]",
                "indent_size = 4"
        ));
        Path subDir = Files.createDirectories(tempDir.resolve("sub"));
        Files.write(subDir.resolve(".editorconfig"), Arrays.asList(
                "root = true",
                "",
                "[*]",
                "indent_size = 2"
        ));
        EditorConfigResolver resolver = new EditorConfigResolver(tempDir);
        Map<String, String> props = resolver.resolve(subDir.resolve("file.xml"));
        // Should only see the sub/.editorconfig since it has root=true
        assertThat(props).containsEntry("indent_size", "2");
    }

    @Test
    void braceExpansionPattern() throws IOException {
        Files.write(tempDir.resolve(".editorconfig"), Arrays.asList(
                "root = true",
                "",
                "[*.{xml,xsl}]",
                "indent_size = 2"
        ));
        EditorConfigResolver resolver = new EditorConfigResolver(tempDir);
        assertThat(resolver.resolve(tempDir.resolve("file.xml"))).containsEntry("indent_size", "2");
        assertThat(resolver.resolve(tempDir.resolve("file.xsl"))).containsEntry("indent_size", "2");
        assertThat(resolver.resolve(tempDir.resolve("file.java"))).isEmpty();
    }

    @Test
    void sameDirectoryReturnsConsistentResults() throws IOException {
        Files.write(tempDir.resolve(".editorconfig"), Arrays.asList(
                "root = true",
                "",
                "[*]",
                "indent_size = 4"
        ));
        EditorConfigResolver resolver = new EditorConfigResolver(tempDir);
        Map<String, String> first = resolver.resolve(tempDir.resolve("a.xml"));
        Map<String, String> second = resolver.resolve(tempDir.resolve("b.xml"));
        assertThat(first).isEqualTo(second);
    }

    @Test
    void tabSettings() throws IOException {
        Files.write(tempDir.resolve(".editorconfig"), Arrays.asList(
                "root = true",
                "",
                "[*]",
                "indent_style = tab",
                "tab_width = 4"
        ));
        EditorConfigResolver resolver = new EditorConfigResolver(tempDir);
        Map<String, String> props = resolver.resolve(tempDir.resolve("pom.xml"));
        assertThat(props)
                .containsEntry("indent_style", "tab")
                .containsEntry("tab_width", "4");
    }

    @Test
    void endOfLineProperty() throws IOException {
        Files.write(tempDir.resolve(".editorconfig"), Arrays.asList(
                "root = true",
                "",
                "[*]",
                "end_of_line = crlf"
        ));
        EditorConfigResolver resolver = new EditorConfigResolver(tempDir);
        Map<String, String> props = resolver.resolve(tempDir.resolve("pom.xml"));
        assertThat(props).containsEntry("end_of_line", "crlf");
    }
}
