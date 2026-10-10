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
package org.openrewrite.javascript.internal.lock;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * strip-bom's own test/test.js, the same two cases and fixtures in 3.0.0 and 5.0.0.
 */
class StripBomTest {

    @Test
    void stripsBomFromString() {
        assertThat(StripBom.stripBom("\uFEFFUnicorn\n")).isEqualTo("Unicorn\n");
    }

    @Test
    void stripsBomFromTheBeginningOfAFile() {
        String fixture = "Unicorn \uFEFFUnicorn\n";
        assertThat(StripBom.stripBom(fixture)).isEqualTo(fixture);
    }
}
