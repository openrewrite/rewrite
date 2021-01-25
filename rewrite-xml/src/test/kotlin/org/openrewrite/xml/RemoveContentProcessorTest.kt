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

class RemoveContentTest : XmlVisitorTest() {

    @Test
    fun removeContent() = assertChanged(
            visitorMapped = { x ->
                RemoveContentVisitor(x.root.content[1] as Xml.Tag, false)
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

    @Test
    fun removeAncestorsThatBecomeEmpty() = assertChanged(
            visitorMapped = { x ->
                val groupId = ((x.root.content[1] as Xml.Tag).content[0] as Xml.Tag).content[0] as Xml.Tag
                RemoveContentVisitor(groupId, true)
            },
            before = """
                <project>
                    <name>my.company</name>
                    <dependencyManagement>
                        <dependencies>
                            <groupId>group</groupId>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """,
            after = """
                <project>
                    <name>my.company</name>
                </project>
            """
    )

    @Test
    fun rootChangedToEmptyTagIfLastRemainingTag() = assertChanged(
            visitorMapped = { x ->
                val groupId = ((x.root.content[0] as Xml.Tag).content[0] as Xml.Tag).content[0] as Xml.Tag
                RemoveContentVisitor(groupId, true)
            },
            before = """
                <project>
                    <dependencyManagement>
                        <dependencies>
                            <groupId>group</groupId>
                        </dependencies>
                    </dependencyManagement>
                </project>
            """,
            after = """
                <project/>
            """
    )
}
