package org.openrewrite.yaml

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe

class DeletePropertyKeyTest : YamlRecipeTest {
    override val recipe: Recipe
        get() = DeleteProperty(
            "management.metrics.binders.files.enabled",
            true
        )

    @Test
    fun singleEntry() = assertChanged(
        before = "management.metrics.binders.files.enabled: true",
        after = ""
    )

    @Test
    fun downDeeper() = assertChanged(
        before = """
          management.metrics:
            enabled: true
            binders.files.enabled: true
          server.port: 8080
        """,
        after = """
          management.metrics.enabled: true
          server.port: 8080
        """
    )
}
