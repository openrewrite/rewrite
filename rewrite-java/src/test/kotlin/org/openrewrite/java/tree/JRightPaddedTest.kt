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
package org.openrewrite.java.tree

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.openrewrite.Tree.randomId
import org.openrewrite.marker.Markers

class JRightPaddedTest {

    @Test
    fun withElementsThatDoesntChangeReference() {
        val t = J.Empty(randomId(), Space.EMPTY, Markers.EMPTY)
        val trees = listOf(JRightPadded(t, Space.EMPTY, Markers.EMPTY))
        assertThat(JRightPadded.withElements(trees, listOf(t))).isSameAs(trees)
    }

    @Test
    fun withElementsThatAddsNewElement() {
        val t = J.Empty(randomId(), Space.EMPTY, Markers.EMPTY)
        val t2 = J.Empty(randomId(), Space.EMPTY, Markers.EMPTY)
        val trees = listOf(JRightPadded(t, Space.EMPTY, Markers.EMPTY))
        assertThat(JRightPadded.withElements(trees, listOf(t, t2))).isNotSameAs(trees)
    }

    @Test
    fun withElementsThatReordersElements() {
        val t = J.Empty(randomId(), Space.EMPTY, Markers.EMPTY)
        val t2 = J.Empty(randomId(), Space.EMPTY, Markers.EMPTY)
        val trees = listOf(
            JRightPadded(t, Space.EMPTY, Markers.EMPTY),
            JRightPadded(t2, Space.EMPTY, Markers.EMPTY)
        )
        assertThat(JRightPadded.withElements(trees, listOf(t2, t))).isNotSameAs(trees)
    }

    @Test
    fun withElementsThatDeletesElement() {
        val t = J.Empty(randomId(), Space.EMPTY, Markers.EMPTY)
        val trees = listOf(JRightPadded(t, Space.EMPTY, Markers.EMPTY))
        assertThat(JRightPadded.withElements(trees, emptyList())).isNotSameAs(trees)
    }

    @Test
    fun withElementsThatChangesElement() {
        val t = J.Empty(randomId(), Space.EMPTY, Markers.EMPTY)
        val trees = listOf(JRightPadded(t, Space.EMPTY, Markers.EMPTY))
        assertThat(JRightPadded.withElements(trees, listOf(t.withPrefix(Space.format(" ")))))
            .isNotSameAs(trees)
    }
}
