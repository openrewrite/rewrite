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

import java.util.HashMap;
import java.util.Map;


@Builder
public class ControlFlowGraph {


    private boolean expanded;

    private Map<ControlFlowNode, Integer> nodeToIndex;
    private boolean showDataFlow;


    private static Map<Node, ControlFlowNode> abstractNodeToVisualNode;

    public Map<Node, ControlFlowNode> getAbstractToVisualNodeMapping() {
        return abstractNodeToVisualNode;
    }

    public Graph loadGraph() {
        Graph graph = new SingleGraph("CFG");


        String cssUrl;
        try (ScanResult scanResult = new ClassGraph().acceptPaths("rewrite-visualizer").enableMemoryMapping().scan()) {
            cssUrl = scanResult.getResourcesWithLeafName("styleCFG.css").getURLs().get(0).toString();
        }

        graph.setAttribute("ui.stylesheet", "url('" + cssUrl + "')");
        abstractNodeToVisualNode = new HashMap<>();

        if (expanded) return getExpandedGraph(graph);
        else return getCollapsedGraph(graph);
    }

    private void addSpriteToEdge (SpriteManager sman, String edgeIdentified, String spriteClass, String label, double dist) {
        Sprite trueSuccessorSprite = sman.addSprite(edgeIdentified + "label-sprite");
        trueSuccessorSprite.attachToEdge(edgeIdentified);
        trueSuccessorSprite.setPosition(0.5, dist, 0);
        trueSuccessorSprite.setAttribute("ui.class", spriteClass);
        trueSuccessorSprite.setAttribute("ui.label", label);
    }

    public Graph getCollapsedGraph(Graph graph) {

        SpriteManager sman = new SpriteManager(graph);

        for (ControlFlowNode node : nodeToIndex.keySet()) {
            if (node instanceof ControlFlowNode.BasicBlock) {

                ControlFlowNode.BasicBlock basicBlock = (ControlFlowNode.BasicBlock) node;
                Node n = graph.addNode(String.valueOf(nodeToIndex.get(node)));
                n.setAttribute("ui.class", "BasicBlock");
                n.setAttribute("ui.label", basicBlock.getStatementsWithinBlock());
                abstractNodeToVisualNode.put(n, node);

            } else if (node instanceof ControlFlowNode.ConditionNode) {
                ControlFlowNode.ConditionNode conditionNode = (ControlFlowNode.ConditionNode) node;
                Node n = graph.addNode(String.valueOf(nodeToIndex.get(node)));
                n.setAttribute("ui.class", "ConditionNode");
//                n.setAttribute("ui.label", nodeToIndex.get(node));
                n.setAttribute("ui.label", conditionNode.getCondition());
                abstractNodeToVisualNode.put(n, node);
            } else if (node instanceof ControlFlowNode.Start) {
                Node n = graph.addNode(String.valueOf(nodeToIndex.get(node)));
                n.setAttribute("ui.class", "Start");
                n.setAttribute("ui.label", node.toString());
                abstractNodeToVisualNode.put(n, node);
            } else {
                Node n = graph.addNode(String.valueOf(nodeToIndex.get(node)));
                n.setAttribute("ui.class", "End");
                n.setAttribute("ui.label", node.toString());
                abstractNodeToVisualNode.put(n, node);
            }
        }

        for (ControlFlowNode node : nodeToIndex.keySet()) {
            if (node instanceof ControlFlowNode.ConditionNode) {
                ControlFlowNode.ConditionNode conditionNode = (ControlFlowNode.ConditionNode) node;
                ControlFlowNode trueSuccessor = conditionNode.getTruthySuccessor();
                ControlFlowNode falseSuccessor = conditionNode.getFalsySuccessor();

                String trueEdgeId = nodeToIndex.get(node) + "->" + nodeToIndex.get(trueSuccessor);
                Edge trueEdge = graph.addEdge(trueEdgeId,String.valueOf(nodeToIndex.get(node)), String.valueOf(nodeToIndex.get(trueSuccessor)), true);
                trueEdge.setAttribute("ui.class", "BranchTrue");
                addSpriteToEdge(sman, trueEdgeId, "TrueLabel", "True", -0.1);


                String falseEdgeId = nodeToIndex.get(node) + "->" + nodeToIndex.get(falseSuccessor);
                Edge falseEdge = graph.addEdge(falseEdgeId, String.valueOf(nodeToIndex.get(node)), String.valueOf(nodeToIndex.get(falseSuccessor)), true);
                falseEdge.setAttribute("ui.class", "BranchFalse");
                addSpriteToEdge(sman, falseEdgeId, "FalseLabel", "False", 0.1);
            } else {
                for (ControlFlowNode successor : node.getSuccessors()) {
                    graph.addEdge(nodeToIndex.get(node) + "->" + nodeToIndex.get(successor), String.valueOf(nodeToIndex.get(node)), String.valueOf(nodeToIndex.get(successor)), true);
                }
            }

        }
        return graph;
    }

