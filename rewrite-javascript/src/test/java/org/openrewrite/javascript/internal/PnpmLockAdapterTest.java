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
package org.openrewrite.javascript.internal;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PnpmLockAdapterTest {

    @Test
    void convertsTopLevelDependency() {
        String pnpm = "lockfileVersion: '6.0'\n" +
                "\n" +
                "dependencies:\n" +
                "  lodash:\n" +
                "    specifier: ^4.17.21\n" +
                "    version: 4.17.21\n" +
                "\n" +
                "packages:\n" +
                "\n" +
                "  /lodash@4.17.21:\n" +
                "    resolution: {integrity: sha512-x}\n" +
                "    dev: false\n";
        String npm = PnpmLockAdapter.toNpmV3(pnpm);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll()).hasSize(1);
        assertThat(result.getAll().get(0).getName()).isEqualTo("lodash");
        assertThat(result.getAll().get(0).getVersion()).isEqualTo("4.17.21");
        assertThat(result.getTopLevel()).containsKey("lodash");
    }
}
