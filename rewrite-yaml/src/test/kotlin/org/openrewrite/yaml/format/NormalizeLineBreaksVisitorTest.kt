/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.yaml.format

import org.junit.jupiter.api.Test
import org.openrewrite.style.GeneralFormatStyle
import org.openrewrite.yaml.YamlRecipeTest

class NormalizeLineBreaksVisitorTest : YamlRecipeTest {
    companion object {
        const val windows = "" +
                "root:\r\n" +
                "  - a: 0\r\n" +
                "    b: 0"
        const val linux = "" +
                "root:\n" +
                "  - a: 0\n" +
                "    b: 0"
        const val mixedLinux = "" +
                "root:\n" +
                "  - a: 0\n" +
                "  - b: 0\r\n" +
                "  - c: 0"
        const val formattedLinux = "" +
                "root:\n" +
                "  - a: 0\n" +
                "  - b: 0\n" +
                "  - c: 0"
        const val mixedWindows = "" +
                "root:\r\n" +
                "  - a: 0\n" +
                "  - b: 0\r\n" +
                "  - c: 0"
        const val formattedWindows = "" +
                "root:\r\n" +
                "  - a: 0\r\n" +
                "  - b: 0\r\n" +
                "  - c: 0"
    }

    @Test
    fun windowsToLinux() = assertChanged(
        recipe = toRecipe { NormalizeLineBreaksVisitor(GeneralFormatStyle(false), null) },
        before = windows,
        after = linux
    )

    @Test
    fun linuxToWindows() = assertChanged(
        recipe = toRecipe { NormalizeLineBreaksVisitor(GeneralFormatStyle(true), null) },
        before = linux,
        after = windows
    )

    @Test
    fun autoDetectLinux() = assertChanged(
        recipe = toRecipe { AutoFormatVisitor(null) },
        before = mixedLinux,
        after = formattedLinux
    )

    @Test
    fun autoDetectWindows() = assertChanged(
        recipe = toRecipe { AutoFormatVisitor(null) },
        before = mixedWindows,
        after = formattedWindows
    )
}
