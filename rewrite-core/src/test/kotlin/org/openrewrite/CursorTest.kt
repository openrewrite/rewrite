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
import org.openrewrite.text.PlainText
import kotlin.streams.toList

class CursorTest {
    @Test
    fun peekMessages() {
        val t = PlainText(randomId(), Markers.EMPTY, "test")
        val cursor = Cursor(null, t)

        cursor.putMessage("key", 1)
        assertThat(cursor.getNearestMessage<Int>("key")!!).isEqualTo(1)

        val child = Cursor(cursor, t)
        assertThat(child.getNearestMessage<Int>("key")!!).isEqualTo(1)
    }

    @Test
    fun pollMessages() {
        val t = PlainText(randomId(), Markers.EMPTY, "test")
        val cursor = Cursor(null, t)

        cursor.putMessage("key", 1)
        assertThat(cursor.pollNearestMessage<Int>("key")!!).isEqualTo(1)

        @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
        assertThat(cursor.pollNearestMessage<Integer>("key")).isNull()

        cursor.putMessage("key", 1)
        val child = Cursor(cursor, t)
        assertThat(child.getNearestMessage<Int>("key")!!).isEqualTo(1)
    }

    @Test
    fun pathPredicates() {
        val t = PlainText(randomId(), Markers.EMPTY, "test")
        val cursor = Cursor(Cursor(Cursor(null, 1), t), 2)
        assertThat(cursor.getPath { it is PlainText }.next()).isSameAs(t)
    }

    @Test
    fun pathAsStreamPredicates() {
        val t = PlainText(randomId(), Markers.EMPTY, "test")
        val cursor = Cursor(Cursor(Cursor(null, 1), t), 2)
        assertThat(cursor.getPathAsStream { it is PlainText }.toList()).containsExactly(t)
    }
}
