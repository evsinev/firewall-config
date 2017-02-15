package com.payneteasy.firewall.l2.editor.actions;

public interface IPointSaver {

    void save(String aName, int aX, int aY);

    IPointSaver createChild(String aName);

}
