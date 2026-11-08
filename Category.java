package dev.myclient.module;

public enum Category {
    BASE_RAID("Base Raid"), DONUT("Donut"), COMBAT("Combat"), RENDER("Render"),
    MOVEMENT("Movement"), WORLD("World"), VISUALS("Visuals"), HUD("HUD"), MISC("Misc");

    public final String label;
    Category(String label) { this.label = label; }
}
