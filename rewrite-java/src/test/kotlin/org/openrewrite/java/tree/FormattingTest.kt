/*
 * Copyright 2020 the original author or authors.
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
package org.openrewrite.java.tree

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Formatting.format

class FormattingTest {

    @Test
    fun flyweights() {
        val f1 = format("")
        val f2 = format("")

        assertThat(f1).isSameAs(f2)
    }

    @Test
    fun minimumBlankLines() {
        assertThat(format("\n").withMinimumBlankLines(1).prefix).isEqualTo("\n\n")
        assertThat(format("\n\n").withMinimumBlankLines(1).prefix).isEqualTo("\n\n")
        assertThat(format("\n\n\n").withMinimumBlankLines(1).prefix).isEqualTo("\n\n\n")
    }

    @Test
    fun maximumBlankLines() {
        assertThat(format("  \n").withMaximumBlankLines(1).prefix).isEqualTo("  \n")
        assertThat(format("  \n\n").withMaximumBlankLines(1).prefix).isEqualTo("  \n\n")
        assertThat(format("  \n\n\n").withMaximumBlankLines(1).prefix).isEqualTo("  \n\n")
    }
}
