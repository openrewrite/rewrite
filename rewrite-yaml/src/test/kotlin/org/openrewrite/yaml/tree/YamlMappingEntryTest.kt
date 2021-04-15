package org.openrewrite.yaml.tree

import org.junit.jupiter.api.Test
import org.openrewrite.Recipe
import org.openrewrite.yaml.YamlRecipeTest

class YamlMappingEntryTest : YamlRecipeTest {

    @Test
    fun literals() = assertUnchanged(
        recipe = object: Recipe() {
            override fun getDisplayName(): String = "Do nothing"
        },
        before = """
          data:
            prometheus.yml: |-
              global:
                scrape_interval: 10s
                scrape_timeout: 9s
                evaluation_interval: 10s
        """
    )
}
