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
    static {
        System.setProperty("org.graphstream.ui", "swing");
        System.setProperty("sun.java2d.uiScale", "1.0");
    }

    private final Graph graph;
    private JTextArea codeArea;

    private final Map<Node, ControlFlowNode> nodeToNode;

    boolean loop;



    public GraphShower(Map<ControlFlowNode, Integer> nodeToIndex) {
        ControlFlowGraph cfg = new ControlFlowGraph.ControlFlowGraphBuilder()
                .nodeToIndex(nodeToIndex)
                .expanded(false)
                .build();

        graph = cfg.loadGraph();

        nodeToNode = cfg.getAbstractToVisualNodeMapping();
        loop = true;
    }


    public void runGraph() {
        JFrame frame = new JFrame();
        JPanel mainPanel = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(640, 480);
            }
        };
        mainPanel.setLayout(new BorderLayout());
        codeArea = new JTextArea();
        codeArea.setEditable(false);

        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(mainPanel);

        Viewer viewer = new SwingViewer(graph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);


        ViewPanel viewPanel = (ViewPanel) viewer.addDefaultView(false);
        viewer.getDefaultView().enableMouseOptions();
        viewer.enableAutoLayout();

        mainPanel.add(viewPanel, BorderLayout.CENTER);
        mainPanel.add(codeArea, BorderLayout.NORTH);
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
                codeArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
                codeArea.setForeground(Color.white);
                codeArea.setBackground(Color.BLACK);
            } else {
                codeArea.setForeground(Color.BLACK);
                codeArea.setText("--Select a Basic Block--");
                codeArea.setBackground(Color.WHITE);
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
