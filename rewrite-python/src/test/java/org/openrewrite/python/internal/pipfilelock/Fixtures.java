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
package org.openrewrite.python.internal.pipfilelock;

import org.openrewrite.SourceFile;
import org.openrewrite.toml.TomlParser;
import org.openrewrite.toml.tree.Toml;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

final class Fixtures {

    private Fixtures() {
    }

    static String resource(String name) {
        try (InputStream is = Fixtures.class.getResourceAsStream("/pipfilelock/" + name)) {
            assertThat(is).as(name).isNotNull();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static Toml.Document pipfile(String name) {
        SourceFile sourceFile = new TomlParser().parse(resource("pipfiles/" + name)).findFirst().orElseThrow();
        assertThat(sourceFile).isInstanceOf(Toml.Document.class);
        return (Toml.Document) sourceFile;
    }
}
