package org.openrewrite.java.controlflow;

import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.swing_viewer.ViewPanel;
import org.graphstream.ui.view.Viewer;
import org.graphstream.ui.view.ViewerListener;
import org.graphstream.ui.view.ViewerPipe;

import javax.swing.*;
import java.awt.*;
import java.util.Map;


public class GraphShower {
    private Graph graph;
    private JFrame mainFrame;
    private JPanel mainPanel;
    private JTextArea codeArea;

    private Map<Node, ControlFlowNode> nodeToNode;

    boolean loop;

    public GraphShower(Map<ControlFlowNode, Integer> nodeToIndex) {
        System.setProperty("org.graphstream.ui", "swing");
        System.setProperty("sun.java2d.uiScale", "1.0");

        ControlFlowGraph cfg = new ControlFlowGraph.ControlFlowGraphBuilder()
                .nodeToIndex(nodeToIndex)
                .expanded(true)
                .build();

        graph = cfg.loadGraph();

//        graph = ControlFlowGraph.buildGraph(nodeToIndex);
        nodeToNode = cfg.getAbstractToVisualNodeMapping();
        loop = true;
    }


    public void runGraph() {
        mainFrame = new JFrame();
        JFrame frame = mainFrame;
        mainPanel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(640, 480);
            }
        };
        mainPanel.setLayout(new BorderLayout());
        JPanel panel = mainPanel;
        codeArea = new JTextArea();
        codeArea.setEditable(false);

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        panel.setBorder(BorderFactory.createLineBorder(Color.blue, 5));
        mainFrame.add(panel);
//          graph.display();

        Viewer viewer = new SwingViewer(graph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);


        ViewPanel viewPanel = (ViewPanel) viewer.addDefaultView(false);
        viewer.getDefaultView().enableMouseOptions();
        viewer.enableAutoLayout();

        panel.add(viewPanel, BorderLayout.CENTER);
        panel.add(codeArea, BorderLayout.NORTH);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        ViewerPipe fromViewer = viewer.newViewerPipe();
        fromViewer.addViewerListener(new MouseOptions());
        fromViewer.addSink(graph);

        while (loop) {
            fromViewer.pump();
            if (!frame.isVisible()) {
                break;
            }
        }
    }

    public class MouseOptions implements ViewerListener {
        public void viewClosed(String id) {
            loop = false;
        }

        public void buttonPushed(String id) {
            if (nodeToNode.get(graph.getNode(id)) instanceof ControlFlowNode.BasicBlock) {
                ControlFlowNode.BasicBlock basicBlock = (ControlFlowNode.BasicBlock) nodeToNode.get(graph.getNode(id));
                codeArea.setText(basicBlock.getStatementsWithinBlock());
            } else {
                codeArea.setText("--Select a Basic Block--");
            }

            codeArea.setEditable(false);
        }

        public void buttonReleased(String id) {
            System.out.println("Button released on node " + id);
        }

        public void mouseOver(String id) {
            System.out.println("Need the Mouse Options to be activated");
        }

        public void mouseLeft(String id) {
            System.out.println("Need the Mouse Options to be activated");
        }
    }
}
