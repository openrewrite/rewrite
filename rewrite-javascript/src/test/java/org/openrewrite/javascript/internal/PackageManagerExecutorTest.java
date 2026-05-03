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

class PackageManagerExecutorTest {

    @Test
    void exposesAllFiveExecutors() {
        assertThat(PackageManagerExecutor.NPM.getName()).isEqualTo("npm");
        assertThat(PackageManagerExecutor.YARN.getName()).isEqualTo("yarn");
        assertThat(PackageManagerExecutor.PNPM.getName()).isEqualTo("pnpm");
        assertThat(PackageManagerExecutor.BUN.getName()).isEqualTo("bun");
    }

    @Test
    void findReturnsNullForMissingExecutable() {
        // Construct one with a name that won't exist anywhere on PATH.
        PackageManagerExecutor missing =
                PackageManagerExecutor.forTesting("definitely-not-a-real-binary-2026", 30);
        assertThat(missing.find()).isNull();
    }
}
