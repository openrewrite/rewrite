/*
 * Copyright 2020 the original authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.openrewrite.xml.refactor

import org.junit.jupiter.api.Test
import org.openrewrite.xml.XmlParser
import org.openrewrite.xml.assertRefactored
import org.openrewrite.xml.tree.Xml

class AddToTagTest: XmlParser() {
    @Test
    fun addElement() {
        val x = parse("""
            <?xml version="1.0" encoding="UTF-8"?>
            <beans >
                <bean id="myBean"/>
            </beans>
        """.trimIndent())

        val fixed = x.refactor().visit(AddToTag(x.root, """<bean id="myBean2"/>""")).fix().fixed

        assertRefactored(fixed, """
            <?xml version="1.0" encoding="UTF-8"?>
            <beans >
                <bean id="myBean"/>
                <bean id="myBean2"/>
            </beans>
        """.trimIndent())
    }

    @Test
    fun addElementToSlashClosedTag() {
        val x = parse("""
            <?xml version="1.0" encoding="UTF-8"?>
            <beans >
                <bean id="myBean"/>
            </beans>
        """.trimIndent())

        val fixed = x.refactor().visit(AddToTag(x.root.content[0] as Xml.Tag,
                """<property name="myprop" ref="collaborator"/>""")).fix().fixed

        assertRefactored(fixed, """
            <?xml version="1.0" encoding="UTF-8"?>
            <beans >
                <bean id="myBean">
                    <property name="myprop" ref="collaborator"/>
                </bean>
            </beans>
        """.trimIndent())
    }
}
