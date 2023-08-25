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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openrewrite.SourceFile;
import org.openrewrite.test.RewriteTest;
import org.openrewrite.tree.ParseError;
import org.openrewrite.yaml.tree.Yaml;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.yaml.Assertions.yaml;

class YamlParserTest implements RewriteTest {

    @Test
    void ascii() {
        List<SourceFile> yamlSources = YamlParser.builder().build().parse("a: b\n").toList();
        assertThat(yamlSources).singleElement().isInstanceOf(Yaml.Documents.class);

        Yaml.Documents documents = (Yaml.Documents) yamlSources.get(0);
        Yaml.Document document = documents.getDocuments().get(0);

        // Assert that end is parsed correctly
        assertThat(document.getEnd().getPrefix()).isEqualTo("\n");

        // Assert that the title is parsed correctly
        Yaml.Mapping mapping = (Yaml.Mapping) document.getBlock();
        Yaml.Mapping.Entry entry = mapping.getEntries().get(0);
        Yaml.Scalar title = (Yaml.Scalar) entry.getValue();
        assertThat(title.getValue()).isEqualTo("b");
    }

    @ParameterizedTest
    @ValueSource(strings = {
      "🛠",
      "🛠🛠",
      "🛠 🛠"
    })
    void unicodeParseError(String input ) {
        Stream<SourceFile> yamlSources = YamlParser.builder().build().parse("a: %s\n".formatted(input));
        assertThat(yamlSources).singleElement().isInstanceOf(ParseError.class);
    }

}
