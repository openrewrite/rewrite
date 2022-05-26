/*
 * Copyright 2022 the original author or authors.
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
package org.openrewrite.java.dataflow.analysis;

import lombok.AccessLevel;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.openrewrite.Cursor;

import javax.annotation.CheckReturnValue;
import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;

@Data
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@Setter(AccessLevel.PACKAGE)
public class FlowGraph {
    private final Cursor cursor;
    private List<FlowGraph> edges = emptyList();

    /**
     * Add an edge to the graph returning the newly added {@link FlowGraph} leaf.
     * @param cursor The cursor position of the new leaf.
     * @return The newly added {@link FlowGraph} leaf.
     */
    @CheckReturnValue
    FlowGraph addEdge(Cursor cursor) {
        if (edges.isEmpty()) {
            edges = new ArrayList<>(2);
        }
        FlowGraph edge = new FlowGraph(cursor);
        edges.add(edge);
        return edge;
    }
}
