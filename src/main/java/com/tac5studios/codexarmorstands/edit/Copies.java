package com.tac5studios.codexarmorstands.edit;

import com.tac5studios.codexarmorstands.CodexArmorStands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** A player's saved copies, kept in their player data so they survive restarts. */
public final class Copies {

    private static final String ROOT = CodexArmorStands.MOD_ID;

    private Copies() {}

    private static CompoundTag root(ServerPlayer p) {
        CompoundTag data = p.getPersistentData();
        if (!data.contains(ROOT)) data.put(ROOT, new CompoundTag());
        return data.getCompound(ROOT);
    }

    public static CompoundTag get(ServerPlayer p, int slot) {
        CompoundTag r = root(p);
        String k = "copy" + slot;
        return r.contains(k) ? r.getCompound(k) : null;
    }

    public static void set(ServerPlayer p, int slot, CompoundTag t) {
        root(p).put("copy" + slot, t);
    }

    public static void clear(ServerPlayer p, int slot) {
        root(p).remove("copy" + slot);
    }
}
