package org.openrewrite.xml.refactor

import org.junit.jupiter.api.Test
import org.openrewrite.xml.XmlParser
import org.openrewrite.xml.assertRefactored
import org.openrewrite.xml.tree.Xml

class ChangeTagValueTest : XmlParser() {
    @Test
    fun changeTagValue() {
        val x = parse("""
            <?xml version="1.0" encoding="UTF-8"?>
            <dependency>
                <version/>
            </dependency>
        """.trimIndent())

        val fixed = x.refactor().visit(ChangeTagValue(x.root.content[0] as Xml.Tag, "2.0"))
                .fix().fixed

        assertRefactored(fixed, """
            <?xml version="1.0" encoding="UTF-8"?>
            <dependency>
                <version>2.0</version>
            </dependency>
        """)
    }

    @Test
    fun preserveOriginalFormatting() {
        val x = parse("""
            <?xml version="1.0" encoding="UTF-8"?>
            <dependency>
                <version>
                    2.0
                </version>
            </dependency>
        """.trimIndent())

        val fixed = x.refactor().visit(ChangeTagValue(x.root.content[0] as Xml.Tag, "3.0"))
                .fix().fixed

        assertRefactored(fixed, """
            <?xml version="1.0" encoding="UTF-8"?>
            <dependency>
                <version>
                    3.0
                </version>
            </dependency>
        """)
    }
}
