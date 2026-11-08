package dev.myclient.module;

import net.minecraft.text.Text;

/** Modul inregistrat in GUI cu setarile lui, dar fara logica inca. Spec: "b:Nume:true", "n:Nume:def:min:max:step", "m:Nume:def:A|B|C". */
public class Stub extends Module {
    public Stub(String name, Category cat, String... spec) {
        super(name, cat);
        for (String s : spec) {
            String[] p = s.split(":");
            switch (p[0]) {
                case "b" -> bool(p[1], Boolean.parseBoolean(p[2]));
                case "n" -> num(p[1], Double.parseDouble(p[2]), Double.parseDouble(p[3]), Double.parseDouble(p[4]), Double.parseDouble(p[5]));
                case "m" -> mode(p[1], p[2], p[3].split("\\|"));
                default -> { }
            }
        }
    }

    @Override public boolean implemented() { return false; }

    @Override protected void onEnable() {
        if (mc.player != null) mc.player.sendMessage(Text.literal("[" + name + "] nu e implementat inca"), true);
        setEnabled(false);
    }
}
