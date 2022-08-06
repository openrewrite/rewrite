package org.openrewrite.java.controlflow;

import lombok.Value;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

final class ControlFlowSummaryDotVisualizer implements ControlFlowDotFileGenerator {
    @Override
    public String visualizeAsDotfile(String name, ControlFlowSummary summary) {
        StringBuilder sb = new StringBuilder("digraph ").append(name).append(" {\n");
        sb.append("    rankdir = TB;");
        final Map<ControlFlowNode, Integer> abstractToVisualNodeMapping = new IdentityHashMap<>(summary.getAllNodes().size());
        // Create a predictable iteration order to make unit tests consistent
        List<NodeToNodeText> nodeToNodeText =
                summary
                        .getAllNodes()
                        .stream()
                        .map(NodeToNodeText::new)
                        .sorted()
                        .collect(Collectors.toList());
        for (int i = 0; i < nodeToNodeText.size(); i++) {
            NodeToNodeText toNodeText = nodeToNodeText.get(i);
            ControlFlowNode node = toNodeText.node;
            String nodeText = toNodeText.nodeText;
            abstractToVisualNodeMapping.put(node, i);
            final String shape = getShape(node);
            sb.append("\n    ").append(i).append(" [shape=").append(shape).append(", label=\"").append(nodeText).append("\"];");
        }

        for (NodeToNodeText toNodeText : nodeToNodeText) {
            ControlFlowNode node = toNodeText.node;
            if (node instanceof ControlFlowNode.ConditionNode) {
                ControlFlowNode.ConditionNode cn = (ControlFlowNode.ConditionNode) node;
                sb.append("\n    ").append(abstractToVisualNodeMapping.get(node)).append(" -> ").append(abstractToVisualNodeMapping.get(cn.getTruthySuccessor())).append(" [label=\"True\", color=\"green\" fontcolor=\"green\"];");
                sb.append("\n    ").append(abstractToVisualNodeMapping.get(node)).append(" -> ").append(abstractToVisualNodeMapping.get(cn.getFalsySuccessor())).append(" [label=\"False\", color=\"red\" fontcolor=\"red\"];");
            } else {
                for (ControlFlowNode successor : node.getSuccessors()) {
                    sb.append("\n    ").append(abstractToVisualNodeMapping.get(node)).append(" -> ").append(abstractToVisualNodeMapping.get(successor)).append(";");
                }
            }
        }
        sb.append('\n').append('}');
        return sb.toString();
    }

    @Value
    private static class NodeToNodeText implements Comparable<NodeToNodeText> {
        ControlFlowNode node;
        String nodeText;

        NodeToNodeText(ControlFlowNode node) {
            this.node = node;
            this.nodeText = node.toVisualizerString().replace("\"", "\\\"").replace("\n", "\\l");
        }

        @Override
        public int compareTo(@NotNull NodeToNodeText o) {
            if (this.equals(o)) {
                return 0;
            }
            if (node instanceof ControlFlowNode.Start || o.node instanceof ControlFlowNode.End) {
                return -1;
            } else if (node instanceof ControlFlowNode.End || o.node instanceof ControlFlowNode.Start) {
                return 1;
            } else {
                return nodeText.compareTo(o.nodeText);
            }
        }
    }

    private static String getShape(ControlFlowNode node) {
        if (node instanceof ControlFlowNode.Start || node instanceof ControlFlowNode.End) {
            return "circle";
        } else if (node instanceof ControlFlowNode.ConditionNode) {
            return "diamond";
        } else {
            return "box";
        }
    }
}
