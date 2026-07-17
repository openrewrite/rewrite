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
package org.openrewrite.python.internal.pdmlock;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fixtures under {@code src/test/resources/pdmlock/} were generated with PDM 2.28.0 against live
 * pypi.org; see the README.md there for provenance. Contents are byte-exact reference outputs.
 */
final class PdmLockFixtures {

    static final String[] ALL_LOCKS = {
      "a-minimal/pdm.lock",
      "b-graph/pdm.lock",
      "c-extras/pdm.lock",
      "d-groups/pdm.lock",
      "e-markers/pdm.lock",
      "f-git/pdm.lock",
      "g-upgrade/pdm.lock.before",
      "g-upgrade/pdm.lock.after"
    };

    private PdmLockFixtures() {
    }

    static String resource(String name) {
        try (InputStream is = PdmLockFixtures.class.getResourceAsStream("/pdmlock/" + name)) {
            assertThat(is).as(name).isNotNull();
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
