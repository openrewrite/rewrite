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

import org.openrewrite.Cursor;
import org.openrewrite.java.dataflow.LocalFlowSpec;
import org.openrewrite.java.tree.Expression;

import java.util.*;

import static java.util.Collections.emptyList;

public class SinkFlow<Sink extends Expression> extends FlowGraph {
    private final LocalFlowSpec<?, Sink> spec;

    public SinkFlow(LocalFlowSpec<?, Sink> spec, Cursor cursor) {
        super(cursor);
        this.spec = spec;
    }

    public List<Sink> getSinks() {
        if (getEdges().isEmpty()) {
            return emptyList();
        }
        List<List<Cursor>> flows = getFlows();
        List<Sink> sinks = new ArrayList<>(flows.size());
        for (List<Cursor> flow : flows) {
            sinks.add(flow.get(flow.size() - 1).getValue());
        }
        return sinks;
    }

    public List<List<Cursor>> getFlows() {
        if (getEdges().isEmpty()) {
            return emptyList();
        }
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
        Iterator<Cursor> cursorPath = cursor.getPathAsCursors();
        while (cursorPath.hasNext()) {
            Cursor c = cursorPath.next();
            boolean isSinkType = spec.getSinkType().isAssignableFrom(c.getValue().getClass());
            if (isSinkType && spec.isSink(c.getValue(), c)) {
                List<Cursor> flow = new ArrayList<>(pathToHere);
                flow.add(c);
                pathsToSinks.add(flow);
            }
        }

        for (FlowGraph edge : flowGraph.getEdges()) {
            Stack<Cursor> pathToEdge = new Stack<>();
            pathToEdge.addAll(pathToHere);
            pathToEdge.add(edge.getCursor());
            recurseGetFlows(edge, pathToEdge, pathsToSinks);
        }
    }
}
