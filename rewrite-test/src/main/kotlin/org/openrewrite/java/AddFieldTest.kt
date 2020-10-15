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
import org.openrewrite.Formatting.EMPTY
import org.openrewrite.RefactorVisitorTest
import org.openrewrite.Tree.randomId
import org.openrewrite.java.tree.J
import java.util.Collections.singletonList

interface AddFieldTest : RefactorVisitorTest {
    companion object {
        val private: List<J.Modifier> = singletonList(J.Modifier.Private(randomId(), EMPTY) as J.Modifier)
    }

    @Test
    fun addFieldDefaultIndent(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf { a ->
                AddField.Scoped(a.classes[0], private, "java.util.List", "list", "new ArrayList<>()")
            },
            before = """
                class A {
                }
            """,
            after = """
                import java.util.List;
                
                class A {
                    private List list = new ArrayList<>();
                }
            """
    )

    @Test
    fun addFieldMatchSpaces(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf { a ->
                AddField.Scoped(a.classes[0], private, "java.util.List", "list", null)
            },
            before = """
                import java.util.List;
                
                class A {
                  List l;
                }
            """,
            after = """
                import java.util.List;
                
                class A {
                  private List list;
                  List l;
                }
            """
    )

    @Test
    fun addFieldMatchTabs(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf { a ->
                AddField.Scoped(a.classes[0], private, "java.util.List", "list", null)
            },
            before = """
                import java.util.List;
                
                class A {
                           List l;
                }
            """,
            after = """
            import java.util.List;
            
            class A {
                       private List list;
                       List l;
            }
        """
    )

    @Test
    fun addPrimitiveField(jp: JavaParser) = assertRefactored(
            jp,
            visitorsMapped = listOf { a->
                AddField.Scoped(a.classes[0], private, "int", "count", "5");
            },
            before = """
                class A {
                
                }
            """,
            after = """
                class A {
                    private int count = 5;
                
                }
            """
    )
}
