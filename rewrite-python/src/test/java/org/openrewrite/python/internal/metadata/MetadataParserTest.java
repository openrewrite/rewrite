/*
 * Copyright 2026 the original author or authors.
 * <p>
 * Licensed under the Moderne Source Available License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://docs.moderne.io/licensing/moderne-source-available-license
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.python.internal.metadata;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MetadataParserTest {

    private static CoreMetadata parse(String metadata) {
        return MetadataParser.parse(metadata.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void basicFields() {
        CoreMetadata metadata = parse("""
          Metadata-Version: 2.1
          Name: foo
          Version: 1.0
          Requires-Python: >=3.8
          """);
        assertThat(metadata.getMetadataVersion()).isEqualTo("2.1");
        assertThat(metadata.getName()).isEqualTo("foo");
        assertThat(metadata.getVersion()).isEqualTo("1.0");
        assertThat(metadata.getRequiresPython()).isEqualTo(">=3.8");
        assertThat(metadata.getRequiresDist()).isEmpty();
        assertThat(metadata.getProvidesExtra()).isEmpty();
        assertThat(metadata.getDynamic()).isEmpty();
    }

    @Test
    void repeatedFieldsAccumulate() {
        CoreMetadata metadata = parse("""
          Metadata-Version: 2.1
          Name: foo
          Version: 1.0
          Requires-Dist: charset-normalizer (<4,>=2)
          Requires-Dist: idna (<4,>=2.5)
          Requires-Dist: PySocks (!=1.5.7,>=1.5.6) ; extra == 'socks'
          Provides-Extra: socks
          Provides-Extra: security
          """);
        assertThat(metadata.getRequiresDist()).containsExactly(
          "charset-normalizer (<4,>=2)",
          "idna (<4,>=2.5)",
          "PySocks (!=1.5.7,>=1.5.6) ; extra == 'socks'");
        assertThat(metadata.getProvidesExtra()).containsExactly("socks", "security");
    }

    @Test
    void continuationLines() {
        CoreMetadata metadata = parse("""
          Metadata-Version: 2.1
          Name: foo
          Version: 1.0
          Requires-Dist: requests (>=2.0) ;
            python_version < "3.8"
          Requires-Dist: idna
          """);
        assertThat(metadata.getRequiresDist()).containsExactly(
          "requests (>=2.0) ; python_version < \"3.8\"",
          "idna");
    }

    @Test
    void crlfLineEndings() {
        CoreMetadata metadata = parse(
          "Metadata-Version: 2.2\r\nName: foo\r\nVersion: 1.0\r\nRequires-Dist: bar\r\n\r\nbody\r\n");
        assertThat(metadata.getMetadataVersion()).isEqualTo("2.2");
        assertThat(metadata.getRequiresDist()).containsExactly("bar");
    }

    @Test
    void bodyAfterBlankLineIgnored() {
        CoreMetadata metadata = parse("""
          Metadata-Version: 2.1
          Name: foo
          Version: 1.0

          Description body.
          Requires-Dist: not-a-real-requirement
          """);
        assertThat(metadata.getRequiresDist()).isEmpty();
    }

    @Test
    void dynamicFieldNamesLowerCased() {
        CoreMetadata metadata = parse("""
          Metadata-Version: 2.2
          Name: foo
          Version: 1.0
          Dynamic: Requires-Dist
          Dynamic: Provides-Extra, Requires-Python
          """);
        assertThat(metadata.getDynamic()).containsExactly("requires-dist", "provides-extra", "requires-python");
    }

    @Test
    void trustGate() {
        assertThat(parse("Metadata-Version: 2.1\nName: a\nVersion: 1\n").hasStaticRequiresDist()).isFalse();
        assertThat(parse("Metadata-Version: 2.2\nName: a\nVersion: 1\n").hasStaticRequiresDist()).isTrue();
        assertThat(parse("Metadata-Version: 2.4\nName: a\nVersion: 1\n").hasStaticRequiresDist()).isTrue();
        assertThat(parse("Metadata-Version: 3.0\nName: a\nVersion: 1\n").hasStaticRequiresDist()).isTrue();
        assertThat(parse("Metadata-Version: 2.2\nName: a\nVersion: 1\nDynamic: Requires-Dist\n").hasStaticRequiresDist()).isFalse();
        assertThat(parse("Metadata-Version: 2.2\nName: a\nVersion: 1\nDynamic: Classifier\n").hasStaticRequiresDist()).isTrue();
        assertThat(parse("Metadata-Version: 2\nName: a\nVersion: 1\n").hasStaticRequiresDist()).isFalse();
        assertThat(parse("Metadata-Version: garbage\nName: a\nVersion: 1\n").hasStaticRequiresDist()).isFalse();
        assertThat(parse("Name: a\nVersion: 1\n").hasStaticRequiresDist()).isFalse();
    }

    @Test
    void fieldNamesCaseInsensitive() {
        CoreMetadata metadata = parse("""
          metadata-version: 2.1
          NAME: foo
          version: 1.0
          REQUIRES-DIST: bar
          """);
        assertThat(metadata.getName()).isEqualTo("foo");
        assertThat(metadata.getRequiresDist()).containsExactly("bar");
    }

    @Test
    void realWorldMetadata() throws IOException {
        byte[] bytes;
        try (InputStream is = MetadataParserTest.class.getResourceAsStream("/metadata/requests-2.31.0-METADATA")) {
            assertThat(is).isNotNull();
            bytes = is.readAllBytes();
        }
        CoreMetadata metadata = MetadataParser.parse(bytes);
        assertThat(metadata.getName()).isEqualTo("requests");
        assertThat(metadata.getVersion()).isEqualTo("2.31.0");
        assertThat(metadata.getRequiresPython()).isEqualTo(">=3.7");
        assertThat(metadata.getRequiresDist()).containsExactly(
          "charset-normalizer (<4,>=2)",
          "idna (<4,>=2.5)",
          "urllib3 (<3,>=1.21.1)",
          "certifi (>=2017.4.17)",
          "PySocks (!=1.5.7,>=1.5.6) ; extra == 'socks'",
          "chardet (<6,>=3.0.2) ; extra == 'use_chardet_on_py3'");
        assertThat(metadata.getProvidesExtra()).containsExactly("security", "socks", "use_chardet_on_py3");
        assertThat(metadata.hasStaticRequiresDist()).isFalse();
    }
}
