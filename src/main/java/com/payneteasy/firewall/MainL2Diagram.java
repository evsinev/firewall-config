package com.payneteasy.firewall;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.l2.CreateGraph;
import com.payneteasy.firewall.l2.NodePositions;
import com.payneteasy.firewall.l2.viewer.SwingViewer;
import y.view.Graph2D;

import java.io.File;
import java.io.IOException;

public class MainL2Diagram {

    public static void main(String[] args) throws IOException {
        File configDir = new File(args[0]);
        if(!configDir.exists()) throw new IllegalStateException("Config dir "+configDir.getAbsolutePath()+" is not exists");

        IConfigDao configDao = new ConfigDaoYaml(configDir);
        NodePositions nodePositions = new NodePositions(new File(configDir, "l2positions.properties"));


        SwingViewer viewer = new SwingViewer(nodePositions);
        Graph2D graph = viewer.getGraph();
        CreateGraph creator = new CreateGraph(nodePositions);
        creator.populateGraph(configDao, graph);
        viewer.show();

    }
}
