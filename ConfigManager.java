package dev.myclient.config;

import com.google.gson.*;
import dev.myclient.hud.Hud;
import dev.myclient.module.HeaderSetting;
import dev.myclient.module.ModeSetting;
import dev.myclient.module.Module;
import dev.myclient.module.ModuleManager;
import dev.myclient.module.NumberSetting;
import dev.myclient.module.Setting;
import net.fabricmc.loader.api.FabricLoader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class ConfigManager {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final List<String> SECTIONS = List.of("Modules", "HUD", "Theme", "Friends", "Staff", "Waypoints");

    private static Path dir() {
        Path p = FabricLoader.getInstance().getConfigDir().resolve("myclient");
        try { Files.createDirectories(p); } catch (IOException ignored) {}
        return p;
    }

    public static String safe(String n) { return n.replaceAll("[^a-zA-Z0-9_-]", ""); }
    private static String b64(String s) { return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8)); }
    private static JsonObject parseB64(String code) {
        return JsonParser.parseString(new String(Base64.getDecoder().decode(code.trim()), StandardCharsets.UTF_8)).getAsJsonObject();
    }

    // ---------- setari per modul ----------
    private static JsonObject settingsOf(Module m) {
        JsonObject s = new JsonObject();
        for (Setting<?> st : m.settings) if (!(st instanceof HeaderSetting)) s.add(st.name, GSON.toJsonTree(st.value));
        return s;
    }

    @SuppressWarnings("unchecked")
    private static void readSetting(Setting<?> st, JsonElement e) {
        try {
            if (st instanceof NumberSetting n) n.set(e.getAsDouble());
            else if (st instanceof ModeSetting ms) { String v = e.getAsString(); if (ms.modes.contains(v)) ms.value = v; }
            else ((Setting<Object>) st).value = GSON.fromJson(e, st.def.getClass());
        } catch (Exception ignored) {}
    }

    private static void applySettings(Module m, JsonObject s) {
        for (Setting<?> st : m.settings) if (!(st instanceof HeaderSetting) && s.has(st.name)) readSetting(st, s.get(st.name));
    }

    /** Cod de share pentru UN modul. Contine doar setarile lui, nu tasta si nu starea on/off. */
    public static String exportModule(Module m) {
        JsonObject o = new JsonObject();
        o.addProperty("module", m.name);
        o.add("settings", settingsOf(m));
        return b64(GSON.toJson(o));
    }

    public static boolean importModule(Module m, String code) {
        try {
            JsonObject o = parseB64(code);
            if (!m.name.equals(o.get("module").getAsString())) return false;
            applySettings(m, o.getAsJsonObject("settings"));
            return true;
        } catch (Exception e) { return false; }
    }

    // ---------- profile complete ----------
    public static JsonObject snapshot(Set<String> sec) {
        JsonObject root = new JsonObject();
        if (sec.contains("Modules")) {
            JsonObject mods = new JsonObject();
            for (Module m : ModuleManager.get().all()) {
                JsonObject o = new JsonObject();
                o.addProperty("enabled", m.isEnabled());
                o.addProperty("key", m.key);
                o.add("settings", settingsOf(m));
                mods.add(m.name, o);
            }
            root.add("modules", mods);
        }
        if (sec.contains("HUD")) root.add("hud", GSON.toJsonTree(Hud.toggles));
        if (sec.contains("Theme")) {
            JsonObject t = new JsonObject();
            t.addProperty("accent", Theme.accent);
            t.addProperty("opacity", Theme.opacity);
            t.addProperty("radius", Theme.radius);
            root.add("theme", t);
        }
        if (sec.contains("Friends")) root.add("friends", GSON.toJsonTree(FriendManager.friends));
        if (sec.contains("Staff")) root.add("staff", GSON.toJsonTree(StaffManager.staff));
        if (sec.contains("Waypoints")) {
            JsonArray a = new JsonArray();
            for (WaypointManager.Waypoint w : WaypointManager.list) {
                JsonObject o = new JsonObject();
                o.addProperty("n", w.name()); o.addProperty("x", w.x()); o.addProperty("y", w.y());
                o.addProperty("z", w.z()); o.addProperty("d", w.dim());
                a.add(o);
            }
            root.add("waypoints", a);
        }
        return root;
    }

    public static void apply(JsonObject root) {
        if (root.has("modules")) {
            JsonObject mods = root.getAsJsonObject("modules");
            for (Module m : ModuleManager.get().all()) {
                if (!mods.has(m.name)) continue;
                JsonObject o = mods.getAsJsonObject(m.name);
                if (o.has("key")) m.key = o.get("key").getAsInt();
                if (o.has("settings")) applySettings(m, o.getAsJsonObject("settings"));
                if (o.has("enabled")) m.setEnabled(o.get("enabled").getAsBoolean() && m.implemented());
            }
        }
        if (root.has("hud"))
            for (var e : root.getAsJsonObject("hud").entrySet())
                if (Hud.toggles.containsKey(e.getKey())) Hud.toggles.put(e.getKey(), e.getValue().getAsBoolean());
        if (root.has("theme")) {
            JsonObject t = root.getAsJsonObject("theme");
            if (t.has("accent")) Theme.accent = t.get("accent").getAsInt();
            if (t.has("opacity")) Theme.opacity = Math.max(30, Math.min(100, t.get("opacity").getAsInt()));
            if (t.has("radius")) Theme.radius = Math.max(0, Math.min(20, t.get("radius").getAsInt()));
        }
        if (root.has("friends")) {
            FriendManager.friends.clear();
            for (JsonElement e : root.getAsJsonArray("friends")) FriendManager.add(e.getAsString());
        }
        if (root.has("staff")) {
            StaffManager.staff.clear();
            for (JsonElement e : root.getAsJsonArray("staff")) StaffManager.add(e.getAsString());
        }
        if (root.has("waypoints")) {
            WaypointManager.list.clear();
            for (JsonElement e : root.getAsJsonArray("waypoints")) {
                JsonObject o = e.getAsJsonObject();
                WaypointManager.list.add(new WaypointManager.Waypoint(o.get("n").getAsString(), o.get("x").getAsDouble(),
                        o.get("y").getAsDouble(), o.get("z").getAsDouble(), o.get("d").getAsString()));
            }
        }
    }

    public static boolean save(String name, Set<String> sec) {
        try {
            Files.writeString(dir().resolve(safe(name) + ".json"), GSON.toJson(snapshot(sec)));
            return true;
        } catch (Exception e) { return false; }
    }

    public static boolean load(String name) {
        try {
            Path p = dir().resolve(safe(name) + ".json");
            if (!Files.exists(p)) return false;
            apply(JsonParser.parseString(Files.readString(p)).getAsJsonObject());
            return true;
        } catch (Exception e) { return false; }
    }

    public static void delete(String name) {
        try { Files.deleteIfExists(dir().resolve(safe(name) + ".json")); } catch (IOException ignored) {}
    }

    public static List<String> list() {
        try (var s = Files.list(dir())) {
            return s.map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith(".json")).map(n -> n.substring(0, n.length() - 5))
                    .filter(n -> !n.equals("default")).sorted().toList();
        } catch (IOException e) { return List.of(); }
    }

    public static String export(Set<String> sec) { return b64(GSON.toJson(snapshot(sec))); }

    public static boolean importCode(String code) {
        try { apply(parseB64(code)); return true; } catch (Exception e) { return false; }
    }
}
