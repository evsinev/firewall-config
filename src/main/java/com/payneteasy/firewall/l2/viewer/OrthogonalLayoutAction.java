package com.payneteasy.firewall.l2.viewer;

import y.layout.CompositeLayouter;
import y.layout.organic.SplitEdgeLayoutStage;
import y.layout.router.BusRouter;
import y.view.Graph2DLayoutExecutor;
import y.view.Graph2DView;

import javax.swing.*;
import java.awt.event.ActionEvent;

class OrthogonalLayoutAction extends AbstractAction {
    Graph2DView view;

    public OrthogonalLayoutAction(Graph2DView view) {
        super("Bus Layout");
        this.view = view;
        this.putValue(Action.SHORT_DESCRIPTION, "Bus Layout");
    }

    public void actionPerformed(ActionEvent e) {

        final Graph2DLayoutExecutor layoutExecutor = new Graph2DLayoutExecutor(Graph2DLayoutExecutor.ANIMATED);
//            OrthogonalLayouter layouter = new OrthogonalLayouter();
//            layouter.setAlignDegreeOneNodesEnabled(false);
//            layouter.setIntegratedEdgeLabelingEnabled(false);
//            layouter.setUseSpacePostprocessing(false);
//            layouter.setAlignDegreeOneNodesEnabled(false);
//            layouter.setConsiderNodeLabelsEnabled(false);
//            layouter.setAlignDegreeOneNodesEnabled(false);
//            layouter.setDebugCompaction(false);
//            layouter.setIntegratedEdgeLabelingEnabled(false);
//            layouter.setPerceivedBendsOptimizationEnabled(false);
//            layouter.setUseLengthReduction(false);
//            layouter.setGroupNodeHider(new BusRouter());
//            layouter.setUseSketchDrawing(true);
//            layouter.setGrid(10);


//            layoutExecutor.doLayout(view, new BusRouter());
        layoutExecutor.doLayout(view, new CompositeLayouter(new BusRouter(), new SplitEdgeLayoutStage()));
        view.updateView();
    }
}
