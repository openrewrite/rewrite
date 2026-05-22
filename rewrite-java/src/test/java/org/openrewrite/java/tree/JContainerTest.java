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

class JContainerTest {

    @Test
    void withBeforeThatDoesntChangeReference() {
        var t = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var trees = JContainer.build(List.of(new JRightPadded<>(t, Space.EMPTY, Markers.EMPTY)));
        assertThat(trees.withBefore(Space.EMPTY)).isSameAs(trees);
    }

    @Test
    void withMarkerThatDoesntChangeReference() {
        var t = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var trees = JContainer.build(List.of(new JRightPadded<>(t, Space.EMPTY, Markers.EMPTY)));
        assertThat(trees.withMarkers(Markers.EMPTY)).isSameAs(trees);
    }

    @Test
    void withElementsThatDoesntChangeReference() {
        var t = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var trees = JContainer.build(List.of(new JRightPadded<>(t, Space.EMPTY, Markers.EMPTY)));
        assertThat(JContainer.withElements(trees, List.of(t))).isSameAs(trees);
    }

    @Test
    void withElementsThatAddsNewElement() {
        var t = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var t2 = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var trees = JContainer.build(List.of(new JRightPadded<>(t, Space.EMPTY, Markers.EMPTY)));
        assertThat(JContainer.withElements(trees, List.of(t, t2))).isNotSameAs(trees);
    }

    @Test
    void withElementsThatReordersElements() {
        var t = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var t2 = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var trees = JContainer.build(
            List.of(
                new JRightPadded<>(t, Space.EMPTY, Markers.EMPTY),
                new JRightPadded<>(t2, Space.EMPTY, Markers.EMPTY)
            )
        );
        assertThat(JContainer.withElements(trees, List.of(t2, t))).isNotSameAs(trees);
    }

    @Test
    void withElementsThatDeletesElement() {
        var t = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var trees = JContainer.build(List.of(new JRightPadded<>(t, Space.EMPTY, Markers.EMPTY)));
        assertThat(JContainer.withElements(trees, emptyList())).isNotSameAs(trees);
    }

    @Test
    void withElementsThatChangesElement() {
        var t = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var trees = JContainer.build(List.of(new JRightPadded<>(t, Space.EMPTY, Markers.EMPTY)));
        assertThat(JContainer.withElements(trees, List.of(t.withPrefix(Space.format(" ")))))
            .isNotSameAs(trees);
    }

    @Test
    void withElementsNullableTwoArgDefaultsBeforeToEmpty() {
        var t = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var container = JContainer.withElementsNullable(null, List.of(t));
        assertThat(container).isNotNull();
        assertThat(container.getBefore()).isEqualTo(Space.EMPTY);
        assertThat(container.getElements().get(0).getPrefix()).isEqualTo(Space.EMPTY);
    }

    @Test
    void withElementsNullableUsesDefaultBeforeWhenBuildingNewContainer() {
        var t = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var container = JContainer.withElementsNullable(null, List.of(t), Space.SINGLE_SPACE, Space.SINGLE_SPACE);
        assertThat(container).isNotNull();
        assertThat(container.getBefore()).isEqualTo(Space.SINGLE_SPACE);
    }

    @Test
    void withElementsNullableNormalizesFirstElementPrefixWhenEmpty() {
        var t = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var t2 = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var container = JContainer.withElementsNullable(null, List.of(t, t2), Space.SINGLE_SPACE, Space.SINGLE_SPACE);
        assertThat(container).isNotNull();
        assertThat(container.getElements().get(0).getPrefix()).isEqualTo(Space.SINGLE_SPACE);
        // subsequent elements are the caller's responsibility — they keep their original (empty) prefix
        assertThat(container.getElements().get(1).getPrefix()).isEqualTo(Space.EMPTY);
    }

    @Test
    void withElementsNullablePreservesIntentionalFirstElementPrefix() {
        var t = new J.Empty(randomId(), Space.format("\n  "), Markers.EMPTY);
        var container = JContainer.withElementsNullable(null, List.of(t), Space.SINGLE_SPACE, Space.SINGLE_SPACE);
        assertThat(container).isNotNull();
        assertThat(container.getElements().get(0).getPrefix()).isEqualTo(Space.format("\n  "));
    }

    @Test
    void withElementsNullableLeavesFirstElementPrefixWhenDefaultPrefixIsEmpty() {
        // try-resources shape: keyword-prefixed container BUT first-element prefix should stay EMPTY
        // because the delimiter "(" sits flush against the first element.
        var t = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var container = JContainer.withElementsNullable(null, List.of(t), Space.SINGLE_SPACE, Space.EMPTY);
        assertThat(container).isNotNull();
        assertThat(container.getBefore()).isEqualTo(Space.SINGLE_SPACE);
        assertThat(container.getElements().get(0).getPrefix()).isEqualTo(Space.EMPTY);
    }

    @Test
    void withElementsNullableIgnoresDefaultsWhenExistingContainerProvided() {
        var t = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var t2 = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        var existing = JContainer.build(Space.format(" "), List.of(new JRightPadded<>(t, Space.EMPTY, Markers.EMPTY)), Markers.EMPTY);
        var updated = JContainer.withElementsNullable(existing, List.of(t, t2), Space.format("\n"), Space.format("\n"));
        assertThat(updated).isNotNull();
        assertThat(updated.getBefore()).isEqualTo(Space.format(" "));
        assertThat(updated.getElements().get(0).getPrefix()).isEqualTo(Space.EMPTY);
        assertThat(updated.getElements()).hasSize(2);
    }

    @Test
    void withElementsNullableReturnsNullForEmptyElementsEvenWithDefaults() {
        assertThat(JContainer.withElementsNullable(null, emptyList(), Space.SINGLE_SPACE, Space.SINGLE_SPACE)).isNull();
        assertThat(JContainer.<J>withElementsNullable(null, null, Space.SINGLE_SPACE, Space.SINGLE_SPACE)).isNull();
    }
}
