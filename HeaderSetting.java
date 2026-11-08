package dev.myclient.module;

/** Titlu de grup in lista de setari (nu se salveaza in config). */
public class HeaderSetting extends Setting<String> {
    public HeaderSetting(String title) { super(title, title); }
}
