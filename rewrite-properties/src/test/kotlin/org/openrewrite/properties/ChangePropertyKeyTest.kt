package org.openrewrite.properties

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.properties.tree.Properties

class ChangePropertyKeyTest : PropertiesParser() {
    @Test
    fun changeKey() {
        val props = parse("""
            management.metrics.binders.files.enabled=true
        """.trimIndent())

        val fixed = props.refactor()
                .visit(ChangePropertyKey().apply {
                    setKey("management.metrics.binders.files.enabled")
                    setToKey("management.metrics.enable.process.files")
                })
                .fix().fixed

        assertThat(fixed.content.map { it as Properties.Entry }.map { it.key })
                .hasSize(1).containsExactly("management.metrics.enable.process.files")
    }
}
