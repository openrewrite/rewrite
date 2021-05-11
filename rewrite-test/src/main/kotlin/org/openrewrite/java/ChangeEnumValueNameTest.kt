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
import org.openrewrite.Issue
import org.openrewrite.java.tree.J
import org.openrewrite.java.tree.JavaType
import java.util.stream.Collectors

interface ChangeEnumValueNameTest : JavaRecipeTest {
    @Issue("https://github.com/openrewrite/rewrite/issues/462")
    @Test
    fun test(jp: JavaParser) = assertChanged(
        jp,
        recipe = object: JavaVisitor<ExecutionContext>() {
            override fun visitClassDeclaration(classDecl: J.ClassDeclaration, p: ExecutionContext): J {

                val enumValueSets = classDecl.body.statements.stream().filter { s -> s is J.EnumValueSet}
                    .map {s -> s as J.EnumValueSet}
                    .collect(Collectors.toList())

                val enumValues: MutableList<J.EnumValue> = mutableListOf()
                enumValueSets.forEach { enumValues.addAll(it.enums) }

                for (enumValue in enumValues) {
                    if (classDecl.simpleName == "TestVal") {
                        if (enumValue.name.simpleName.equals("valA")) {
                            doAfterVisit(
                                ChangeEnumValueName(
                                    JavaType.Class.build(classDecl.simpleName),
                                    "valA",
                                    "NEW_VALUE_A"
                                )
                            )
                        } else if (enumValue.name.simpleName.equals("valB")) {
                            doAfterVisit(
                                ChangeEnumValueName(
                                    JavaType.Class.build(classDecl.simpleName),
                                    "valB",
                                    "NEW_VALUE_B"
                                )
                            )
                        }
                    }
                }

                return super.visitClassDeclaration(classDecl, p)
            }
        }.toRecipe(),
        before =  """
            enum doNotChange {
                valA, valB
            }

            enum TestVal {
                valA, valB
            }

            public class Test {
                void foo() {
                    TestVal valA = TestVal.valA;
                    TestVal valB = TestVal.valB;
                    if (valB == TestVal.valB){}
                }
            }
        """,
        after = """
            enum doNotChange {
                valA, valB
            }

            enum TestVal {
                NEW_VALUE_A, NEW_VALUE_B
            }

            public class Test {
                void foo() {
                    TestVal valA = TestVal.NEW_VALUE_A;
                    TestVal valB = TestVal.NEW_VALUE_B;
                    if (valB == TestVal.NEW_VALUE_B){}
                }
            }
        """
    )
}
