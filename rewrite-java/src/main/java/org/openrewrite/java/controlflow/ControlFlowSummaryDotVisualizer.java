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

import lombok.Value;
import org.openrewrite.internal.lang.NonNull;

import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

final class ControlFlowSummaryDotVisualizer implements ControlFlowDotFileGenerator {
    @Override
    public String visualizeAsDotfile(String name, boolean darkMode, ControlFlowSummary summary) {
        StringBuilder sb = new StringBuilder("digraph ").append(name).append(" {\n");
        sb.append("    rankdir = TB;\n");
        sb.append("    edge [fontname=Arial];");
        if (darkMode) {
            sb.append("\n    graph [bgcolor=black];\n" +
                    "    node [color=white, fontcolor=whitesmoke];\n" +
                    "    edge [fontname=Arial; color=whitesmoke];");
        }
        final Map<ControlFlowNode, Integer> abstractToVisualNodeMapping = new IdentityHashMap<>(summary.getAllNodes().size());
        // Create a predictable iteration order to make unit tests consistent
        List<NodeToNodeText> nodeToNodeText =
                summary
                        .getAllNodes()
                        .stream()
                        .map(NodeToNodeText::new)
                        .sorted()
                        .collect(Collectors.toList());
        int vizSrc = -1, vizSink = -1;
        for (int i = 0; i < nodeToNodeText.size(); i++) {
            NodeToNodeText toNodeText = nodeToNodeText.get(i);
            ControlFlowNode node = toNodeText.node;
            String nodeText = toNodeText.nodeText;
            abstractToVisualNodeMapping.put(node, i);
            if (node instanceof ControlFlowNode.GraphTerminator) {
                ControlFlowNode.GraphTerminator terminator = (ControlFlowNode.GraphTerminator) node;
                if (terminator.getGraphType().equals(ControlFlowNode.GraphType.METHOD_BODY_OR_STATIC_INITIALIZER_OR_INSTANCE_INITIALIZER)) {
                    if (node instanceof ControlFlowNode.Start) {
                        vizSrc = i;
                    } else if (node instanceof ControlFlowNode.End) {
                        vizSink = i;
                    }
                }
            }
            final String shape = getShape(node);
            final String fontName = getFont(node);
            sb.append("\n    ").append(i).append(" [shape=").append(shape)
                    .append(", label=\"").append(nodeText)
                    .append("\", fontname=\"").append(fontName)
                    .append("\"];");
        }

        for (NodeToNodeText toNodeText : nodeToNodeText) {
            ControlFlowNode node = toNodeText.node;
            if (node instanceof ControlFlowNode.ConditionNode) {
                ControlFlowNode.ConditionNode cn = (ControlFlowNode.ConditionNode) node;
                sb.append("\n    ").append(abstractToVisualNodeMapping.get(node))
                        .append(" -> ").append(abstractToVisualNodeMapping.get(cn.getTruthySuccessor()));
                if (!cn.isAlwaysFalse()) {
                    sb.append(" [label=\"True\", ");
                    if (darkMode) {
                        sb.append("color=\"darkgreen\" fontcolor=\"darkgreen\"];");
                    } else {
                        sb.append("color=\"green3\" fontcolor=\"green3\"];");
                    }
                } else {
                    sb.append(" [label=\"Unreachable\", color=\"grey\" fontcolor=\"grey\" style=dashed];");
                }
                sb.append("\n    ").append(abstractToVisualNodeMapping.get(node))
                        .append(" -> ").append(abstractToVisualNodeMapping.get(cn.getFalsySuccessor()));
                if (!cn.isAlwaysTrue()) {
                    sb.append(" [label=\"False\", color=\"red\" fontcolor=\"red\"];");
                } else {
                    sb.append(" [label=\"Unreachable\", color=\"grey\" fontcolor=\"grey\" style=dashed];");
                }
            } else {
                for (ControlFlowNode successor : node.getSuccessors()) {
                    sb.append("\n    ").append(abstractToVisualNodeMapping.get(node))
                            .append(" -> ").append(abstractToVisualNodeMapping.get(successor)).append(";");
                }
            }
        }
        if (vizSrc != -1 && vizSink != -1) {
            sb.append("\n    {rank=\"src\";").append(vizSrc).append("};\n");
            sb.append("    {rank=\"sink\";").append(vizSink).append("};");
        }
        sb.append('\n').append('}');
        return sb.toString();
    }

    @Value
    private static class NodeToNodeText implements Comparable<NodeToNodeText> {
        private static Comparator<NodeToNodeText> comparator = Comparator
                .comparingInt(NodeToNodeText::comparingType)
                .thenComparing(NodeToNodeText::getNodeText);

        @NonNull ControlFlowNode node;
        String nodeText;

        NodeToNodeText(ControlFlowNode node) {
            this.node = node;
            if (node instanceof ControlFlowNode.BasicBlock) {
                this.nodeText = node.toVisualizerString().replace("\"", "\\\"")
                        .replace("\n", "\\l") + "\\l";
            } else {
                this.nodeText = node.toVisualizerString().replace("\"", "\\\"");
            }

        }

        /**
         * Order by type, then by node text.
         * <p>
         * This makes the test output consistent, even if the order of the nodes changes.
         */
        @Override
        public int compareTo(@NonNull NodeToNodeText o) {
            if (this.equals(o)) {
                return 0;
            }
            return comparator.compare(this, o);
        }

        private int comparingType() {
            if (node instanceof ControlFlowNode.Start) {
                ControlFlowNode.Start start = (ControlFlowNode.Start) node;
                if (ControlFlowNode.GraphType.METHOD_BODY_OR_STATIC_INITIALIZER_OR_INSTANCE_INITIALIZER.equals(start.getGraphType())) {
                    return -2;
                } else {
                    return -1;
                }
            } else if (node instanceof ControlFlowNode.End) {
                ControlFlowNode.End end = (ControlFlowNode.End) node;
                if (ControlFlowNode.GraphType.METHOD_BODY_OR_STATIC_INITIALIZER_OR_INSTANCE_INITIALIZER.equals(end.getGraphType())) {
                    return 2;
                } else {
                    return 1;
                }
            } else {
                return 0;
            }
        }
    }

    private static String getShape(ControlFlowNode node) {
        if (node instanceof ControlFlowNode.Start || node instanceof ControlFlowNode.End) {
            ControlFlowNode.GraphTerminator graphTerminator = (ControlFlowNode.GraphTerminator) node;
            if (ControlFlowNode.GraphType.METHOD_BODY_OR_STATIC_INITIALIZER_OR_INSTANCE_INITIALIZER.equals(graphTerminator.getGraphType())) {
                return "circle";
            } else {
                return "oval";
            }
        } else if (node instanceof ControlFlowNode.ConditionNode) {
            return "diamond";
        } else {
            return "box";
        }
    }

    private static String getFont(ControlFlowNode node) {
        if (node instanceof ControlFlowNode.Start || node instanceof ControlFlowNode.End) {
            return "Arial";
        } else {
            return "Courier";
        }
    }
}
