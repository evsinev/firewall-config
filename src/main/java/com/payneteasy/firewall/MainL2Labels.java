package com.payneteasy.firewall;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.l2.editor.create.L2GraphCreator;
import com.payneteasy.firewall.l2.editor.model.Link;
import com.payneteasy.firewall.service.model.LinkInfo;
import com.payneteasy.firewall.shell.AbstractDirPrefixFilterCommand;
import com.payneteasy.firewall.util.MustacheFilePrinter;
import picocli.CommandLine;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@CommandLine.Command(
        name = "MainL2Labels"
        , mixinStandardHelpOptions = true
        , description = "Generates L2 labels"
)
public class MainL2Labels extends AbstractDirPrefixFilterCommand {

    @Override
    public Integer call() throws Exception {
        File configDir = dir;
        if(!configDir.exists()) throw new IllegalStateException("Config dir "+configDir.getAbsolutePath()+" is not exists");

        IConfigDao     configDao = new ConfigDaoYaml(configDir);
        MainL2Labels   main      = new MainL2Labels();
        List<LinkInfo> infos     = sort(main.createLinkInfos(configDao, configDir));
        List<Row>      rows      = createRows(infos);

        main.printLinkInfos(rows);

        return 0;
    }

    public static void main(String[] args) throws IOException {
        System.exit(
                new CommandLine(
                        new MainL2Labels()
                ).execute(args)
        );
    }

    private static List<LinkInfo> sort(List<LinkInfo> aLinks) {
        aLinks.sort(Comparator.comparing(left -> (left.leftAddress + " " + left.rightAddress + " " + left.colorHex)));
        return aLinks;
    }

    private static List<Row> createRows(List<LinkInfo> aLinks) {
        System.out.println("Creating rows ...");
        List<Row> rows = new ArrayList<>();

        Row currentRow = null;
        for(int i=0; i<aLinks.size(); i++) {
            if(i % 6 == 0) {
                currentRow = new Row();
                rows.add(currentRow);
            }
            currentRow.addLink(aLinks.get(i));
        }
        return rows;
    }

    private void printLinkInfos(List<Row> aRows) {
        System.out.println("Creating file ...");
        MustacheFilePrinter filePrinter = new MustacheFilePrinter("ethernet-labels.html.mustache");
        filePrinter.add("rows", aRows);
        filePrinter.write(new File("target/labels.html"));
    }

    List<LinkInfo> createLinkInfos(IConfigDao aDao, File aConfigDir) {
        System.out.println("Creating link infos ...");
        L2GraphCreator graphCreator = new L2GraphCreator(aDao, aConfigDir, prefix);
        graphCreator.create(getFilterArray());
        graphCreator.getHosts();
        
        List<Link> links = graphCreator.getLinks().getLinks();

        List<LinkInfo> infos = new ArrayList<>();
        for (Link link : links) {
            infos.add(replaceNames(link.createLinkInfo()));
        }

        return infos;


    }

    private LinkInfo replaceNames(LinkInfo aLinkInfo) {
        return new LinkInfo(replaceName(aLinkInfo.leftAddress), replaceName(aLinkInfo.rightAddress), aLinkInfo.colorHex);
    }

    private String replaceName(String aText) {

        String ret = aText
//                .replace("sw-01-01"  , "sw-11")
//                .replace("sw-01-02"  , "sw-12")
//                .replace("sw-02-02"  , "sw-22")
//                .replace("sw-02-01"  , "sw-21")
                .replace("sw-inet-01", "sw-ine-1")
                .replace("sw-inet-02", "sw-ine-2")
                .replace("sw-ipmi-1" , "sw-ipm-1")
                .replace("sw-ipmi-2" , "sw-ipm-2")
                .replace("internet"  , "inet")
                .replace("fw-ipmi-"  , "ifw-")
                .replace("ipmi-mc-"  , "imc-")
                ;

        if(ret.length() > 12) {
            System.out.println("Long name " + ret +" len = " + ret.length());
        }
        return ret;
        
    }

    private static class Row {

        private final List<LinkInfo> cells = new ArrayList<>();

        public void addLink(LinkInfo linkInfo) {
            cells.add(linkInfo);
        }

        public List<LinkInfo> getCells() {
            return cells;
        }
    }

}
