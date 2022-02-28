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
package org.openrewrite.xml

import org.junit.jupiter.api.Test
import org.openrewrite.ExecutionContext
import org.openrewrite.xml.tree.Xml

class ChangeXMLAttributeTest: XmlRecipeTest {

    @Test
    fun alterAttribute() = assertChanged(
            recipe = toRecipe {
                object : XmlVisitor<ExecutionContext>() {
                    override fun visitDocument(x: Xml.Document, p: ExecutionContext): Xml {

                            doAfterVisit(ChangeXMLAttributeVisitor("bean","id","myBean.subpackage","myBean2.subpackage"))

                        return super.visitDocument(x, p)
                    }
                }
            },
            before = """
            <beans>
                <bean id='myBean.subpackage.subpackage2'/>
            </beans>
        """,
            after = """
            <beans>
                <bean id='myBean2.subpackage.subpackage2'/>
            </beans>
        """,
            cycles = 2
    )
}