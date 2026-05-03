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

class BunLockAdapterTest {

    @Test
    void convertsTopLevelEntry() {
        String bun = "{\n" +
                "  \"packages\": {\n" +
                "    \"lodash\": [\"lodash@4.17.21\", \"\", { }, \"sha512-x\"]\n" +
                "  }\n" +
                "}";
        String npm = BunLockAdapter.toNpmV3(bun);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll()).hasSize(1);
        assertThat(result.getAll().get(0).getName()).isEqualTo("lodash");
        assertThat(result.getAll().get(0).getVersion()).isEqualTo("4.17.21");
        assertThat(result.getTopLevel()).containsKey("lodash");
    }

    @Test
    void convertsNestedEntry() {
        String bun = "{\n" +
                "  \"packages\": {\n" +
                "    \"is-even\": [\"is-even@1.0.0\", \"\", { }, \"sha512-a\"],\n" +
                "    \"is-even/is-odd\": [\"is-odd@0.1.2\", \"\", { }, \"sha512-b\"]\n" +
                "  }\n" +
                "}";
        String npm = BunLockAdapter.toNpmV3(bun);
        LockFileParser.ParseResult result = LockFileParser.parse(npm);

        assertThat(result.getAll())
                .extracting(d -> d.getName() + "@" + d.getVersion())
                .containsExactlyInAnyOrder("is-even@1.0.0", "is-odd@0.1.2");
        // only is-even is top-level
        assertThat(result.getTopLevel().keySet()).containsExactly("is-even");
    }
}
