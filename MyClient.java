package dev.myclient;

import dev.myclient.config.ConfigManager;
import dev.myclient.gui.ClickGuiScreen;
import dev.myclient.hud.Hud;
import dev.myclient.module.Module;
import dev.myclient.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import java.util.*;

public class MyClient implements ClientModInitializer {
    private static KeyBinding guiKey;
    private final Map<Module, Boolean> down = new HashMap<>();

    @Override
    public void onInitializeClient() {
        ModuleManager.get().init();
        ConfigManager.load("default");

        KeyBinding.Category cat = KeyBinding.Category.create(Identifier.of("myclient", "main"));
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.myclient.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, cat));

        ClientTickEvents.END_CLIENT_TICK.register(this::tick);
        HudElementRegistry.attachElementAfter(VanillaHudElements.CHAT,
                Identifier.of("myclient", "overlay"), (ctx, tickCounter) -> Hud.render(ctx));
        ClientLifecycleEvents.CLIENT_STOPPING.register(c ->
                ConfigManager.save("default", new LinkedHashSet<>(ConfigManager.SECTIONS)));
    }

    private void tick(MinecraftClient mc) {
        ModuleManager.get().globalTick();
        while (guiKey.wasPressed()) if (mc.currentScreen == null) mc.setScreen(new ClickGuiScreen());
        if (mc.player == null) return;

        if (mc.currentScreen == null) {
            long w = GLFW.glfwGetCurrentContext();
            for (Module m : ModuleManager.get().all()) {
                if (m.key < 0) continue;
                boolean p = GLFW.glfwGetKey(w, m.key) == GLFW.GLFW_PRESS;
                if (p && !down.getOrDefault(m, false)) m.toggle();
                down.put(m, p);
            }
        }
        ModuleManager.get().tick();
    }
}
