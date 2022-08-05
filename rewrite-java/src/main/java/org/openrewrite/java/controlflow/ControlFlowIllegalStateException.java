package org.openrewrite.java.controlflow;

import lombok.Value;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

class ControlFlowIllegalStateException extends IllegalStateException {

    ControlFlowIllegalStateException(Message.MessageBuilder message) {
        super(message.build().createMessage());
    }

    ControlFlowIllegalStateException(String message, ControlFlowNode thisNode) {
        this(exceptionMessageBuilder(message).thisNode(thisNode));
    }

    static Message.MessageBuilder exceptionMessageBuilder(String message) {
        return new Message.MessageBuilder(message);
    }
    @Value
    static class Message {
        String message;
        LinkedHashMap<String, ControlFlowNode> nodes;
        Set<ControlFlowNode> predecessors;

        static class MessageBuilder {
            private final String message;
            // LinkedHashMap to preserve order of insertion1
            private final LinkedHashMap<String, ControlFlowNode> nodes = new LinkedHashMap<>();
            private final Set<ControlFlowNode> predecessors = new LinkedHashSet<>();

            private MessageBuilder(String message) {
                this.message = message;
            }

            MessageBuilder thisNode(ControlFlowNode node) {
                return addNode("This", node);
            }

            MessageBuilder current(ControlFlowNode node) {
                return addNode("Current", node);
            }

            MessageBuilder otherNode(ControlFlowNode node) {
                return addNode("Other", node);
            }

            MessageBuilder addPredecessors(ControlFlowNode node) {
                // Don't use the getter as that could throw an exception
                predecessors.addAll(node.predecessors);
                return this;
            }

            MessageBuilder addNode(String name, ControlFlowNode node) {
                nodes.put(name, node);
                return this;
            }

            Message build() {
                return new Message(this.message, this.nodes, this.predecessors);
            }
        }

        private String createMessage() {
            StringBuilder sb = new StringBuilder(message);
            nodes.forEach((key, node) -> {
                sb.append("\n\t").append(key).append(": ").append(node.getClass().getSimpleName()).append(" ").append(node.toDescriptiveString());
            });
            if (!predecessors.isEmpty()) {
                sb.append("\n\tPredecessors: ").append(predecessors.stream().map(ControlFlowNode::toDescriptiveString).reduce("\n\t\t", (a, b) -> a + "\n\t\t" + b));
            }
            return sb.toString();
        }
    }
}
