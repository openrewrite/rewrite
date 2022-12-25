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
package org.openrewrite.java.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.marker.Markers;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class JRightPaddedTest {

    @Test
    void withElementsThatDoesntChangeReference() {
        var t = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var trees = List.of(new JRightPadded<>(t, Space.EMPTY, Markers.EMPTY));
        assertThat(JRightPadded.withElements(trees, List.of(t))).isSameAs(trees);
    }

    @Test
    void withElementsThatAddsNewElement() {
        var t = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var t2 = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var trees = List.of(new JRightPadded<>(t, Space.EMPTY, Markers.EMPTY));
        assertThat(JRightPadded.withElements(trees, List.of(t, t2))).isNotSameAs(trees);
    }

    @Test
    void withElementsThatReordersElements() {
        var t = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var t2 = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var trees = List.of(
            new JRightPadded<>(t, Space.EMPTY, Markers.EMPTY),
            new JRightPadded<>(t2, Space.EMPTY, Markers.EMPTY)
        );
        assertThat(JRightPadded.withElements(trees, List.of(t2, t))).isNotSameAs(trees);
    }

    @Test
    void withElementsThatDeletesElement() {
        var t = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var trees = List.of(new JRightPadded<>(t, Space.EMPTY, Markers.EMPTY));
        assertThat(JRightPadded.withElements(trees, emptyList())).isNotSameAs(trees);
    }

    @Test
    void withElementsThatChangesElement() {
        var t = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var trees = List.of(new JRightPadded<>(t, Space.EMPTY, Markers.EMPTY));
        assertThat(JRightPadded.withElements(trees, List.of(t.withPrefix(Space.format(" ")))))
            .isNotSameAs(trees);
    }
}
