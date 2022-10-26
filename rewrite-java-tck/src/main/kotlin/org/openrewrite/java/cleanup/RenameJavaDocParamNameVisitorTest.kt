/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.cleanup

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.java.Assertions.java
import org.openrewrite.java.JavaIsoVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest
import org.openrewrite.test.RewriteTest.toRecipe

@Suppress("JavadocDeclaration", "JavadocReference")
interface RenameJavaDocParamNameVisitorTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(toRecipe {
            object : JavaIsoVisitor<ExecutionContext>() {
                override fun visitMethodDeclaration(
                    method: J.MethodDeclaration,
                    p: ExecutionContext
                ): J.MethodDeclaration {
                    var md = method
                    if ("method" == md.name.simpleName && md.parameters.stream().anyMatch {
                            it is J.VariableDeclarations && it.variables.stream().anyMatch { it2 ->
                                "oldName" == it2.name.simpleName
                            }
                    }) {
                        md = RenameJavaDocParamNameVisitor<ExecutionContext>(
                            md,
                            "oldName",
                            "newName"
                        ).visitMethodDeclaration(md, p)
                    }
                    return super.visitMethodDeclaration(md, p)
                }

                override fun visitVariable(
                    variable: J.VariableDeclarations.NamedVariable,
                    p: ExecutionContext
                ): J.VariableDeclarations.NamedVariable {
                    var v = super.visitVariable(variable, p)
                    if ("oldName" == variable.simpleName) {
                        v = v.withName(v.name.withSimpleName("newName"))
                    }
                    return v
                }
            }
        })
    }

    @Test
    fun noJavaDocParamMatch() =
      rewriteRun(
        java(
          """
            class Test {
                /**
                 * @param noMatch
                 */
                void method(String oldName) {
                }
            }
          """,
          """
            class Test {
                /**
                 * @param noMatch
                 */
                void method(String newName) {
                }
            }
          """
        )
      )

    @Test
    fun renameParamName() =
      rewriteRun(
        java(
          """
            class Test {
                /**
                 * @param oldName
                 */
                void method(String oldName) {
                }
            }
          """,
          """
            class Test {
                /**
                 * @param newName
                 */
                void method(String newName) {
                }
            }
          """
        )
      )
}
