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
package org.openrewrite

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Tree.randomId
import org.openrewrite.marker.Markers
import org.openrewrite.style.NamedStyles
import org.openrewrite.text.PlainText
import org.openrewrite.text.TextStyle

class TreeSerializerTest {
    @Test
    fun serializeStyle() {
        val serializer = TreeSerializer<PlainText>()

        val styles = NamedStyles(randomId(),"utf8", "test", "test", emptySet(), listOf(TextStyle().apply {
            charset = "UTF-8"
        }))

        val plainText1 = PlainText(randomId(), Markers.build(listOf(styles)), "hi Jon")
        val plainText2 = PlainText(randomId(), Markers.build(listOf(styles)), "hi Jonathan")

        val serialized = serializer.write(listOf(plainText1, plainText2))

        val deserialized = serializer.readList(serialized)
        assertThat(deserialized[0].getStyle(TextStyle::class.java)!!.charset).isEqualTo("UTF-8")
    }
}
