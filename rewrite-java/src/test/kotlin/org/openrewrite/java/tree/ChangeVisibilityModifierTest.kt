package org.openrewrite.java.tree

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Formatting
import java.util.*

class ChangeVisibilityModifierTest {
    @Test
    fun publicToPrivate() {
        val beforeMods = listOf(
               J.Modifier.Public(UUID.randomUUID(), Formatting.EMPTY),
               J.Modifier.Static(UUID.randomUUID(), Formatting.EMPTY)
        )

        val expectedMods = listOf(
                J.Modifier.Private(UUID.randomUUID(), Formatting.EMPTY),
                J.Modifier.Static(UUID.randomUUID(), Formatting.EMPTY)
        )

        val actualMods = J.Modifier.withVisibility(beforeMods, "private")

        assertThat(actualMods.map { it.javaClass })
                .isEqualTo(expectedMods.map { it.javaClass })
    }

    @Test
    fun protectedToPrivate() {
        val beforeMods = listOf(
                J.Modifier.Protected(UUID.randomUUID(), Formatting.EMPTY),
                J.Modifier.Static(UUID.randomUUID(), Formatting.EMPTY)
        )

        val expectedMods = listOf(
                J.Modifier.Private(UUID.randomUUID(), Formatting.EMPTY),
                J.Modifier.Static(UUID.randomUUID(), Formatting.EMPTY)
        )

        val actualMods = J.Modifier.withVisibility(beforeMods, "private")

        assertThat(actualMods.map { it.javaClass })
                .isEqualTo(expectedMods.map { it.javaClass })
    }

    @Test
    fun packagePrivateToPrivate() {
        val beforeMods = listOf(
                J.Modifier.Static(UUID.randomUUID(), Formatting.EMPTY)
        )

        val expectedMods = listOf(
                J.Modifier.Private(UUID.randomUUID(), Formatting.EMPTY),
                J.Modifier.Static(UUID.randomUUID(), Formatting.EMPTY)
        )

        val actualMods = J.Modifier.withVisibility(beforeMods, "private")

        assertThat(actualMods.map { it.javaClass })
                .isEqualTo(expectedMods.map { it.javaClass })
    }

    @Test
    fun publicToPackagePrivate() {
        val beforeMods = listOf(
                J.Modifier.Public(UUID.randomUUID(), Formatting.EMPTY),
                J.Modifier.Static(UUID.randomUUID(), Formatting.EMPTY)
        )

        val expectedMods = listOf(
                J.Modifier.Static(UUID.randomUUID(), Formatting.EMPTY)
        )

        val actualMods = J.Modifier.withVisibility(beforeMods, "package")

        assertThat(actualMods.map { it.javaClass })
                .isEqualTo(expectedMods.map { it.javaClass })
    }
}
