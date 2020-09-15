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
import org.openrewrite.RefactorVisitorTestForParser
import org.openrewrite.xml.tree.Xml

class AddToTagTest : RefactorVisitorTestForParser<Xml.Document> {
    override val parser: XmlParser = XmlParser()

    @Test
    fun addElement() = assertRefactored(
            visitorsMapped = listOf { x ->
                AddToTag.Scoped(x.root, """<bean id="myBean2"/>""")
            },
            before = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans >
                    <bean id="myBean"/>
                </beans>
            """,
            after = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans >
                    <bean id="myBean"/>
                    <bean id="myBean2"/>
                </beans>
            """
    )

    @Test
    fun addElementToSlashClosedTag() = assertRefactored(
            visitorsMapped = listOf { x ->
                AddToTag.Scoped(x.root.content[0] as Xml.Tag,
                        """<property name="myprop" ref="collaborator"/>""")
            },
            before = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans >
                    <bean id="myBean"/>
                </beans>
            """,
            after = """
                <?xml version="1.0" encoding="UTF-8"?>
                <beans >
                    <bean id="myBean">
                        <property name="myprop" ref="collaborator"/>
                    </bean>
                </beans>
            """
    )
}
