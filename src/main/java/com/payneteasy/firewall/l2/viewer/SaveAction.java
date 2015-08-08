package com.payneteasy.firewall.l2.viewer;

import com.payneteasy.firewall.l2.NodePositions;
import y.base.Node;
import y.base.NodeCursor;
import y.view.Graph2D;
import y.view.Graph2DView;
import y.view.hierarchy.HierarchyManager;

import javax.swing.*;
import java.awt.event.ActionEvent;

class SaveAction extends AbstractAction {
    final Graph2DView view;
    final NodePositions positions;

    public SaveAction(Graph2DView view, NodePositions aNodePositions) {
        super("Save");
        this.view = view;
        this.putValue(Action.SHORT_DESCRIPTION, "Save");
        positions = aNodePositions;
    }

    public void actionPerformed(ActionEvent e) {
        Graph2D graph = view.getGraph2D();
        HierarchyManager manager = graph.getHierarchyManager();
        positions.clear();

        for (NodeCursor nc = graph.nodes(); nc.ok(); nc.next()) {
            Node node = nc.node();
            Node parent = manager.getParentNode(node);
            if(parent!=null) {
                int y = (int) graph.getY(node);
                int x = (int) graph.getX(node);
                String label = graph.getLabelText(node);
                String parentLabel = graph.getLabelText(parent);
                positions.add(parentLabel+"."+ label, x, y);
            }
        }
        positions.save();
    }
}
