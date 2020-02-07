package com.payneteasy.firewall;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.dao.model.THost;
import com.payneteasy.firewall.util.MustacheFilePrinter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainHostLabels {

    public static void main(String[] args) throws IOException {
        File configDir = new File(args[0]);
        if(!configDir.exists()) throw new IllegalStateException("Config dir "+configDir.getAbsolutePath()+" is not exists");

        IConfigDao     configDao = new ConfigDaoYaml(configDir);
        List<THost>    hosts  = ((ConfigDaoYaml) configDao).theHosts;

        MainHostLabels main      = new MainHostLabels();
        main.printHostLabels(hosts);
    }

    private void printHostLabels(List<THost> aHosts) {
        for (THost host : aHosts) {
            System.out.println(host.name);
        }

        MustacheFilePrinter filePrinter = new MustacheFilePrinter("host-labels.html.mustache");
        filePrinter.add("rows", createRows(aHosts));
        filePrinter.write(new File("target/hosts.html"));

    }

    private static List<Row> createRows(List<THost> aHosts) {
        System.out.println("Creating rows ...");
        List<Row> rows = new ArrayList<>();

        Row currentRow = null;
        for(int i=0; i<aHosts.size(); i++) {
            if(i % 6 == 0) {
                currentRow = new Row();
                rows.add(currentRow);
            }
            currentRow.add(aHosts.get(i).name);
        }
        return rows;
    }


    static class Row {

        private final List<String> cells = new ArrayList<>();

        public void add(String linkInfo) {
            cells.add(linkInfo);
        }

        public List<String> getCells() {
            return cells;
        }
    }

}
