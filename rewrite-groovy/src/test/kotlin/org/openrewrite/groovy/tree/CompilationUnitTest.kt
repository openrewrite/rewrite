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
import org.openrewrite.groovy.Assertions.groovy
import org.openrewrite.test.RewriteTest

@Suppress("GroovyUnusedAssignment")
class CompilationUnitTest : RewriteTest {

    @Suppress("GrPackage")
    @Test
    fun packageDecl() = rewriteRun(
        groovy(
            """
                package org.openrewrite
                def a = 'hello'
            """
        )
    )

    @Test
    fun mixedImports() = rewriteRun(
        groovy(
            """
                def a = 'hello'
                import java.util.List
                List l = null
            """
        )
    )

    @Test
    fun shellScript() = rewriteRun(
        groovy(
            """
               #!/usr/bin/env groovy
               
               def a = 'hello'
            """
        )
    )

    @Test
    fun trailingComment() = rewriteRun(
        groovy(
            """
                // foo
            """
        )
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1974")
    @Test
    fun topLevelExpression() = rewriteRun(
        groovy(
            """
                5
            """
        )
    )
}
