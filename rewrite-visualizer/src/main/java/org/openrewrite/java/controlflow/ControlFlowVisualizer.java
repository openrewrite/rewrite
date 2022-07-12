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

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.swing_viewer.DefaultView;
import org.graphstream.ui.view.Viewer;

import java.awt.*;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class ControlFlowVisualizer {

    public static void printCFG(ControlFlowSummary summary) {
        Set<ControlFlowNode> all = summary.getAllNodes();
        // map each node to its index in the list
        Map<ControlFlowNode, Integer> nodeToIndex = new HashMap<>();
        int index = 0;
        for (ControlFlowNode node : all) {
            nodeToIndex.put(node, index);
            index++;
        }
        // print the graph
        System.out.println("digraph G {");
        for (ControlFlowNode node : all) {
            if (node instanceof ControlFlowNode.BasicBlock) {
                ControlFlowNode.BasicBlock basicBlock = (ControlFlowNode.BasicBlock) node;
                System.out.println("  " + nodeToIndex.get(node) + " [label=\"" + basicBlock.getStatementsWithinBlock() + "\"];");
                for (ControlFlowNode successor : node.getSuccessors()) {
                    System.out.println(String.format("  %d -> %d;", nodeToIndex.get(node), nodeToIndex.get(successor)));
                }
                continue;
            } else if (node instanceof ControlFlowNode.ConditionNode) {
                ControlFlowNode.ConditionNode conditionNode = (ControlFlowNode.ConditionNode) node;
                System.out.println("  " + nodeToIndex.get(node) + " [label=\"" + conditionNode.getCondition() + "\"];");
                for (ControlFlowNode successor : node.getSuccessors()) {
                    System.out.println(String.format("  %d -> %d;", nodeToIndex.get(node), nodeToIndex.get(successor)));
                }
                continue;
            }
            System.out.println(String.format("  %d [label=\"%s\"];", nodeToIndex.get(node), node.toString()));
            for (ControlFlowNode successor : node.getSuccessors()) {
                System.out.println(String.format("  %d -> %d;", nodeToIndex.get(node), nodeToIndex.get(successor)));
            }
        }
        System.out.println("}");

        showVisual(nodeToIndex);

        System.out.println("Graph displayed.");


    }

    private static void showVisual(Map<ControlFlowNode, Integer> nodeToIndex) {
        System.setProperty("org.graphstream.ui", "swing");

        String cssUrl;
        try (ScanResult scanResult = new ClassGraph().acceptPaths("rewrite-visualizer").enableMemoryMapping().scan()) {
            cssUrl = scanResult.getResourcesWithLeafName("styleCFG.css").getURLs().get(0).toString();
        }
        Graph graph = new SingleGraph("CFG");
        graph.setAttribute("ui.stylesheet", "url('" + cssUrl + "')");

        for (ControlFlowNode node : nodeToIndex.keySet()) {
            if (node instanceof ControlFlowNode.BasicBlock) {
                ControlFlowNode.BasicBlock basicBlock = (ControlFlowNode.BasicBlock) node;
                Node n = graph.addNode(String.valueOf(nodeToIndex.get(node)));
                n.setAttribute("ui.class", "BasicBlock");
//                n.setAttribute("ui.label", nodeToIndex.get(node));
                n.setAttribute("ui.label", basicBlock.getStatementsWithinBlock());

            } else if (node instanceof ControlFlowNode.ConditionNode) {
                ControlFlowNode.ConditionNode conditionNode = (ControlFlowNode.ConditionNode) node;
                Node n = graph.addNode(String.valueOf(nodeToIndex.get(node)));
                n.setAttribute("ui.class", "ConditionNode");
//                n.setAttribute("ui.label", nodeToIndex.get(node));
                n.setAttribute("ui.label", conditionNode.getCondition());
            } else {
                Node n = graph.addNode(String.valueOf(nodeToIndex.get(node)));
                n.setAttribute("ui.class", "StartEnd");
                n.setAttribute("ui.label", node.toString());
            }
        }

        for (ControlFlowNode node : nodeToIndex.keySet()) {
            if (node instanceof ControlFlowNode.ConditionNode) {
                ControlFlowNode.ConditionNode conditionNode = (ControlFlowNode.ConditionNode) node;
                ControlFlowNode trueSuccessor = conditionNode.getTruthySuccessor();
                ControlFlowNode falseSuccessor = conditionNode.getFalsySuccessor();
                Edge trueEdge = graph.addEdge(String.valueOf(nodeToIndex.get(node)) + "->" + String.valueOf(nodeToIndex.get(trueSuccessor)),
                        String.valueOf(nodeToIndex.get(node)),
                        String.valueOf(nodeToIndex.get(trueSuccessor)),
                        true);
                trueEdge.setAttribute("ui.class", "BranchTrue");
                trueEdge.setAttribute("ui.label", "True");

                Edge falseEdge = graph.addEdge(String.valueOf(nodeToIndex.get(node)) + "->" + String.valueOf(nodeToIndex.get(falseSuccessor)),
                        String.valueOf(nodeToIndex.get(node)),
                        String.valueOf(nodeToIndex.get(falseSuccessor)),
                        true);
                falseEdge.setAttribute("ui.class", "BranchFalse");
                falseEdge.setAttribute("ui.label", "False");
            } else {
                for (ControlFlowNode successor : node.getSuccessors()) {
                    graph.addEdge(String.valueOf(nodeToIndex.get(node)) + "->" + String.valueOf(nodeToIndex.get(successor)),
                            String.valueOf(nodeToIndex.get(node)),
                            String.valueOf(nodeToIndex.get(successor)),
                            true);
                }
            }

        }

        Viewer viewer = graph.display();
        viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.CLOSE_VIEWER);


        while (Stream.of(Frame.getFrames()).anyMatch(Frame::isVisible)) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
//        viewer.getDefaultID()
//        while (true) {}



    }
}
