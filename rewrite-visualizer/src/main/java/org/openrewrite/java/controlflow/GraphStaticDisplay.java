package org.openrewrite.java.controlflow;

import guru.nidi.graphviz.attribute.*;
import guru.nidi.graphviz.engine.*;
import guru.nidi.graphviz.model.CreationContext;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.Node;
import lombok.Getter;
import lombok.var;

import javax.naming.ldap.Control;

import static guru.nidi.graphviz.attribute.Attributes.attr;
import static guru.nidi.graphviz.attribute.Rank.RankDir.LEFT_TO_RIGHT;
import static guru.nidi.graphviz.attribute.Rank.RankDir.TOP_TO_BOTTOM;
import static guru.nidi.graphviz.model.Factory.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class GraphStaticDisplay {


    Map<ControlFlowNode, Node> abstractToVisualNodeMapping;
    Map<ControlFlowNode, Integer> nodeToIndex;
    public GraphStaticDisplay(Map<ControlFlowNode, Integer> nodeToIndex) {
        this.abstractToVisualNodeMapping = new HashMap<>();
        this.nodeToIndex = nodeToIndex;
    }

    public void loadGraph () {
        Graph g = graph("example1").directed()
                .graphAttr().with(Rank.dir(TOP_TO_BOTTOM))
                .nodeAttr().with(Font.name("courier"))
                .linkAttr().with("class", "link-class")
                .linkAttr().with(Font.name("arial"));
        Node n = null;
        for (ControlFlowNode node : nodeToIndex.keySet()) {
            String lbl = nodeToIndex.get(node).toString();
            if (node instanceof ControlFlowNode.BasicBlock)  {
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
                n = node(lbl).with(Shape.CIRCLE, Label.of(start.toString()));
                abstractToVisualNodeMapping.put(start, n);
            } else if (node instanceof ControlFlowNode.End) {
                ControlFlowNode.End end = (ControlFlowNode.End) node;
                n = node(lbl).with(Shape.CIRCLE, Label.of(end.toString()));
                abstractToVisualNodeMapping.put(end, n);
            }
            g = g.with(n);
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

        try {
            Graphviz.useEngine(new GraphvizV8Engine());
            Graphviz.fromGraph(g).render(Format.DOT).toFile(new File("example/ex1.dot"));
            Graphviz.fromGraph(g).render(Format.SVG).toFile(new File("example/ex1.svg"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
