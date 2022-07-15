///*
// * Copyright 2022 the original author or authors.
// * <p>
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// * <p>
// * https://www.apache.org/licenses/LICENSE-2.0
// * <p>
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
package org.openrewrite.java.controlflow;

//import org.graphstream.graph.*;
//import org.graphstream.graph.implementations.*;
//import org.graphstream.stream.ProxyPipe;
//import org.graphstream.ui.swing_viewer.ViewPanel;
//import org.graphstream.ui.swing_viewer.util.MouseOverMouseManager;
//import org.graphstream.ui.view.View;
//import org.graphstream.ui.view.Viewer;
//import org.graphstream.ui.view.util.InteractiveElement;
//
//import javax.swing.*;
//import java.awt.*;
//import java.util.EnumSet;
//import java.util.stream.Stream;
//
//public class DisplayControlFlowGraph {
//
//    public static void main(String args[]) {
//        System.setProperty("org.graphstream.ui", "swing");
//
//        Graph graph = new SingleGraph("Tutorial 1");
//
//        Node a = graph.addNode("A");
//        a.setAttribute("ui.style", "shape:circle; text-size:10px;");
//        a.setAttribute("ui.label", "abc A");
//        graph.addNode("B");
//        graph.addNode("C");
////        graph.addEdge("AB", "A", "B");
//        graph.addEdge("BC", "B", "C");
//        graph.addEdge("CA", "C", "A");
//
//        Viewer viewer = graph.display();
////        View view = viewer.addDefaultView();   // false indicates "no JFrame".
////        JFrame myJFrame = new JFrame();
////        myJFrame.add((Component) view);
//        ProxyPipe pipe = viewer.newViewerPipe();
////        viewer.setCloseFramePolicy(Viewer.CloseFramePolicy.CLOSE_VIEWER);
//        viewer.getDefaultView().enableMouseOptions();
//        pipe.addAttributeSink(graph);
//        viewer.getDefaultView().setMouseManager(new MouseOverMouseManager(EnumSet.of(InteractiveElement.EDGE, InteractiveElement.NODE, InteractiveElement.SPRITE)));
//
//
//        while (true) {
//            try {
//                Thread.sleep(100);
//                pipe.pump();
//                viewer.getDefaultView().display(graph, true);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//
//    }
//
//}

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import org.graphstream.graph.*;
import org.graphstream.graph.implementations.*;
import org.graphstream.ui.swing_viewer.SwingViewer;
import org.graphstream.ui.swing_viewer.ViewPanel;
import org.graphstream.ui.view.*;

///** @see https://stackoverflow.com/a/45055683/230513 */
public class DisplayControlFlowGraph {

    public static void main(String args[]) {
        EventQueue.invokeLater(new DisplayControlFlowGraph()::display);
        System.setProperty("org.graphstream.ui", "swing");
        System.setProperty("sun.java2d.uiScale", "1.0");
    }

    Graph graph;
    JFrame mainFrame;
    private void display() {
        mainFrame = new JFrame();
        JFrame frame = mainFrame;
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel(new GridLayout()){
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(640, 480);
            }
        };
        panel.setBorder(BorderFactory.createLineBorder(Color.blue, 5));
        graph = new SingleGraph("Tutorial", false, true);
        graph.addEdge("AB", "A", "B");
        Node a = graph.getNode("A");
        a.setAttribute("xy", 1, 1);
        Node b = graph.getNode("B");
        b.setAttribute("xy", -1, -1);
        Viewer viewer = new SwingViewer(graph, Viewer.ThreadingModel.GRAPH_IN_GUI_THREAD);


        ViewerPipe fromViewer = viewer.newViewerPipe();
        fromViewer.addViewerListener(new ClickHandler());
        fromViewer.addSink(graph);


        ViewPanel viewPanel = (ViewPanel) viewer.addDefaultView(false);
        viewer.getDefaultView().enableMouseOptions();

        panel.add(viewPanel);
        frame.add(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        while(true) {
            fromViewer.pump(); // or fromViewer.blockingPump(); in the nightly builds

            // here your simulation code.

            // You do not necessarily need to use a loop, this is only an example.
            // as long as you call pump() before using the graph. pump() is non
            // blocking.  If you only use the loop to look at event, use blockingPump()
            // to avoid 100% CPU usage. The blockingPump() method is only available from
            // the nightly builds.
        }
    }

    public class ClickHandler implements ViewerListener {


        @Override
        public void viewClosed(String viewName) {

        }

        @Override
        public void buttonPushed(String id) {
            JFrame frame = new JFrame("Graph");
            JPanel panel = new JPanel(new GridLayout()){
                @Override
                public Dimension getPreferredSize() {
                    return new Dimension(640, 480);
                }
            };
            // create a JTextArea
            JTextArea textArea = new JTextArea();
            textArea.setText((String) graph.getNode(id).getAttribute("ui.label"));
            textArea.setEditable(false);
            panel.add(textArea);
            // add the JTextArea to the frame
            frame.add(panel);
            frame.setLocationRelativeTo(mainFrame);
            frame.pack();
            // display the frame
            frame.setSize(300, 300);
            frame.setVisible(true);
        }

        @Override
        public void buttonReleased(String id) {

        }

        @Override
        public void mouseOver(String id) {

        }

        @Override
        public void mouseLeft(String id) {

        }
    }
}
