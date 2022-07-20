package org.openrewrite.java.dataflow.analysis;

import org.openrewrite.Cursor;
import org.openrewrite.java.controlflow.ControlFlow;
import org.openrewrite.java.controlflow.ControlFlowNode;
import org.openrewrite.java.dataflow.analysis.SinkFlow;

import java.util.ArrayList;
import java.util.List;

public class DataFlowGraph {

    private SinkFlow<?, ?> flow;

    public DataFlowGraph(SinkFlow<?, ?> flow) {
        this.flow = flow;
    }

    public void getFlows() {
        for (List<Cursor> l : flow.getFlows()) {
            for (Cursor o : l) {
//                ControlFlowNode.BasicBlock b = new ControlFlowNode.BasicBlock()
                System.out.println(o.getValue().toString());
            }
            System.out.println();
        }
    }






}
