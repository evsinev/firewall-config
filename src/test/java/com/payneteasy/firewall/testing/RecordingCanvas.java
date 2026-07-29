package com.payneteasy.firewall.testing;

import com.payneteasy.firewall.l2.editor.graphics.ICanvas;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * An ICanvas that records what was drawn instead of rendering it.
 *
 * This is what makes Host/Ports/Port/Link/Links/Hosts.draw testable without Swing:
 * the geometry is asserted on the recorded calls. Text metrics are fixed so
 * Port.calculateWidth/Height are predictable.
 */
public class RecordingCanvas implements ICanvas {

    public static final int TEXT_WIDTH = 20;
    public static final int TEXT_HEIGHT = 10;

    private final List<String> calls;
    private final int offsetX;
    private final int offsetY;

    public RecordingCanvas() {
        this(new ArrayList<>(), 0, 0);
    }

    private RecordingCanvas(List<String> aCalls, int aOffsetX, int aOffsetY) {
        calls = aCalls;
        offsetX = aOffsetX;
        offsetY = aOffsetY;
    }

    @Override
    public void drawText(Color aColor, int aX, int aY, String aName) {
        record("drawText", aX, aY, aName);
    }

    @Override
    public void fillRect(Color aColor, int aX, int aY, int aWidth, int aHeight) {
        record("fillRect", aX, aY, aWidth + "x" + aHeight + " " + hex(aColor));
    }

    @Override
    public void drawRect(Color aColor, int aX, int aY, int aWidth, int aHeight) {
        record("drawRect", aX, aY, aWidth + "x" + aHeight);
    }

    @Override
    public void drawLine(Color aColor, float aWidth, int aX, int aY, int aX2, int aY2) {
        record("drawLine", aX, aY, (aX2 + offsetX) + "," + (aY2 + offsetY) + " w=" + aWidth);
    }

    @Override
    public ICanvas createChild(int aOffsetX, int aOffsetY) {
        return new RecordingCanvas(calls, offsetX + aOffsetX, offsetY + aOffsetY);
    }

    @Override
    public int getTextWidth(String aText) {
        return TEXT_WIDTH;
    }

    @Override
    public int getTextHeight(String aText) {
        return TEXT_HEIGHT;
    }

    /** Every recorded call, in draw order, with child offsets already applied. */
    public List<String> getCalls() {
        return calls;
    }

    public int count(String aOperation) {
        int n = 0;
        for (String call : calls) {
            if (call.startsWith(aOperation + " ")) {
                n++;
            }
        }
        return n;
    }

    public static String hex(Color aColor) {
        return Integer.toHexString(0xffffff & aColor.getRGB());
    }

    private void record(String aOperation, int aX, int aY, String aDetails) {
        calls.add(aOperation + " " + (aX + offsetX) + "," + (aY + offsetY) + " " + aDetails);
    }
}
