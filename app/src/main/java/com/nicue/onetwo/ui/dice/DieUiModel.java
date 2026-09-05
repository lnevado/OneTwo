package com.nicue.onetwo.ui.dice;

public class DieUiModel {
    private final long id;
    private final int faces;
    private final int value;
    private final boolean locked;
    private final int colorIndex;
    private final String label;

    public DieUiModel(long id, int faces, int value, boolean locked, int colorIndex, String label) {
        this.id = id;
        this.faces = faces;
        this.value = value;
        this.locked = locked;
        this.colorIndex = colorIndex;
        this.label = label == null ? "" : label;
    }

    public long getId() {
        return id;
    }

    public int getFaces() {
        return faces;
    }

    public int getValue() {
        return value;
    }

    public boolean isLocked() {
        return locked;
    }

    public int getColorIndex() {
        return colorIndex;
    }

    public String getLabel() {
        return label;
    }

    public boolean hasLabel() {
        return !label.isEmpty();
    }
}
