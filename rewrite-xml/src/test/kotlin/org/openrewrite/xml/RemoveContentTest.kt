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

class RemoveContentTest : RefactorVisitorTestForParser<Xml.Document> {
    override val parser: XmlParser = XmlParser()

    @Test
    fun removeContent() = assertRefactored(
            visitorsMapped = listOf { x ->
                RemoveContent.Scoped(x.root.content[1] as Xml.Tag)
            },
            before = """
                <?xml version="1.0" encoding="UTF-8"?>
                <dependency>
                    <groupId>group</groupId>
                    <version/>
                </dependency>
            """,
            after = """
                <?xml version="1.0" encoding="UTF-8"?>
                <dependency>
                    <groupId>group</groupId>
                </dependency>
            """
    )
}
