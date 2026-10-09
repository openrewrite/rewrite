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

class ExpectTest {

    @Test
    void noLineIsExact() {
        assertThat(Expect.of(null).isExact()).isTrue();
        assertThat(Expect.of("  \n").isExact()).isTrue();
        assertThat(Expect.of(null).words()).isEmpty();
    }

    @Test
    void theFirstWordIsTheKind() {
        Expect expect = Expect.of("cannot-know the manifest pnpm reads from packages/c/package.yaml\n");
        assertThat(expect.getKind()).isEqualTo("cannot-know");
        assertThat(expect.getRest()).isEqualTo("the manifest pnpm reads from packages/c/package.yaml");
        assertThat(expect.words()).startsWith("cannot-know", "the");
    }

    @Test
    void notReproducedAndPmFailsCarryTheirReason() {
        Expect skipped = Expect.of("not-reproduced the before lock was made by hand to differ from its tarball");
        assertThat(skipped.getKind()).isEqualTo(Expect.NOT_REPRODUCED);
        assertThat(skipped.getRest()).isEqualTo("the before lock was made by hand to differ from its tarball");
        assertThat(Expect.of("pm-fails TypeError: r.startsWith is not a function").getKind())
          .isEqualTo(Expect.PM_FAILS);
    }
}
