package com.payneteasy.firewall;

import com.payneteasy.firewall.dao.ConfigDaoYaml;
import com.payneteasy.firewall.dao.IConfigDao;
import com.payneteasy.firewall.l3.CreateL3Diagram;

import java.io.File;
import java.io.IOException;

public class MainL3Diagram {

    public static void main(String[] args) throws IOException {
        File configDir = new File(args[0]);
        if(!configDir.exists()) throw new IllegalStateException("Config dir "+configDir.getAbsolutePath()+" is not exists");

        IConfigDao configDao = new ConfigDaoYaml(configDir);

        CreateL3Diagram creator = new CreateL3Diagram();
        creator.create(configDao);

    }

}
