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
package org.openrewrite.yaml

import org.junit.jupiter.api.Test
import org.openrewrite.RecipeTest

class CoalescePropertiesTest : RecipeTest {
    override val parser = YamlParser()

    @Test
    fun fold() = assertChanged(
            recipe = CoalesceProperties().apply { },
            before = """
                management:
                    metrics:
                        enable.process.files: true
                    endpoint:
                        health:
                            show-components: always
                            show-details: always
            """,
            after = """
                management:
                    metrics.enable.process.files: true
                    endpoint.health:
                        show-components: always
                        show-details: always
            """
    )

//    @Test
//    fun group() {
//        val y = parse("""
//            management.metrics.enable.process.files: true
//            management.metrics.enable.jvm: true
//        """.trimIndent())
//
//        val fixed = y.refactor().visit(CoalesceProperties()).fix().fixed
//
//        assertRefactored(fixed, """
//            management.metrics.enable:
//                process.files: true
//                jvm: true
//        """.trimIndent())
//    }
}
