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

        val fixed = x.refactor().visit(ChangeTagValue.Scoped(x.root.content[0] as Xml.Tag, "2.0"))
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

        val fixed = x.refactor().visit(ChangeTagValue.Scoped(x.root.content[0] as Xml.Tag, "3.0"))
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
