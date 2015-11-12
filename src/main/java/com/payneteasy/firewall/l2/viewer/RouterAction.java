package com.payneteasy.firewall.l2.viewer;

import y.layout.router.BusRouter;
import y.view.Graph2DLayoutExecutor;
import y.view.Graph2DView;

import javax.swing.*;
import java.awt.event.ActionEvent;

class RouterAction extends AbstractAction {
    Graph2DView view;

    public RouterAction(Graph2DView view) {
        super("Router");
        this.view = view;
        this.putValue(Action.SHORT_DESCRIPTION, "Router");
    }

    public void actionPerformed(ActionEvent e) {

        final Graph2DLayoutExecutor layoutExecutor = new Graph2DLayoutExecutor(Graph2DLayoutExecutor.ANIMATED);
        layoutExecutor.doLayout(view, new BusRouter());
        view.updateView();
    }
}
