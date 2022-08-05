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
package org.openrewrite.java.controlflow;

import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.Format;
import guru.nidi.graphviz.engine.Graphviz;
import guru.nidi.graphviz.engine.GraphvizJdkEngine;
import guru.nidi.graphviz.engine.GraphvizV8Engine;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.Node;
import lombok.NoArgsConstructor;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static guru.nidi.graphviz.attribute.Rank.RankDir.TOP_TO_BOTTOM;
import static guru.nidi.graphviz.model.Factory.*;
import static guru.nidi.graphviz.model.Factory.to;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
final class ControlFlowVisualizer {

    static {
        Graphviz.useEngine(new GraphvizV8Engine());
    }

    static String visualizeAsDotfile(String name, ControlFlowSummary summary) {
        final Graph graph = createGraph(name, summary);
        return Graphviz.fromGraph(graph).render(Format.DOT).toString();
    }

    static Graph createGraph(String name, ControlFlowSummary summary) {
        Set<ControlFlowNode> all = summary.getAllNodes();
        // map each node to its index in the list
        Map<ControlFlowNode, Integer> nodeToIndex = new HashMap<>();
        int index = 0;
        for (ControlFlowNode node : all) {
            nodeToIndex.put(node, index);
            index++;
        }

        return createGraph(name, nodeToIndex);
    }

    private static Graph createGraph(String name, Map<ControlFlowNode, Integer> nodeToIndex) {
        final Map<ControlFlowNode, Node> abstractToVisualNodeMapping = new HashMap<>();

        Graph g = graph(name).directed()
                .graphAttr().with(Rank.dir(TOP_TO_BOTTOM))
                .nodeAttr().with(Font.name("courier"))
                .linkAttr().with("class", "link-class")
                .linkAttr().with(Font.name("arial"));

        for (ControlFlowNode node : nodeToIndex.keySet()) {
            String lbl = nodeToIndex.get(node).toString();
            Node n = null;
            if (node instanceof ControlFlowNode.BasicBlock) {
                ControlFlowNode.BasicBlock bb = (ControlFlowNode.BasicBlock) node;
                n = node(lbl).with(Shape.RECTANGLE,
                        Label.of(bb.getStatementsWithinBlock().replace("\n", "\\l") + "\\l"));
                abstractToVisualNodeMapping.put(bb, n);
            } else if (node instanceof ControlFlowNode.ConditionNode) {
                ControlFlowNode.ConditionNode cn = (ControlFlowNode.ConditionNode) node;
                n = node(lbl).with(Shape.DIAMOND, Label.of(cn.getCondition().toString()));
                abstractToVisualNodeMapping.put(cn, n);
            } else if (node instanceof ControlFlowNode.Start) {
                ControlFlowNode.Start start = (ControlFlowNode.Start) node;
                n = node(lbl).with(Shape.CIRCLE, Label.of(start.toString()), Font.name("arial"));
                abstractToVisualNodeMapping.put(start, n);
            } else if (node instanceof ControlFlowNode.End) {
                ControlFlowNode.End end = (ControlFlowNode.End) node;
                n = node(lbl).with(Shape.CIRCLE, Label.of(end.toString()), Font.name("arial"));
                abstractToVisualNodeMapping.put(end, n);
            }
        }

        for (ControlFlowNode node : nodeToIndex.keySet()) {
            if (node instanceof ControlFlowNode.ConditionNode) {
                ControlFlowNode.ConditionNode cn = (ControlFlowNode.ConditionNode) node;
                g = g.with(abstractToVisualNodeMapping.get(node)
                        .link(to(abstractToVisualNodeMapping.get(cn.getTruthySuccessor()))
                                .with(Color.GREEN)
                                .with(Label.html("True"), Color.rgb("006400").font())));
                g = g.with(abstractToVisualNodeMapping.get(node)
                        .link(to(abstractToVisualNodeMapping.get(cn.getFalsySuccessor()))
                                .with(Color.RED)
                                .with(Label.html("False"), Color.rgb("FF0000").font())));
            } else {
                for (ControlFlowNode successor : node.getSuccessors()) {
                    g = g.with(abstractToVisualNodeMapping.get(node).link(to(abstractToVisualNodeMapping.get(successor))));
                }
            }
        }

        return g;
    }

    @SuppressWarnings("unused")
    public static void renderGraphToFile(String name, Graph graph) {
        try {
            Graphviz.useEngine(new GraphvizV8Engine());
            Graphviz.fromGraph(graph).render(Format.DOT).toFile(new File("example/", name + ".dot"));
            Graphviz.fromGraph(graph).render(Format.SVG).toFile(new File("example/", name + ".svg"));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render graph!", e);
        }
    }
}
