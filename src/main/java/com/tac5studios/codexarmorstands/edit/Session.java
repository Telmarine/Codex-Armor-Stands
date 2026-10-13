package com.tac5studios.codexarmorstands.edit;

import net.minecraft.server.level.ServerPlayer;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** What a player has picked in the tool menu. Kept until they log out. */
public final class Session {

    private static final Map<UUID, Session> ALL = new ConcurrentHashMap<>();

    public Mode mode = Mode.HEAD;
    public Axis axis = Axis.X;
    public boolean fine;
    public String preset = "";
    public int copySlot;
    /** Hotbar slot last tick, for the sneak + scroll axis switch. */
    public int lastSlot = -1;
    public boolean lastHeldTool;

    public static Session of(ServerPlayer p) {
        return ALL.computeIfAbsent(p.getUUID(), u -> new Session());
    }

    public static void forget(UUID player) {
        ALL.remove(player);
    }
}
