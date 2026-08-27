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
package org.openrewrite.python.internal.poetrylock;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fixtures under {@code src/test/resources/poetrylock/} were generated with Poetry 2.4.1 against
 * live pypi.org; see the README.md there for provenance. Contents are byte-exact reference outputs.
 */
final class PoetryLockFixtures {

    static final String[] ALL_LOCKS = {
      "a-minimal/poetry.lock",
      "b-graph/poetry.lock",
      "c-modern/poetry.lock",
      "d-extras/poetry.lock",
      "e-groups/poetry.lock",
      "f-markers/poetry.lock",
      "g-git/poetry.lock",
      "i-upgrade/poetry.lock.before",
      "i-upgrade/poetry.lock.after"
    };

    private PoetryLockFixtures() {
    }

    static String resource(String name) {
        try (InputStream is = PoetryLockFixtures.class.getResourceAsStream("/poetrylock/" + name)) {
            assertThat(is).as(name).isNotNull();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
