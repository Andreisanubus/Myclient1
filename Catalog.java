package dev.myclient.module;

import dev.myclient.module.modules.*;
import java.util.List;

/** Toate modulele din client, in ordinea din panouri. Cele cu * din GUI sunt inregistrate dar fara logica inca (Stub). */
public final class Catalog {
    private Catalog() {}

    private static void st(List<Module> m, Category c, String name, String... spec) { m.add(new Stub(name, c, spec)); }

    public static void registerAll(List<Module> m) {
        // ---------------- BASE RAID ----------------
        m.add(SusFinder.betterSusChunk());
        m.add(SusFinder.susChunkFinder());
        st(m, Category.BASE_RAID, "Chunk Finder", "n:Changes To Activate:5:1:20:1", "b:Sky Light:true", "b:Only Below Y 0:true");
        m.add(SusFinder.amethystBypass());
        st(m, Category.BASE_RAID, "Tunnel Base Finder", "b:Follow Sus Chunks:true", "n:Scan Radius:64:16:256:8", "m:Move:Walk:Walk|Fly");
        st(m, Category.BASE_RAID, "Media Base Finder", "n:Min Storage:8:1:64:1", "n:Radius:96:16:256:8");
        st(m, Category.BASE_RAID, "Storage ESP Bypass", "b:Chests:true", "b:Shulkers:true", "b:Barrels:true", "b:Spawners:true");
        st(m, Category.BASE_RAID, "Block Entity Debug", "b:Show Icons:true", "b:Show Count:true");
        st(m, Category.BASE_RAID, "Player Bypass 2", "n:Sensitivity:3:1:5:1", "b:Elytra Detect:true");

        // ---------------- DONUT ----------------
        st(m, Category.DONUT, "Donut Speed Mine", "n:Break At %:80:50:100:5");
        m.add(new AntiVanish());
        m.add(new StaffListHud());
        st(m, Category.DONUT, "Staff Live", "b:Notify:true", "n:Refresh (s):10:1:60:1");
        m.add(new AutoRelog());
        st(m, Category.DONUT, "Spawner Protect", "b:Webhook Alert:true", "b:Mine Spawners:true", "b:Disconnect After:true");
        st(m, Category.DONUT, "AH Sniper", "n:Max Price:0:0:10000000:1000", "n:Max Total Spend:0:0:100000000:10000", "b:Only Buy With Confirm:true", "b:Check Menu Item:true");
        st(m, Category.DONUT, "Panic Pay", "n:Amount %:100:1:100:1", "b:Armor Off First:true");
        st(m, Category.DONUT, "Goliath RTP", "m:Region:EU West:EU West|EU Central|NA East|NA West", "b:Auto Repeat:false");

        // ---------------- COMBAT ----------------
        st(m, Category.COMBAT, "Crystal Aura", "n:Range:5:1:6:0.5", "n:Min Damage:4:0:20:0.5", "n:Max Self Damage:8:0:20:0.5", "b:Silent Rotations:true", "b:Show Damage:true", "b:Run Anchor Shots:true");
        st(m, Category.COMBAT, "Anchor Aura", "n:Range:5:1:6:0.5", "n:Max Self Damage:8:0:20:0.5", "b:Safe Anchor:true", "b:Double Anchor:false");
        st(m, Category.COMBAT, "Crystal Macro", "b:Pause After Kill:true", "n:Place Delay:0:0:10:1", "b:Switch Back:true");
        st(m, Category.COMBAT, "Anchor Macro", "b:Safe Anchor:true", "b:Double Anchor:false", "b:Pause After Kill:true");
        m.add(new CombatMods.CrystalOptimizer());
        m.add(new FriendGuard());
        st(m, Category.COMBAT, "Spear Swap", "b:Dash:true", "b:Switch Back:true");
        st(m, Category.COMBAT, "Auto City", "n:Range:5:1:6:0.5", "b:Turn Off After:true");
        m.add(new CombatMods.AutoTotem());
        st(m, Category.COMBAT, "Auto Trap", "n:Range:4:1:6:0.5", "m:Block:Obsidian:Obsidian|Cobweb");
        st(m, Category.COMBAT, "Jump Reset", "n:Chance %:100:0:100:5");
        st(m, Category.COMBAT, "Key Throw", "m:Item:Ender Pearl:Ender Pearl|Wind Charge|Snowball", "b:Switch Back:true");
        st(m, Category.COMBAT, "Knockback Delay", "n:Delay (ms):100:0:500:10");
        st(m, Category.COMBAT, "Fake Lag", "n:Delay (ms):150:0:1000:10", "b:Only In Combat:true");
        st(m, Category.COMBAT, "Crystal PvP Bot", "n:Reach:4.5:3:6:0.5", "b:Auto Totem:true", "b:Target Friends:false");
        st(m, Category.COMBAT, "Sword PvP Bot", "n:Reach:3:2:6:0.5", "b:Crits:true", "b:Block Hit:false");

        // ---------------- RENDER ----------------
        m.add(new RenderMods.GlowEsp());
        m.add(new RenderMods.Trajectories());
        m.add(new RenderMods.LowHealthWarning());
        m.add(BlockSetEsp.esp());
        m.add(BlockSetEsp.storageEsp());
        st(m, Category.RENDER, "Hole ESP", "n:Range:10:2:32:1", "b:Bedrock:true", "b:Obsidian:true", "b:Ignore Vines And Plants:true");
        m.add(new RenderMods.Nametags());
        m.add(new RenderMods.Tracers());
        m.add(new RenderMods.Waypoints());
        st(m, Category.RENDER, "Region Map", "n:Zoom:1:1:5:1", "b:Show Chunks:true");
        m.add(new Freecam());
        m.add(BlockSetEsp.xray());
        st(m, Category.RENDER, "NoRender", "b:Rain And Snow:true", "b:Lava Overlay:true", "b:Fire On Entities:true");
        m.add(new Zoom());

        // ---------------- MOVEMENT ----------------
        m.add(new NoJumpDelay());
        m.add(new Sprint());
        m.add(new MovementMods.Fly());
        m.add(new MovementMods.Speed());
        m.add(new MovementMods.AutoWalk());
        m.add(new MovementMods.AutoJump());
        m.add(new BoatMods.BoatFly());
        m.add(new BoatMods.BoatNoClip());
        st(m, Category.MOVEMENT, "Elytra Autopilot", "n:Cruise Height:200:60:320:5", "b:Auto Firework:true");
        st(m, Category.MOVEMENT, "Elytra Fly", "n:Speed:1.5:0.5:5:0.1", "b:Auto Swap:true");

        // ---------------- WORLD ----------------
        st(m, Category.WORLD, "Area Miner", "n:Size X:8:1:64:1", "n:Size Z:8:1:64:1", "n:Depth:4:1:64:1");
        st(m, Category.WORLD, "Easy Place", "n:Delay (ticks):0:0:5:1", "b:Only Blocks:true");
        st(m, Category.WORLD, "Schematic Builder", "n:Blocks Per Tick:1:1:4:1", "b:Rotate:true");
        st(m, Category.WORLD, "Anti Ghost Block", "n:Radius:4:1:6:1");

        // ---------------- VISUALS ----------------
        st(m, Category.VISUALS, "3D Character", "m:Character:Sakura:Sakura|Luna|Mika|Aoi|Tung Tung Tung Sahur|Tralalero Tralala|CJ|Neighbor|Miku|Custom", "b:Animate Joints:true");
        st(m, Category.VISUALS, "3D Hand", "b:Follow Hand Pose:true");
        st(m, Category.VISUALS, "Pharaoh Headdress", "b:Glow:false");
        m.add(new VisualMods.FloatingRelics());
        m.add(new VisualMods.TargetEsp());
        st(m, Category.VISUALS, "Totem Spirit", "b:Ghost Effect:true");
        st(m, Category.VISUALS, "Particles", "b:Totem Pops:true", "b:Projectiles:true", "b:Steps:false");
        st(m, Category.VISUALS, "Hand Pose", "n:X:0:-2:2:0.05", "n:Y:0:-2:2:0.05", "n:Z:0:-2:2:0.05", "n:Rotation:0:-180:180:5", "n:Scale:1:0.2:2:0.05");
        st(m, Category.VISUALS, "Swing Animation", "m:Style:Slash:Slash|Chop|Hook|Spin|Double Tap|Push", "n:Speed:1:0.2:3:0.1");
        st(m, Category.VISUALS, "Hand Aura", "b:Glow Outline:true");
        st(m, Category.VISUALS, "Weapon Inspect", "m:Knife Skin:None:None|Butterfly|Karambit|Katana", "m:Inspect:Classic:Classic|Twirl");
        st(m, Category.VISUALS, "Atmosphere", "m:Sky:Space:Space|Northern Lights|Ringed Planet|Black Hole", "b:Keep In Rain:true");

        // ---------------- HUD ----------------
        m.add(new HudMods.TargetHud());
        m.add(new HudMods.ArmorHud());
        st(m, Category.HUD, "Cooldown", "b:Pearl:true", "b:Wind Charge:true");
        m.add(new HudMods.HudLogo());
        m.add(new SpotifyHud());

        // ---------------- MISC ----------------
        st(m, Category.MISC, "Auto Replenish", "m:Mode:Instant:Instant|Legit", "n:Refill Below:16:1:63:1");
        st(m, Category.MISC, "Auto Eat", "n:Hunger Below:14:1:19:1", "b:Prefer Golden Apples:false");
        st(m, Category.MISC, "Auto Reconnect", "n:Delay (s):5:1:60:1", "b:Stop After Ban:true");
        st(m, Category.MISC, "No Inv Leak", "b:Stream Only:true", "b:Borderless Fullscreen:true", "b:Hide Selected Slot:false");
        st(m, Category.MISC, "Fake Stats", "b:Edit Tab List:true");
        st(m, Category.MISC, "Name Protect", "b:Hide Bedrock Names:true");
        st(m, Category.MISC, "Vehicle Log", "b:Log To Chat:true");
        st(m, Category.MISC, "Discord Presence", "b:Show Server:false");
    }
}
