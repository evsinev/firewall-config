package com.payneteasy.firewall.l2.viewer;

import com.payneteasy.firewall.l2.NodePositions;
import y.view.*;

import javax.swing.*;
import java.awt.*;

public class SwingViewer {
    JFrame frame;
    /** The yFiles view component that displays (and holds) the graph. */
    Graph2DView view;
    /** The yFiles graph type. */
    Graph2D graph;

    final NodePositions nodePositions;

    public SwingViewer(NodePositions aPositions, Dimension size, String title) {
        view = createGraph2DView();
        graph = view.getGraph2D();
        nodePositions = aPositions;
        frame = createApplicationFrame(size, title, view);
        configureDefaultRealizers(graph);
    }

    public SwingViewer(NodePositions aPositions) {
        this(aPositions, getMaxDimension(), "");
        frame.setTitle(getClass().getName());
    }

    private static Dimension getMaxDimension() {
        return Toolkit.getDefaultToolkit().getScreenSize();
    }

    private Graph2DView createGraph2DView() {
        Graph2DView view = new Graph2DView();
        view.setAntialiasedPainting(true);

//        new Graph2DViewMouseWheelZoomListener().addToCanvas(view);
//        new Graph2DViewMouseWheelScrollListener().addToCanvas(view);

        EditMode editMode = new EditMode();
        // Enables convenient switching between group node and folder node presentation.
        editMode.getMouseInputMode().setNodeSearchingEnabled(true);
        view.addViewMode(editMode);

        return view;
    }

    /** Creates a JFrame that will show the demo graph. */
    private JFrame createApplicationFrame(Dimension size, String title, JComponent view) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(size);
        // Add the given view to the panel.
        panel.add(view, BorderLayout.CENTER);
        // Add a toolbar with some actions to the panel, too.
        panel.add(createToolBar(), BorderLayout.NORTH);
        JFrame frame = new JFrame(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getRootPane().setContentPane(panel);
        frame.pack();
        frame.setLocationRelativeTo(null);
        return frame;
    }

    protected void configureDefaultRealizers(Graph2D graph) {
        // Add an arrowhead decoration to the target side of the edges.
        graph.getDefaultEdgeRealizer().setTargetArrow(Arrow.STANDARD);
        // Set the node size and some other graphical properties.
        NodeRealizer defaultNodeRealizer = graph.getDefaultNodeRealizer();
        defaultNodeRealizer.setSize(50, 30);
        defaultNodeRealizer.setFillColor(Color.ORANGE);
        defaultNodeRealizer.setLineType(LineType.DASHED_1);
    }

    /** Creates a toolbar for this demo. */
    protected JToolBar createToolBar() {
        JToolBar toolbar = new JToolBar();

        toolbar.add(new RouterAction(getView()));
        toolbar.addSeparator();

        toolbar.add(new FitContentAction(getView()));
        toolbar.add(new ZoomAction(getView(), 1.25));
        toolbar.add(new ZoomAction(getView(), 0.8));

        toolbar.addSeparator();
        toolbar.add(new SaveAction(view, nodePositions));

        toolbar.addSeparator();

//        toolbar.add(new HierarchicLayoutAction(getView()));
//        toolbar.add(new OrthogonalLayoutAction(getView()));
//        toolbar.add(new SmartOrganicLayoutAction(getView()));



        return toolbar;
    }

    public void show() {
        frame.setVisible(true);
    }

    public Graph2DView getView() {
        return view;
    }

    public Graph2D getGraph() {
        return graph;
    }


}
