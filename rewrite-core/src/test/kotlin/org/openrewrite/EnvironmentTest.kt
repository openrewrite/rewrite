package org.openrewrite

import io.micrometer.core.instrument.Tags
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.config.CompositeSourceVisitor
import org.openrewrite.text.PlainText

class EnvironmentTest {
    @Test
    fun profileExtends() {
        val parent = Profile().apply {
            name = "parent"
            setDefine(
                    mapOf(
                            "custom.Declarative1" to
                                    listOf(mapOf("org.openrewrite.text.ChangeText" to mapOf("toText" to "Hello Jon"))),
                            "custom.Declarative2" to
                                    listOf(mapOf("text.ChangeText" to mapOf("toText" to "Hello Jonathan!")))
                    )
            )
        }

        val child = Profile().apply {
            name = "child"
            extend = setOf("parent")
            setInclude(setOf("custom.*"))
        }

        val env = Environment.builder()
                .loadProfile { listOf(parent, child) }
                .scan("org.openrewrite.text")

        assertThat(env.getProfile("child")
                .getVisitorsForSourceType(PlainText::class.java)
                .map { it as CompositeSourceVisitor }
                .map { it.tags }
        ).containsExactlyInAnyOrder(Tags.of("name", "custom.Declarative1"), Tags.of("name", "custom.Declarative2"))
    }
}
