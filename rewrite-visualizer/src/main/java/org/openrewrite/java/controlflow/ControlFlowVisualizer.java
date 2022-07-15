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
import lombok.Builder;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.graphstream.ui.swing_viewer.DefaultView;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.swing_viewer.ViewPanel;
import org.graphstream.ui.swing_viewer.util.MouseOverMouseManager;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;
import org.graphstream.ui.view.util.InteractiveElement;

import javax.swing.*;
import java.awt.*;
import java.util.*;
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
                System.out.println("  " + nodeToIndex.get(node) + " [label=\"\n" + basicBlock.getStatementsWithinBlock() + "\"\n];");
                for (ControlFlowNode successor : node.getSuccessors()) {
                    System.out.println(String.format("  %d -> %d;", nodeToIndex.get(node), nodeToIndex.get(successor)));
                }
                continue;
            } else if (node instanceof ControlFlowNode.ConditionNode) {
                ControlFlowNode.ConditionNode conditionNode = (ControlFlowNode.ConditionNode) node;
                System.out.println("  " + nodeToIndex.get(node) + " [label=\"\n" + conditionNode.getCondition() + "\"\n];");
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

        GraphShower shower = new GraphShower(nodeToIndex);
        shower.runGraph();

        System.out.println("Graph displayed.");


    }






//    private static void showVisual(Map<ControlFlowNode, Integer> nodeToIndex) {
//        System.setProperty("org.graphstream.ui", "swing");
//        System.setProperty("sun.java2d.uiScale", "1.0");
//
//        graph = ControlFlowGraph.buildGraph(nodeToIndex);
//
////        Viewer viewer = graph.display();
////        viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.CLOSE_VIEWER);
////        viewer.addViewerListener(new ClickHandler);
////        viewer.addDefaultView(true);
//
////        viewer.getDefaultView().enableMouseOptions();
//
//        runGraph();
//
////        while (Stream.of(Frame.getFrames()).anyMatch(Frame::isVisible)) {
////            try {
////                Thread.sleep(100);
////            } catch (InterruptedException e) {
////                e.printStackTrace();
////            }
////        }
//
//        return graph;
//
//
//    }
//
//
//    private void runGraph () {
//
//            mainFrame = new JFrame();
//            JFrame frame = mainFrame;
//            mainPanel = new JPanel(){
//                @Override
//                public Dimension getPreferredSize() {
//                    return new Dimension(640, 480);
//                }
//            };
//            mainPanel.setLayout(new BorderLayout());
//            JPanel panel = mainPanel;
//            codeArea = new JTextArea();
//            codeArea.setEditable(false);
//
//            viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.CLOSE_VIEWER);
//            panel.setBorder(BorderFactory.createLineBorder(Color.blue, 5));
//            mainFrame.add(panel);
//
//            graph = new SingleGraph("Clicks");
//            Node a = graph.addNode("A");
//            a.setAttribute("ui.style", "shape:circle; text-size:10px;");
//            a.setAttribute("ui.label", "abc A");
//            graph.addNode("B");
//            graph.addNode("C");
//            graph.addEdge("AB", "A", "B");
//            graph.addEdge("BC", "B", "C");
//            graph.addEdge("CA", "C", "A");
////          graph.display();
//
//            Viewer viewer = new SwingViewer(graph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);
//
//
//            ViewPanel viewPanel = (ViewPanel) viewer.addDefaultView(false);
//            viewer.getDefaultView().enableMouseOptions();
//            viewer.enableAutoLayout();
//
//            panel.add(viewPanel, BorderLayout.CENTER);
//            panel.add(codeArea, BorderLayout.SOUTH);
//            frame.pack();
//            frame.setLocationRelativeTo(null);
//            frame.setVisible(true);
//
//            ViewerPipe fromViewer = viewer.newViewerPipe();
//            fromViewer.addViewerListener(new Clicks.MouseOptions());
//            fromViewer.addSink(graph);
//
//            while(true) {
//                fromViewer.pump();
//            }
//        }
//
//    }


}
