package com.payneteasy.firewall.l2.editor.model;

import com.payneteasy.firewall.l2.editor.actions.IPointSaver;

import java.awt.*;

public interface INode {

    void moveTo(int aX, int aY);

    Point createOffset(int aX, int aY);

    void save(IPointSaver aSaver);
}
