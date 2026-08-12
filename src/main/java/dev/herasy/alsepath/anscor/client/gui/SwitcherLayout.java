package dev.herasy.alsepath.anscor.client.gui;

public final class SwitcherLayout {

    public final int vanillaCount;
    public final int customCount;
    public final int totalItems;

    public final int contentWidth;
    public final int totalBoxWidth;

    public boolean justBuilt;

    public SwitcherLayout(
            int vanillaCount,
            int customCount,
            int totalItems,
            int contentWidth,
            int totalBoxWidth
    ) {
        this.vanillaCount = vanillaCount;
        this.customCount = customCount;
        this.totalItems = totalItems;
        this.contentWidth = contentWidth;
        this.totalBoxWidth = totalBoxWidth;
        this.justBuilt = true;
    }
}