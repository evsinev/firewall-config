package com.payneteasy.firewall.l2.viewer;

import y.layout.CompositeLayouter;
import y.layout.organic.SmartOrganicLayouter;
import y.layout.router.BusRouter;
import y.view.Graph2DLayoutExecutor;
import y.view.Graph2DView;

import javax.swing.*;
import java.awt.event.ActionEvent;

class SmartOrganicLayoutAction extends AbstractAction {
    Graph2DView view;

    public SmartOrganicLayoutAction(Graph2DView view) {
        super("Smart Organic Layout");
        this.view = view;
        this.putValue(Action.SHORT_DESCRIPTION, "Smart Organic Layout");
    }

    public void actionPerformed(ActionEvent e) {
        SmartOrganicLayouter organicLayouter = new SmartOrganicLayouter();
        organicLayouter.setQualityTimeRatio(1.0);
//            organicLayouter.setMaximumDuration(2L * 60L * 1000L);
//            organicLayouter.setMultiThreadingAllowed(true);
        organicLayouter.setAutoClusteringEnabled(true);
        organicLayouter.setAutoClusteringQuality(1);
        organicLayouter.setAutomaticGroupNodeCompactionEnabled(true);
//            organicLayouter.setCompactness(100);
//            organicLayouter.setCompactness(1);
//            organicLayouter.setDeterministic(true);
        organicLayouter.setConsiderNodeLabelsEnabled(true);
//            organicLayouter.setNodeEdgeOverlapAvoided(true);
        organicLayouter.setSmartComponentLayoutEnabled(true);

        Graph2DLayoutExecutor layoutExecutor = new Graph2DLayoutExecutor(Graph2DLayoutExecutor.ANIMATED);
        layoutExecutor.doLayout(view, new CompositeLayouter(new BusRouter(), organicLayouter));

//            layoutExecutor.doLayout(view, organicLayouter);

        view.updateView();
    }
}
