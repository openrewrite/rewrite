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
package org.openrewrite.xml.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.TreeSerializer
import org.openrewrite.xml.XmlParser

class XmlDocumentSerializerTest {

    @Test
    fun roundTripSerialization() {
        val serializer = TreeSerializer<Xml.Document>()
        val a = XmlParser().parse("<root></root>")[0]

        val aBytes = serializer.write(a)
        val aDeser = serializer.read(aBytes)

        assertEquals(a, aDeser)
    }

    @Test
    fun roundTripSerializationList() {
        val serializer = TreeSerializer<Xml.Document>()
        val x1 = XmlParser().parse("<root></root>")[0]
        val x2 = XmlParser().parse("<another></another>")[0]

        val serialized = serializer.write(listOf(x1, x2))
        val deserialized = serializer.readList(serialized)

        assertEquals(x1, deserialized[0])
    }
}
