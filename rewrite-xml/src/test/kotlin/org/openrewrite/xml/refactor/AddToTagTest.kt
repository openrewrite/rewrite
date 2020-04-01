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
