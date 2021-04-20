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
package org.openrewrite.java.format

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.java.style.IntelliJ

class ShiftFormatTest {

    @Test
    fun shiftLeft() {
        assertThat(ShiftFormat.shiftLeft(IntelliJ.tabsAndIndents(), "        ", 1))
            .isEqualTo("    ")
    }

    @Test
    fun shiftLeftTab() {
        assertThat(ShiftFormat.shiftLeft(IntelliJ.tabsAndIndents(), "\t\t", 1))
            .isEqualTo("	")
    }

    @Test
    fun shiftLeftTabWithRemainder() {
        assertThat(ShiftFormat.shiftLeft(IntelliJ.tabsAndIndents(), "\t\t ", 1))
            .isEqualTo("\t ")
    }

    @Test
    fun shiftRight() {
        assertThat(ShiftFormat.shiftRight(IntelliJ.tabsAndIndents(), "    ", 1))
            .isEqualTo("        ")
    }

    @Test
    fun shiftRightTab() {
        assertThat(ShiftFormat.shiftRight(IntelliJ.tabsAndIndents().withUseTabCharacter(true), "\t", 1))
            .isEqualTo("\t\t")
    }
}