    private Graph getExpandedGraph(Graph graph) {
        for (ControlFlowNode node : nodeToIndex.keySet()) {
            if (node instanceof ControlFlowNode.BasicBlock) {
                ControlFlowNode.BasicBlock basicBlock = (ControlFlowNode.BasicBlock) node;
                String[] statements = basicBlock.getStatementsWithinBlock().split("\n");
                for (int i = 0; i < statements.length; i++) {
                    Node n = graph.addNode(nodeToIndex.get(node) + "-" + i);
                    n.setAttribute("ui.class", "BasicBlock");
                    n.setAttribute("ui.label", statements[i]);
                    abstractNodeToVisualNode.put(n, node);
                }

                for (int i = 0; i < statements.length - 1; i++) {
                    graph.addEdge(nodeToIndex.get(node) + "-" + i + "-" + (i + 1), nodeToIndex.get(node) + "-" + i, nodeToIndex.get(node) + "-" + (i + 1));
                }


            } else if (node instanceof ControlFlowNode.ConditionNode) {
                ControlFlowNode.ConditionNode conditionNode = (ControlFlowNode.ConditionNode) node;
                Node n = graph.addNode(String.valueOf(nodeToIndex.get(node) + "-0"));
                n.setAttribute("ui.class", "ConditionNode");
                n.setAttribute("ui.label", conditionNode.getCondition());
                abstractNodeToVisualNode.put(n, node);
            } else if (node instanceof ControlFlowNode.Start) { // start/end nodes
                Node n = graph.addNode(String.valueOf(nodeToIndex.get(node)) + "-0");
                n.setAttribute("ui.class", "Start");
                n.setAttribute("ui.label", node.toString());
                abstractNodeToVisualNode.put(n, node);
            } else {
                Node n = graph.addNode(String.valueOf(nodeToIndex.get(node)) + "-0");
                n.setAttribute("ui.class", "End");
                n.setAttribute("ui.label", node.toString());
                System.out.println("Got here");
                abstractNodeToVisualNode.put(n, node);
            }
        }

        for (ControlFlowNode node : nodeToIndex.keySet()) {
            if (node instanceof ControlFlowNode.ConditionNode) {
                ControlFlowNode.ConditionNode conditionNode = (ControlFlowNode.ConditionNode) node;
                ControlFlowNode trueSuccessor = conditionNode.getTruthySuccessor();
                ControlFlowNode falseSuccessor = conditionNode.getFalsySuccessor();
                Edge trueEdge = graph.addEdge(
                        nodeToIndex.get(node) + "->" + nodeToIndex.get(trueSuccessor),
                        String.valueOf(nodeToIndex.get(node) + "-0"),
                        String.valueOf(nodeToIndex.get(trueSuccessor) + "-0"), true);
                trueEdge.setAttribute("ui.class", "BranchTrue");
                trueEdge.setAttribute("ui.label", "True");

                Edge falseEdge = graph.addEdge(
                        nodeToIndex.get(node) + "->" + nodeToIndex.get(falseSuccessor),
                        String.valueOf(nodeToIndex.get(node) + "-0"),
                        String.valueOf(nodeToIndex.get(falseSuccessor) + "-0"), true);
                falseEdge.setAttribute("ui.class", "BranchFalse");
                falseEdge.setAttribute("ui.label", "False");
            } else if (node instanceof ControlFlowNode.BasicBlock) {
                ControlFlowNode.BasicBlock basicBlock = (ControlFlowNode.BasicBlock) node;
                for (ControlFlowNode successor : basicBlock.getSuccessors()) {
                    graph.addEdge(nodeToIndex.get(basicBlock) + "->" + nodeToIndex.get(successor),
                            String.valueOf(nodeToIndex.get(basicBlock) + "-" + (basicBlock.getStatementsWithinBlock().split("\n").length - 1)),
                            String.valueOf(nodeToIndex.get(successor) + "-0"), true);
                }
            } else {
                for (ControlFlowNode successor : node.getSuccessors()) {
                    graph.addEdge(nodeToIndex.get(node) + "->" + nodeToIndex.get(successor),
                            nodeToIndex.get(node) + "-0",
                            nodeToIndex.get(successor) + "-0", true);
                }
            }

        }
        return graph;
    }
}
