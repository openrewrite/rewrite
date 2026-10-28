/*
 * Copyright 2026 the original author or authors.
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
package org.openrewrite.groovy.tree;

import org.junit.jupiter.api.Test;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.Space;
import org.openrewrite.marker.Markers;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.openrewrite.Tree.randomId;

class GTest {

    @Test
    void nodesWithDifferentIdsAreNotEqual() {
        // given
        J.Empty empty = new J.Empty(randomId(), Space.EMPTY, Markers.EMPTY);
        List<J> nodes = List.of(
          new G.ExpressionStatement(randomId(), empty),
          new G.ExpressionStatement(randomId(), empty),
          new G.GString(randomId(), Space.EMPTY, Markers.EMPTY, "\"", emptyList(), null),
          new G.GString(randomId(), Space.EMPTY, Markers.EMPTY, "\"", emptyList(), null),
          new G.GString.Value(randomId(), Markers.EMPTY, empty, Space.EMPTY, true),
          new G.GString.Value(randomId(), Markers.EMPTY, empty, Space.EMPTY, true)
        );

        // when
        Set<J> distinct = new HashSet<>(nodes);

        // then
        assertThat(distinct).hasSize(6);
    }
}
