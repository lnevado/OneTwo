package com.nicue.onetwo.data.dice;

/** One persisted die: how many faces it has, its palette colour, and its optional label. */
public class DieRecord {
    private final int faces;
    private final int colorIndex;
    private final String label;

    public DieRecord(int faces, int colorIndex, String label) {
        this.faces = faces;
        this.colorIndex = colorIndex;
        this.label = label == null ? "" : label;
    }

    public int getFaces() {
        return faces;
    }

    public int getColorIndex() {
        return colorIndex;
    }

    public String getLabel() {
        return label;
    }
}
