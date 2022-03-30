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

import org.junit.jupiter.api.Test
import org.openrewrite.Issue

class AssignmentTest : GroovyTreeTest {

    @Test
    fun concat() = assertParsePrintAndProcess(
        """
            android {
                // specify the artifactId as module-name for kotlin
                kotlinOptions.freeCompilerArgs += ["-module-name", POM_ARTIFACT_ID]
            }
        """.trimIndent()
    )

    @Test
    fun assignment() = assertParsePrintAndProcess(
        """
            String s;
            s = "foo";
        """
    )

    @Test
    fun unaryMinus() = assertParsePrintAndProcess(
        """
            def i = -1
            def l = -1L
            def f = -1.0f
            def d = -1.0d
        """
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1522")
    @Test
    fun unaryPlus() = assertParsePrintAndProcess(
        """
            int k = +10
        """
    )
}
