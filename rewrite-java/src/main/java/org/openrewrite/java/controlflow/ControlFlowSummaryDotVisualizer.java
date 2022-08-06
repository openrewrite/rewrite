package org.openrewrite.java.controlflow;

import java.util.*;

final class ControlFlowSummaryDotVisualizer implements ControlFlowDotFileGenerator {
    @Override
    public String visualizeAsDotfile(String name, ControlFlowSummary summary) {
        StringBuilder sb = new StringBuilder(" digraph ").append(name).append(" {\n");
        sb.append("\trankdir = TB");
        final Map<ControlFlowNode, Integer> abstractToVisualNodeMapping = new IdentityHashMap<>(summary.getAllNodes().size());
        final List<ControlFlowNode> allNodes = new ArrayList<>(summary.getAllNodes());
        for (int i = 0; i < allNodes.size(); i++) {
            ControlFlowNode node = allNodes.get(i);
            abstractToVisualNodeMapping.put(node, i);
            final String shape;
            if (node instanceof ControlFlowNode.Start || node instanceof ControlFlowNode.End) {
                shape = "circle";
            } else if (node instanceof ControlFlowNode.ConditionNode) {
                shape = "diamond";
            } else {
                shape = "box";
            }
            sb.append("\n\t").append(i).append(" [shape=").append(shape).append(", label=\"").append(node.toVisualizerString().replace("\n", "\\l")).append("\"];");
        }
        for (ControlFlowNode node : allNodes) {
            if (node instanceof ControlFlowNode.ConditionNode) {
                ControlFlowNode.ConditionNode cn = (ControlFlowNode.ConditionNode) node;
                sb.append("\n\t").append(abstractToVisualNodeMapping.get(node)).append(" -> ").append(abstractToVisualNodeMapping.get(cn.getTruthySuccessor())).append(" [label=\"true\", color=\"green\"];");
                sb.append("\n\t").append(abstractToVisualNodeMapping.get(node)).append(" -> ").append(abstractToVisualNodeMapping.get(cn.getFalsySuccessor())).append(" [label=\"false\", color=\"red\"];");
            } else if (node instanceof ControlFlowNode.BasicBlock) {
                ControlFlowNode.BasicBlock bb = (ControlFlowNode.BasicBlock) node;
                for (ControlFlowNode successor : bb.getSuccessors()) {
                    sb.append("\n\t").append(abstractToVisualNodeMapping.get(node)).append(" -> ").append(abstractToVisualNodeMapping.get(successor)).append(";");
                }
            }
        }

        sb.append('\n').append('}');
        return sb.toString();
    }
}
