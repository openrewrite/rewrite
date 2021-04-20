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
