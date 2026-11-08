package dev.myclient.module.modules;

import com.mojang.authlib.GameProfile;
import dev.myclient.module.Category;
import dev.myclient.module.Module;
import dev.myclient.module.NumberSetting;
import dev.myclient.module.Setting;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import java.util.UUID;

/**
 * Camera libera: o entitate-camera zboara, jucatorul tau ramane pe loc (tastele de miscare sunt blocate).
 * Limita: rotirea camerei se actualizeaza la fiecare tick, deci poate parea putin intarziata.
 */
public class Freecam extends Module {
    private final NumberSetting speed, vSpeed;
    private final Setting<Boolean> keepWalking, smooth;
    private OtherClientPlayerEntity cam;
    private double x, y, z, vx, vy, vz;

    public Freecam() {
        super("Freecam", Category.RENDER);
        header("Movement");
        speed = num("Speed", 1.0, 0.1, 5.0, 0.1);
        vSpeed = num("Vertical Speed", 0.8, 0.1, 4.0, 0.1);
        smooth = bool("Smooth Acceleration", true);
        header("Player");
        keepWalking = bool("Keep Walking", false);
    }

    @Override protected void onEnable() {
        if (mc.player == null || mc.world == null) { setEnabled(false); return; }
        cam = new OtherClientPlayerEntity(mc.world, new GameProfile(UUID.randomUUID(), "Freecam"));
        x = mc.player.getX(); y = mc.player.getEyeY() - 1.62; z = mc.player.getZ();
        vx = vy = vz = 0;
        cam.setPosition(x, y, z);
        cam.setYaw(mc.player.getYaw());
        cam.setPitch(mc.player.getPitch());
        mc.world.addEntity(cam);
        mc.setCameraEntity(cam);
    }

    @Override protected void onDisable() {
        if (mc.player != null) mc.setCameraEntity(mc.player);
        if (cam != null && mc.world != null) mc.world.removeEntity(cam.getId(), Entity.RemovalReason.DISCARDED);
        cam = null;
    }

    @Override public void onTick() {
        if (cam == null || mc.player == null) { setEnabled(false); return; }
        var o = mc.options;
        double f = (o.forwardKey.isPressed() ? 1 : 0) - (o.backKey.isPressed() ? 1 : 0);
        double s = (o.leftKey.isPressed() ? 1 : 0) - (o.rightKey.isPressed() ? 1 : 0);
        double v = (o.jumpKey.isPressed() ? 1 : 0) - (o.sneakKey.isPressed() ? 1 : 0);
        double yaw = Math.toRadians(mc.player.getYaw()), pit = Math.toRadians(mc.player.getPitch());
        double tx = (-Math.sin(yaw) * Math.cos(pit) * f + Math.cos(yaw) * s) * speed.get();
        double ty = -Math.sin(pit) * f * speed.get() + v * vSpeed.get() * 0.5;
        double tz = (Math.cos(yaw) * Math.cos(pit) * f + Math.sin(yaw) * s) * speed.get();
        double k = smooth.value ? 0.35 : 1.0;
        vx += (tx - vx) * k; vy += (ty - vy) * k; vz += (tz - vz) * k;
        x += vx; y += vy; z += vz;
        cam.setPosition(x, y, z);
        cam.setYaw(mc.player.getYaw());
        cam.setPitch(mc.player.getPitch());
        if (!keepWalking.value) {
            o.forwardKey.setPressed(false); o.backKey.setPressed(false);
            o.leftKey.setPressed(false); o.rightKey.setPressed(false);
            o.jumpKey.setPressed(false); o.sneakKey.setPressed(false);
        }
    }
}
