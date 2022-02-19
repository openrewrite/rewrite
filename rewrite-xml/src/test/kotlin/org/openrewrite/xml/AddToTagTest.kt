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
@file:Suppress("CheckTagEmptyBody")

package org.openrewrite.xml

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.Issue
import org.openrewrite.xml.tree.Xml

class AddToTagTest : XmlRecipeTest {

    @Test
    fun addElement() = assertChanged(
        recipe = toRecipe {
            object : XmlVisitor<ExecutionContext>() {
                override fun visitDocument(x: Xml.Document, p: ExecutionContext): Xml {
                    val bean2Tag =
                        x.root.children.find { it.attributes.find { attr -> attr.key.name == "id" && attr.value.value == "myBean2" } != null }
                    if (bean2Tag == null) {
                        doAfterVisit(AddToTagVisitor(x.root, Xml.Tag.build("""<bean id="myBean2"/>""")))
                    }
                    return super.visitDocument(x, p)
                }
            }
        },
        before = """
            <beans>
                <bean id="myBean"/>
            </beans>
        """,
        after = """
            <beans>
                <bean id="myBean"/>
                <bean id="myBean2"/>
            </beans>
        """,
        cycles = 2
    )

    @Test
    fun addElementToSlashClosedTag() = assertChanged(
        recipe = toRecipe {
            object : XmlVisitor<ExecutionContext>() {
                override fun visitDocument(x: Xml.Document, p: ExecutionContext): Xml {
                    if (x.root.children.first().children.size == 0) {
                        doAfterVisit(
                            AddToTagVisitor(
                                x.root.content[0] as Xml.Tag,
                                Xml.Tag.build("""<property name="myprop" ref="collaborator"/>""")
                            )
                        )
                    }
                    return super.visitDocument(x, p)
                }
            }
        },
        before = """
            <beans >
                <bean id="myBean" />
            </beans>
        """,
        after = """
            <beans >
                <bean id="myBean">
                    <property name="myprop" ref="collaborator"/>
                </bean>
            </beans>
        """,
        cycles = 2
    )

    @Test
    fun addElementToEmptyTagOnSameLine() = assertChanged(
        recipe = toRecipe {
            object : XmlVisitor<ExecutionContext>() {
                override fun visitDocument(x: Xml.Document, p: ExecutionContext): Xml {
                    if (x.root.children.isEmpty()) {
                        doAfterVisit(AddToTagVisitor(x.root, Xml.Tag.build("""<bean id="myBean"/>""")))
                    }
                    return super.visitDocument(x, p)
                }
            }
        },
        before = """
            <beans></beans>
        """,
        after = """
            <beans>
                <bean id="myBean"/>
            </beans>
        """,
        cycles = 2
    )

    @Test
    fun addElementInOrder() = assertChanged(
        recipe = toRecipe {
            object : XmlVisitor<ExecutionContext>() {
                override fun visitDocument(x: Xml.Document, p: ExecutionContext): Xml {
                    if (x.root.children.find { it.name == "apple" } == null) {
                        doAfterVisit(AddToTagVisitor(
                            x.root, Xml.Tag.build("""<apple/>"""),
                            TagNameComparator()
                        ))
                    }
                    return super.visitDocument(x, p)
                }
            }
        },
        before = """
            <beans >
                <banana/>
            </beans>
        """,
        after = """
            <beans >
                <apple/>
                <banana/>
            </beans>
        """,
        cycles = 2
    )

    @Issue("https://github.com/openrewrite/rewrite/issues/1392")
    @Test
    fun preserveNonTagContent() = assertChanged(
        recipe = toRecipe {
            object : XmlVisitor<ExecutionContext>() {
                override fun visitDocument(x: Xml.Document, p: ExecutionContext): Xml {
                    if (x.root.children.find { it.name == "apple" } == null) {
                        doAfterVisit(AddToTagVisitor(
                            x.root, Xml.Tag.build("""<apple/>"""),
                            TagNameComparator()
                        ))
                    }
                    return super.visitDocument(x, p)
                }
            }
        },
        before = """
            <beans>
                <!-- comment -->
                <?processing instruction?>
                <banana/>
            </beans>
        """,
        after = """
            <beans>
                <!-- comment -->
                <?processing instruction?>
                <apple/>
                <banana/>
            </beans>
        """,
        cycles = 2
    )
}
