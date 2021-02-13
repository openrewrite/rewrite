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
package org.openrewrite.yaml.tree

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.openrewrite.TreeSerializer
import org.openrewrite.yaml.YamlParser

class YamlDocumentsSerializerTest {

    @Test
    fun roundTripSerialization() {
        val serializer = TreeSerializer<Yaml.Documents>()
        val a = YamlParser.builder().build().parse("key: value")[0]

        val aBytes = serializer.write(a)
        val aDeser = serializer.read(aBytes)

        assertEquals(a, aDeser)
    }

    @Test
    fun roundTripSerializationList() {
        val serializer = TreeSerializer<Yaml.Documents>()
        val y1 = YamlParser.builder().build().parse("key: value")[0]
        val y2 = YamlParser.builder().build().parse("key: value")[0]

        val serialized = serializer.write(listOf(y1, y2))
        val deserialized = serializer.readList(serialized)

        assertEquals(y1, deserialized[0])
    }
}
