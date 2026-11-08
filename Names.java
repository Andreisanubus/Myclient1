package dev.myclient.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Numele din tab list, citite din toString() ca sa mearga indiferent de versiunea GameProfile. */
public final class Names {
    private Names() {}

    private static final Pattern P = Pattern.compile("name='?([^,'\\]}]+)");

    public static String of(PlayerListEntry e) {
        Matcher m = P.matcher(String.valueOf(e.getProfile()));
        return m.find() ? m.group(1).trim() : null;
    }

    public static Set<String> tab(MinecraftClient mc) {
        Set<String> out = new HashSet<>();
        var h = mc.getNetworkHandler();
        if (h == null) return out;
        for (PlayerListEntry e : h.getPlayerList()) {
            String n = of(e);
            if (n != null) out.add(n);
        }
        return out;
    }
}
