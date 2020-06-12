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
