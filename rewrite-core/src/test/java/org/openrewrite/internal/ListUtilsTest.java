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

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class ListUtilsTest {

    @Test
    void flatMap() {
        var l = List.of(1.0, 2.0, 3.0);
        assertThat(ListUtils.flatMap(l, l2 -> l2.intValue() % 2 == 0 ? List.of(2.0, 2.1, 2.2) : List.of(l2)))
          .containsExactly(1.0, 2.0, 2.1, 2.2, 3.0);
    }

    @Test
    void flatMapList() {
        var before = List.of(List.of(1, 2), List.of(3, 4));
        var after = ListUtils.flatMap(before,
          list -> Collections.singletonList(list.stream().map(n -> n * 2).collect(Collectors.toList())));
        assertThat(after).containsExactly(List.of(2, 4), List.of(6, 8));
    }

    @Test
    void flatMapWithNoChangeShouldHaveReferenceEquality() {
        var before = List.of(1, 2, 3);
        var after = ListUtils.flatMap(before, Collections::singletonList);
        assertThat(before).isSameAs(after);
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
        assertThat(ListUtils.flatMap(l, l1 -> l1.equals(2) ? null : List.of(l1)))
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
