package org.openrewrite.properties

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.openrewrite.properties.tree.Properties

class ChangePropertyValueTest : PropertiesParser() {
    @Test
    fun changeValue() {
        val props = parse("""
            management.metrics.binders.files.enabled=true
        """.trimIndent())

        val fixed = props.refactor()
                .visit(ChangePropertyValue().apply {
                    setKey("management.metrics.binders.files.enabled")
                    setToValue("false")
                })
                .fix().fixed

        Assertions.assertThat(fixed.content.map { it as Properties.Entry }.map { it.value })
                .hasSize(1).containsExactly("false")
    }
}
