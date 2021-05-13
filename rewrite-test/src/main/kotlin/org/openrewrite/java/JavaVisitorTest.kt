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

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.java.tree.J

interface JavaVisitorTest : JavaRecipeTest {

    @Test
    fun javaVisitorHandlesPaddedWithNullElem() = assertChanged(
        recipe = object : JavaIsoVisitor<ExecutionContext>() {
            override fun visitMethodInvocation(method: J.MethodInvocation, p: ExecutionContext): J.MethodInvocation? {
                val mi = super.visitMethodInvocation(method, p)
                if ("removeMethod".equals(mi.simpleName)) {
                    return null
                }
                return mi
            }
        }.toRecipe().doNext(
            object : JavaIsoVisitor<ExecutionContext>() {
                override fun visitMethodDeclaration(method: J.MethodDeclaration, p: ExecutionContext): J.MethodDeclaration {
                    var md = super.visitMethodDeclaration(method, p)
                    if (md.simpleName.equals("allTheThings")) {
                        md = md.withTemplate(template("throws Exception").build(), md.coordinates.replaceThrows())
                    }
                    return md
                }
            }.toRecipe()
        ),
        before = """
            class A {
                void allTheThings() {
                    doSomething();
                    removeMethod();
                }
                void doSomething() {}
                void removeMethod() {}
            }
        """,
        after = """
            class A {
                void allTheThings() throws Exception {
                    doSomething();
                }
                void doSomething() {}
                void removeMethod() {}
            }
        """,
        cycles = 2,
        expectedCyclesToComplete = 2
    )
}