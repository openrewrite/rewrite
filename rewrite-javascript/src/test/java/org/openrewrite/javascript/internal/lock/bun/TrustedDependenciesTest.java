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
package org.openrewrite.javascript.internal.lock.bun;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The order bun 1.3.10 writes {@code trustedDependencies} in, a Zig hash map's slot order, as recorded in the Bun
 * corpus ({@code trusted-deps}, {@code open-trusted-order-capacity}, {@code -capacity-small}, {@code -many}).
 */
class TrustedDependenciesTest {

    private static List<String> order(int listed, String... installed) {
        TrustedDependencies<String> found = new TrustedDependencies<>();
        found.ensureTotalCapacity(listed);
        for (String name : installed) {
            found.put(Wyhash11.stringHash(name), name);
        }
        return found.values();
    }

    @Test
    void fourListedThreeInstalled() {
        assertThat(order(4, "debug", "is-number", "ms", "ms")).containsExactly("ms", "debug", "is-number");
    }

    @Test
    void capacityFollowsTheWholeList() {
        assertThat(order(9, "camelcase", "ms", "safe-buffer")).containsExactly("ms", "safe-buffer", "camelcase");
        assertThat(order(3, "camelcase", "ms", "safe-buffer")).containsExactly("safe-buffer", "ms", "camelcase");
    }

    @Test
    void twelveListedAndInstalled() {
        assertThat(order(12, "arrify", "balanced-match", "camelcase", "has-flag", "inherits", "is-buffer", "is-number",
          "is-plain-obj", "isarray", "ms", "path-key", "safe-buffer"))
          .isEqualTo(Arrays.asList("balanced-match", "ms", "is-buffer", "arrify", "inherits", "path-key", "safe-buffer",
            "has-flag", "isarray", "camelcase", "is-number", "is-plain-obj"));
    }
}
