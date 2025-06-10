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
package org.openrewrite.internal;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ListUtilsTest {

    @Nested
    class FlatMap {
        @Test
        void flatMap() {
            var l = List.of(1.0, 2.0, 3.0);
            assertThat(ListUtils.flatMap(l, l2 -> l2.intValue() % 2 == 0 ? List.of(2.0, 2.1, 2.2) : l2))
              .containsExactly(1.0, 2.0, 2.1, 2.2, 3.0);
        }

        @Test
        void flatMapWithNoChangeShouldHaveReferenceEquality() {
            var before = Arrays.asList(1, 2, 3);
            var after = ListUtils.flatMap(before, Collections::singletonList);
            assertThat(after).isSameAs(before);
        }

        @Test
        void flatMapWithNoChangeShouldHaveReferenceEqualityIncludingNulls() {
            var before = Arrays.asList(1, null, 3);
            var after = ListUtils.flatMap(before, Collections::singletonList);
            assertThat(after).isSameAs(before);
        }

        @Test
        void replaceSingleWithMultipleAtPosition0() {
            var l = List.of(1);
            assertThat(ListUtils.flatMap(l, l2 -> List.of(2, 3)))
              .containsExactly(2, 3);
        }

        @Test
        void removeSingleItem() {
            var l = List.of(1, 2, 3);
            assertThat(ListUtils.flatMap(l, l1 -> l1.equals(2) ? null : l1))
              .containsExactly(1, 3);
        }

        @Test
        void replaceItemWithCollectionThenRemoveNextItem() {
            var l = List.of(2, 0);
            assertThat(ListUtils.flatMap(l, l1 -> l1.equals(2) ? List.of(10, 11) : null))
              .containsExactly(10, 11);
        }

        @Test
        void removeByAddingEmptyCollection() {
            var l = List.of(2, 0);
            assertThat(ListUtils.flatMap(l, l1 -> l1.equals(2) ? List.of(10, 11) : List.of()))
              .containsExactly(10, 11);
        }
    }

    @Nested
    class Map {
        @Test
        void identityMap() {
            var l = Arrays.asList(1, 2, 3);
            assertThat(ListUtils.map(l, (i, e) -> e)).isSameAs(l);
        }
        @Test
        void identityMapWithNulls() {
            var l = Arrays.asList(1, null, 3);
            assertThat(ListUtils.map(l, (i, e) -> e)).isSameAs(l);
        }
        @Test
        void removeElements() {
            var l = Arrays.asList(1, 2, 3);
            assertThat(ListUtils.map(l, (i, e) -> i % 2 == 0 ? null : e)).isEqualTo(List.of(2));
        }
        @Test
        void removeElementsWithNulls() {
            var l = Arrays.asList(0, 1, 2, null, 4, 5);
            assertThat(ListUtils.map(l, (i, e) -> i == 4 ? null : e)).isEqualTo(List.of(0, 1, 2, 5));
        }
    }
}
