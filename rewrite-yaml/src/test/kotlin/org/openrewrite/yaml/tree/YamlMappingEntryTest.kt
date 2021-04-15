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
