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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class NpmKeyOrderTest {

    /**
     * The expected order is the recorded output of V8 {@code [...].sort((a,b)=>a.localeCompare(b,'en'))}
     * — the exact comparator npm's {@code json-stringify-nice} uses. Pins the weight-based replica so a
     * regression surfaces here, never as a mis-sorted (wrong) lock.
     */
    @Test
    void reproducesLocaleCompareEnForPackagesKeys() {
        List<String> expected = Arrays.asList(
                "",
                "node_modules/@babel/core",
                "node_modules/@babel/types",
                "node_modules/a",
                "node_modules/a-b",
                "node_modules/a.b",
                "node_modules/a1",
                "node_modules/ab",
                "node_modules/abbrev",
                "node_modules/Foo",
                "node_modules/FOO",
                "node_modules/foo10",
                "node_modules/foo2",
                "node_modules/is_number",
                "node_modules/is-number",
                "node_modules/is-odd",
                "node_modules/is.number",
                "node_modules/isnumber",
                "node_modules/left-pad",
                "node_modules/lodash",
                "node_modules/lodash-es",
                "node_modules/lodash.merge",
                "node_modules/zebra");

        List<String> shuffled = new ArrayList<>(expected);
        Collections.shuffle(shuffled, new java.util.Random(42));
        shuffled.sort(NpmKeyOrder::compareKeys);

        assertThat(shuffled).containsExactlyElementsOf(expected);
    }

    @Test
    void preferenceKeysComeFirstInListOrder() {
        // swKeyOrder: name, version, ..., dependencies — pref keys precede non-pref, ordered by index.
        assertThat(NpmKeyOrder.compareKeys("name", "version")).isNegative();
        assertThat(NpmKeyOrder.compareKeys("version", "resolved")).isNegative();
        assertThat(NpmKeyOrder.compareKeys("dependencies", "devDependencies")).isNegative();
        assertThat(NpmKeyOrder.compareKeys("integrity", "deprecated")).isNegative();
        assertThat(NpmKeyOrder.compareKeys("deprecated", "dev")).isNegative();
        assertThat(NpmKeyOrder.compareKeys("dev", "license")).isNegative();
    }
}
