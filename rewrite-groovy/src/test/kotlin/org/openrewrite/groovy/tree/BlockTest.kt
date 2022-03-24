/*
 * Copyright 2021 the original author or authors.
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
package org.openrewrite.groovy.tree

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class BlockTest: GroovyTreeTest {

    @Test
    fun block() = assertParsePrintAndProcess(
        """
            implementation ('org.thymeleaf:thymeleaf-spring4:3.0.6.RELEASE') {
                force = true;
            }
        """.trimIndent()
    )

    @Disabled
    @Test
    fun from() = assertParsePrintAndProcess(
        """
            tasks.named('jar') {
                metaInf {
                    /* this version of from causes the file to be un-parsable */
                    from("${'$'}projectDir/licenses/LICENSE-JARJAR")
                    from("${'$'}projectDir/licenses") {
                        into('licenses')
                        include('asm-license.txt')
                        include('antlr4-license.txt')
                    }
                    from("${'$'}projectDir/notices/NOTICE-JARJAR")
                    rename '^([A-Z]+)-([^.]*)', '${'$'}1'
                }
            }
        """.trimIndent()
    )
}
