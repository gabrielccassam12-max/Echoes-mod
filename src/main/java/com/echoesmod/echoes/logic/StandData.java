package com.echoesmod.echoes.logic;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Per-player Stand data, stored in the player's persistent NBT. */
public final class StandData {
    private StandData() {}

    private static CompoundTag tag(Player p) {
        CompoundTag root = p.getPersistentData();
        if (!root.contains("echoes", 10)) root.put("echoes", new CompoundTag());
        return root.getCompound("echoes");
    }

    public static boolean isAwakened(Player p) { return tag(p).getBoolean("awakened"); }
    public static void awaken(Player p) {
        CompoundTag t = tag(p);
        t.putBoolean("awakened", true);
        if (t.getInt("act") == 0) t.putInt("act", 1);
    }
    public static int getAct(Player p) { return Math.max(1, tag(p).getInt("act")); }
    public static void setAct(Player p, int a) { tag(p).putInt("act", a); }
    public static int getUnlocked(Player p) { return Math.max(1, tag(p).getInt("unlocked")); }
    public static void setUnlocked(Player p, int a) { tag(p).putInt("unlocked", Math.max(1, Math.min(3, a))); }
    public static int getMastery(Player p, int act) { return tag(p).getInt("mastery" + act); }
    public static int addMastery(Player p, int act, int n) {
        CompoundTag t = tag(p);
        int v = Math.min(100, t.getInt("mastery" + act) + n);
        t.putInt("mastery" + act, v);
        return v;
    }
    public static int getWordMode(Player p) { return tag(p).getInt("wordMode") % 3; }
    public static int cycleWordMode(Player p) {
        int m = (getWordMode(p) + 1) % 3;
        tag(p).putInt("wordMode", m);
        return m;
    }
}
