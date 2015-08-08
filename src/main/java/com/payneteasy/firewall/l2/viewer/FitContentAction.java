package com.payneteasy.firewall.l2.viewer;

import y.view.Graph2DView;

import javax.swing.*;
import java.awt.event.ActionEvent;

/** Action that fits the content nicely inside the view. */
class FitContentAction extends AbstractAction {
    Graph2DView view;

    public FitContentAction(Graph2DView view) {
        super("Fit Content");
        this.view = view;
        this.putValue(Action.SHORT_DESCRIPTION, "Fit Content");
    }

    public void actionPerformed(ActionEvent e) {
        view.fitContent();
        view.updateView();
    }
}
