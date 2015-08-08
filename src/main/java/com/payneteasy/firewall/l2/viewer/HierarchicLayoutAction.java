package com.payneteasy.firewall.l2.viewer;

import y.layout.hierarchic.IncrementalHierarchicLayouter;
import y.view.Graph2DLayoutExecutor;
import y.view.Graph2DView;

import javax.swing.*;
import java.awt.event.ActionEvent;

class HierarchicLayoutAction extends AbstractAction {
    Graph2DView view;

    public HierarchicLayoutAction(Graph2DView view) {
        super("Hierarchic Layout");
        this.view = view;
        this.putValue(Action.SHORT_DESCRIPTION, "Hierarchic Layout");
    }

    public void actionPerformed(ActionEvent e) {
        final Graph2DLayoutExecutor layoutExecutor = new Graph2DLayoutExecutor(Graph2DLayoutExecutor.ANIMATED);
        layoutExecutor.doLayout(view, new IncrementalHierarchicLayouter());
        view.updateView();
    }
}
