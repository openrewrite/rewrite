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
import lombok.Getter;
import org.openrewrite.Cursor;
import org.openrewrite.Incubating;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.dataflow.LocalFlowSpec;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Incubating(since = "7.24.0")
public class SinkFlow<Source extends Expression, Sink extends J> extends FlowGraph {
    @Nullable
    private final Cursor source;

    @Getter(AccessLevel.PACKAGE)
    private final LocalFlowSpec<Source, Sink> spec;

    private final Set<Expression> reachable;

    public SinkFlow(@Nullable Cursor source, LocalFlowSpec<Source, Sink> spec, Set<Expression> reachable) {
        super(source);
        this.source = source;
        this.spec = spec;
        this.reachable = reachable;
    }

    public Cursor getSourceCursor() {
        return requireNonNull(source);
    }

    public Source getSource() {
        return requireNonNull(source).getValue();
    }

    public List<Sink> getSinks() {
        return getSinkCursors()
                .stream()
                .map(Cursor::<Sink>getValue)
                .collect(Collectors.toList());
    }

    public List<Cursor> getSinkCursors() {
        List<List<Cursor>> flows = getFlows();
        List<Cursor> sinkCursors = new ArrayList<>(flows.size());
        for (List<Cursor> flow : flows) {
            sinkCursors.add(flow.get(flow.size() - 1));
        }
        return sinkCursors;
    }

    public List<List<Cursor>> getFlows() {
        // IMPORTANT NOTE!
        // A naive implementation would return emptyList if the edges are empty.
        // But we need to consider that the source can also be a valid sink.
        List<List<Cursor>> flows = new ArrayList<>();
        Stack<Cursor> path = new Stack<>();
        path.push(getCursor());
        recurseGetFlows(this, path, flows);
        return flows;
    }

    public boolean isEmpty() {
        return getSinks().isEmpty();
    }

    public boolean isNotEmpty() {
        return !isEmpty();
    }

    private void recurseGetFlows(FlowGraph flowGraph, Stack<Cursor> pathToHere,
                                 List<List<Cursor>> pathsToSinks) {
        Cursor cursor = flowGraph.getCursor();
        if (cursor.getValue() instanceof Expression && !reachable.contains(cursor.<Expression>getValue())) {
            return;
        }

        boolean isSinkType = spec.getSinkType().isAssignableFrom(cursor.getValue().getClass());
        if (isSinkType && spec.isSink(cursor.getValue(), cursor)) {
            List<Cursor> flow = new ArrayList<>(pathToHere);
            flow.add(cursor);
            pathsToSinks.add(flow);
        }

        for (FlowGraph edge : flowGraph.getEdges()) {
            Stack<Cursor> pathToEdge = new Stack<>();
            pathToEdge.addAll(pathToHere);
            pathToEdge.add(edge.getCursor());
            recurseGetFlows(edge, pathToEdge, pathsToSinks);
        }
    }
}
