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
