package com.tac5studios.codexarmorstands.perms;

import com.tac5studios.codexarmorstands.CodexArmorStands;
import com.tac5studios.codexarmorstands.config.Features;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.server.permission.PermissionAPI;
import net.neoforged.neoforge.server.permission.events.PermissionGatherEvent;
import net.neoforged.neoforge.server.permission.nodes.PermissionNode;
import net.neoforged.neoforge.server.permission.nodes.PermissionTypes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BooleanSupplier;

/**
 * Every permission node, named "armorstands.<path>". They go through the NeoForge PermissionAPI, so they work
 * with Elements: Nexus ranks, any other permission mod, or plain OP levels. A node whose switch is off isn't registered.
 */
@EventBusSubscriber(modid = CodexArmorStands.MOD_ID, value = Dist.DEDICATED_SERVER)
public final class Perm {

    public enum Who {
        PLAYER(0), STAFF(2), ADMIN(3);

        final int opLevel;

        Who(int opLevel) {
            this.opLevel = opLevel;
        }
    }

    private record Entry(PermissionNode<Boolean> node, Who who, BooleanSupplier gate) {}

    private static final Map<String, Entry> NODES = new LinkedHashMap<>();

    public static final String USE = node("use", Who.PLAYER, Features::editor);
    public static final String POSE = node("edit.pose", Who.PLAYER, Features.POSE);
    public static final String ROTATE = node("edit.rotate", Who.PLAYER, Features.ROTATE);
    public static final String MOVE = node("edit.move", Who.PLAYER, Features.MOVE);
    /** Show arms, small, base plate, invisible, gravity, name visible. */
    public static final String TOGGLES = node("edit.toggles", Who.PLAYER, Features::editor);
    public static final String GLOWING = node("edit.glowing", Who.STAFF, Features.GLOWING);
    public static final String INVULNERABLE = node("edit.invulnerable", Who.STAFF, Features.INVULNERABLE);
    public static final String SCALE = node("edit.scale", Who.PLAYER, Features.SCALE);
    public static final String LOCK = node("edit.lock", Who.PLAYER, Features.LOCK);
    public static final String EQUIPMENT = node("edit.equipment", Who.PLAYER, Features.EQUIPMENT);
    public static final String PRESETS = node("edit.presets", Who.PLAYER, Features.PRESETS);
    public static final String COPY = node("edit.copy", Who.PLAYER, Features.COPY_PASTE);
    public static final String MIRROR = node("edit.mirror", Who.PLAYER, Features.MIRROR);
    public static final String RESET = node("edit.reset", Who.PLAYER, Features.RESET);
    public static final String ITEM_FRAME = node("edit.itemframe", Who.PLAYER, Features.ITEM_FRAMES);
    public static final String RENAME_COLOR = node("rename.color", Who.PLAYER, Features.RENAME_COLORS);
    public static final String HEAD = node("head", Who.STAFF, Features.PLAYER_HEADS);
    public static final String TOOL = node("tool", Who.STAFF, Features.GIVE_TOOL);
    /** Edit any stand: ignores owners, claims and locks. */
    public static final String BYPASS = node("bypass", Who.STAFF, Features::editor);
    /** Place more stands than the chunk limit. */
    public static final String LIMIT_BYPASS = node("limit.bypass", Who.STAFF, Features::chunkLimit);
    /** /armorstand reload, /armorstand tool <player>. */
    public static final String ADMIN = node("admin", Who.ADMIN, () -> true);

    private Perm() {}

    private static String node(String path, Who who, BooleanValue mode) {
        return node(path, who, () -> Features.on(mode));
    }

    private static String node(String path, Who who, BooleanSupplier gate) {
        PermissionNode<Boolean> n = new PermissionNode<>("armorstands", path, PermissionTypes.BOOLEAN,
                (player, uuid, ctx) -> player != null && player.hasPermissions(who.opLevel));
        n.setInformation(Component.literal("armorstands." + path), Component.literal("Default: " + switch (who) {
            case PLAYER -> "everyone";
            case STAFF -> "OP level 2+";
            case ADMIN -> "OP level 3+";
        }));
        NODES.put(n.getNodeName(), new Entry(n, who, gate));
        return n.getNodeName();
    }

    @SubscribeEvent
    public static void onGatherNodes(PermissionGatherEvent.Nodes event) {
        List<PermissionNode<?>> on = new ArrayList<>();
        for (Entry e : NODES.values()) if (e.gate().getAsBoolean()) on.add(e.node());
        event.addNodes(on.toArray(new PermissionNode<?>[0]));
        CodexArmorStands.LOGGER.info("[ArmorStands] {} permission nodes registered.", on.size());
    }

    public static boolean enabled(String node) {
        Entry e = NODES.get(node);
        return e != null && e.gate().getAsBoolean();
    }

    public static boolean has(CommandSourceStack source, String node) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            Entry e = NODES.get(node);
            Who who = e == null ? Who.ADMIN : e.who();
            return source.hasPermission(Math.max(who.opLevel, 2));
        }
        return has(player, node);
    }

    public static boolean has(ServerPlayer player, String node) {
        Entry e = NODES.get(node);
        if (e == null || !e.gate().getAsBoolean()) return false;
        try {
            return PermissionAPI.getPermission(player, e.node());
        } catch (RuntimeException ex) {
            CodexArmorStands.LOGGER.warn("[ArmorStands] Permission check failed for {}: {}", node, ex.toString());
            return player.hasPermissions(e.who().opLevel);
        }
    }
}
