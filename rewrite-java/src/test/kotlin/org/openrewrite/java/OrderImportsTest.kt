package org.openrewrite.java

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml

class OrderImportsTest {
    @Test
    fun fromYaml() {
        val yaml = Yaml()
        val orderImportsConfig: Map<String, Object> = yaml.load("""
            removeUnused: true
            layout:
                classCountToUseStarImport: 5
                nameCountToUseStarImport: 3
                blocks:
                    - import all other imports
                    - <blank line>
                    - import javax.*
                    - import java.*
                    - <blank line>
                    - import static all other imports
        """.trimIndent())

        val orderImports: OrderImports = OrderImports()
        ObjectMapper().updateValue(orderImports, orderImportsConfig)

        assertThat(orderImports.layout.blocks[0]).isInstanceOf(OrderImports.Layout.Block.AllOthers::class.java)
        assertThat(orderImports.layout.blocks[1]).isInstanceOf(OrderImports.Layout.Block.BlankLines::class.java)
        assertThat(orderImports.layout.blocks[2]).isInstanceOf(OrderImports.Layout.Block.ImportPackage::class.java)
    }
}
