package org.openrewrite.java.controlflow;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.SingleGraph;
import org.graphstream.ui.spriteManager.SpriteManager;

import java.util.HashMap;
import java.util.Map;

public class ControlFlowGraph {

    private static Graph graph;
    private static Map<Node, ControlFlowNode> nodeToNode;

    public static Map<Node, ControlFlowNode> getAbstractToVisualNodeMapping () {
        return nodeToNode;
    }
    public static Graph buildGraph(Map<ControlFlowNode, Integer> nodeToIndex){
        graph = new SingleGraph("CFG");
        SpriteManager sman = new SpriteManager(graph);


        String cssUrl;
        try (ScanResult scanResult = new ClassGraph().acceptPaths("rewrite-visualizer").enableMemoryMapping().scan()) {
            cssUrl = scanResult.getResourcesWithLeafName("styleCFG.css").getURLs().get(0).toString();
        }

        graph.setAttribute("ui.stylesheet", "url('" + cssUrl + "')");
        nodeToNode = new HashMap<>();

        for (ControlFlowNode node : nodeToIndex.keySet()) {
            if (node instanceof ControlFlowNode.BasicBlock) {
                ControlFlowNode.BasicBlock basicBlock = (ControlFlowNode.BasicBlock) node;
                Node n = graph.addNode(String.valueOf(nodeToIndex.get(node)));
                n.setAttribute("ui.class", "BasicBlock");
//                n.setAttribute("ui.label", nodeToIndex.get(node));
                n.setAttribute("ui.label", basicBlock.getStatementsWithinBlock());
                nodeToNode.put(n, node);

//                Sprite s1 = sman.addSprite("S1" + nodeToIndex.get(node));
//                s1.setAttribute("ui.class", "BasicBlock");
//                s1.setAttribute("ui.label", basicBlock.getStatementsWithinBlock());
//                s1.attachToNode(String.valueOf(nodeToIndex.get(node)));

            } else if (node instanceof ControlFlowNode.ConditionNode) {
                ControlFlowNode.ConditionNode conditionNode = (ControlFlowNode.ConditionNode) node;
                Node n = graph.addNode(String.valueOf(nodeToIndex.get(node)));
                n.setAttribute("ui.class", "ConditionNode");
//                n.setAttribute("ui.label", nodeToIndex.get(node));
                n.setAttribute("ui.label", conditionNode.getCondition());
                nodeToNode.put(n, node);
            } else {
                Node n = graph.addNode(String.valueOf(nodeToIndex.get(node)));
                n.setAttribute("ui.class", "StartEnd");
                n.setAttribute("ui.label", node.toString());
                nodeToNode.put(n, node);
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
        return graph;
    }
}
