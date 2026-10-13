package com.tac5studios.codexarmorstands.edit;

import com.tac5studios.codexarmorstands.config.Features;
import com.tac5studios.codexarmorstands.perms.Perm;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;

/** Every edit mode: what it does on a click, its menu icon, switch and permission. */
public enum Mode {
    HEAD(Kind.STEP, Items.PLAYER_HEAD, Features.POSE, Perm.POSE),
    BODY(Kind.STEP, Items.LEATHER_CHESTPLATE, Features.POSE, Perm.POSE),
    LEFT_ARM(Kind.STEP, Items.STICK, Features.POSE, Perm.POSE),
    RIGHT_ARM(Kind.STEP, Items.STICK, Features.POSE, Perm.POSE),
    LEFT_LEG(Kind.STEP, Items.BONE, Features.POSE, Perm.POSE),
    RIGHT_LEG(Kind.STEP, Items.BONE, Features.POSE, Perm.POSE),
    ROTATE(Kind.STEP, Items.COMPASS, Features.ROTATE, Perm.ROTATE),
    MOVE(Kind.STEP, Items.PISTON, Features.MOVE, Perm.MOVE),
    SCALE(Kind.STEP, Items.SLIME_BALL, Features.SCALE, Perm.SCALE),

    SHOW_ARMS(Kind.TOGGLE, Items.BLAZE_ROD, Features.SHOW_ARMS, Perm.TOGGLES),
    SMALL(Kind.TOGGLE, Items.BROWN_MUSHROOM, Features.SMALL, Perm.TOGGLES),
    BASE_PLATE(Kind.TOGGLE, Items.SMOOTH_STONE_SLAB, Features.BASE_PLATE, Perm.TOGGLES),
    INVISIBLE(Kind.TOGGLE, Items.GLASS, Features.INVISIBLE, Perm.TOGGLES),
    GRAVITY(Kind.TOGGLE, Items.FEATHER, Features.GRAVITY, Perm.TOGGLES),
    GLOWING(Kind.TOGGLE, Items.GLOWSTONE_DUST, Features.GLOWING, Perm.GLOWING),
    INVULNERABLE(Kind.TOGGLE, Items.OBSIDIAN, Features.INVULNERABLE, Perm.INVULNERABLE),
    NAME_VISIBLE(Kind.TOGGLE, Items.NAME_TAG, Features.NAME_VISIBLE, Perm.TOGGLES),
    LOCK(Kind.TOGGLE, Items.IRON_BARS, Features.LOCK, Perm.LOCK),

    EQUIPMENT(Kind.ACTION, Items.CHEST, Features.EQUIPMENT, Perm.EQUIPMENT),
    PRESET(Kind.ACTION, Items.BOOK, Features.PRESETS, Perm.PRESETS),
    COPY_PASTE(Kind.ACTION, Items.WRITABLE_BOOK, Features.COPY_PASTE, Perm.COPY),
    MIRROR(Kind.ACTION, Items.TINTED_GLASS, Features.MIRROR, Perm.MIRROR),
    RESET(Kind.ACTION, Items.BARRIER, Features.RESET, Perm.RESET),
    ITEM_FRAME(Kind.ACTION, Items.ITEM_FRAME, Features.ITEM_FRAMES, Perm.ITEM_FRAME);

    public enum Kind { STEP, TOGGLE, ACTION }

    public final Kind kind;
    public final Item icon;
    private final BooleanValue feature;
    private final String perm;

    Mode(Kind kind, Item icon, BooleanValue feature, String perm) {
        this.kind = kind;
        this.icon = icon;
        this.feature = feature;
        this.perm = perm;
    }

    /** Message key part, e.g. "left_arm". */
    public String key() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public boolean isPose() {
        return ordinal() <= RIGHT_LEG.ordinal();
    }

    /** The mode's switch is on and the player may use it. */
    public boolean usable(ServerPlayer p) {
        return Features.on(feature) && Perm.has(p, perm);
    }
}
