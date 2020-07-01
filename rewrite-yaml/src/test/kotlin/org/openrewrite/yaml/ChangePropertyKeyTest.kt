package org.openrewrite.yaml

import org.junit.jupiter.api.Test

class ChangePropertyKeyTest : YamlParser() {
    private val changeProp = ChangePropertyKey().apply {
        setProperty("management.metrics.binders.files.enabled")
        setToProperty("management.metrics.enable.process.files")
    }

    @Test
    fun singleEntry() {
        val y = parse("""
            management.metrics.binders.files.enabled: true
        """.trimIndent())

        val fixed = y.refactor().visit(changeProp).fix().fixed

        assertRefactored(fixed, """
            management.metrics.enable.process.files: true
        """.trimIndent())
    }

    @Test
    fun nestedEntry() {
        val y = parse("""
            management.metrics:
                binders:
                    jvm.enabled: true
                    files.enabled: true
        """.trimIndent())

        val fixed = y.refactor().visit(changeProp).fix().fixed

        assertRefactored(fixed, """
            management.metrics:
                binders:
                    jvm.enabled: true
                enable.process.files: true
        """.trimIndent())
    }

    @Test
    fun nestedEntryEmptyPartialPathRemoved() {
        val y = parse("""
            management.metrics:
                binders:
                    files.enabled: true
        """.trimIndent())

        val fixed = y.refactor().visit(changeProp).fix().fixed

        assertRefactored(fixed, """
            management.metrics:
                enable.process.files: true
        """.trimIndent())
    }
}
