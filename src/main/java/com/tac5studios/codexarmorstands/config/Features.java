package com.tac5studios.codexarmorstands.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.ModConfigSpec.BooleanValue;
import net.neoforged.neoforge.common.ModConfigSpec.ConfigValue;

import java.util.List;

/**
 * Master control file: config/codex_armor_stands/features.toml
 * "enabled" off turns the whole editor off. Each mode has its own switch.
 * Switches marked (restart) need a server restart.
 */
public final class Features {

    public static final ModConfigSpec SPEC;

    public static final BooleanValue ENABLED;

    // Modes
    public static final BooleanValue POSE, ROTATE, MOVE, SHOW_ARMS, SMALL, BASE_PLATE, INVISIBLE, GRAVITY, GLOWING,
            INVULNERABLE, NAME_VISIBLE, SCALE, LOCK, EQUIPMENT, PRESETS, COPY_PASTE, MIRROR, RESET, ITEM_FRAMES;

    // Tool controls
    public static final BooleanValue SCROLL_AXIS, SWAP_HANDS_NEAREST, ACTION_BAR, UI_SOUNDS;

    // Extras
    public static final BooleanValue RENAME_COLORS, PLAYER_HEADS, CHUNK_LIMIT, GIVE_TOOL;

    // Protection
    public static final BooleanValue OWNERSHIP, LOCK_PROTECTS, CLAIMS, CLAIMS_OPAC, CLAIMS_FTB_CHUNKS;

    // Overlap guard
    public static final BooleanValue OVERLAP_GUARD;
    public static final ConfigValue<List<? extends String>> OVERLAP_FORCE_ON;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment(
                "Codex: Armor Stands master switches.",
                "\"enabled\" off turns the whole editor off.",
                "Switches marked (restart) need a server restart."
        );

        b.comment("The editor.").push("editor");
        ENABLED = sw(b, "enabled", true, "Turn the armor stand editor on or off. (restart)");
        b.pop();

        b.comment("Edit modes. A mode that's off is hidden from the menu.").push("modes");
        POSE = sw(b, "pose", true, "Pose the head, body, arms and legs.");
        ROTATE = sw(b, "rotate", true, "Turn the whole stand.");
        MOVE = sw(b, "move", true, "Nudge the stand along an axis.");
        SHOW_ARMS = sw(b, "show_arms", true, "Show or hide the arms.");
        SMALL = sw(b, "small", true, "Switch between normal and small size.");
        BASE_PLATE = sw(b, "base_plate", true, "Show or hide the base plate.");
        INVISIBLE = sw(b, "invisible", true, "Make the stand invisible (worn items still show).");
        GRAVITY = sw(b, "gravity", true, "Let the stand float in place.");
        GLOWING = sw(b, "glowing", true, "Outline the stand so it shows through walls.");
        INVULNERABLE = sw(b, "invulnerable", true, "Stop the stand from being broken.");
        NAME_VISIBLE = sw(b, "name_visible", true, "Show the stand's name above it (name it with a name tag).");
        SCALE = sw(b, "scale", true, "Change the stand's size (limits in steps.toml).");
        LOCK = sw(b, "lock", true, "Lock every slot so nothing can be taken or added.");
        EQUIPMENT = sw(b, "equipment", true, "Open the stand's armor and hand slots in a chest menu.");
        PRESETS = sw(b, "presets", true, "Apply a preset pose (one file per pose in presets/).");
        COPY_PASTE = sw(b, "copy_paste", true, "Copy a stand's pose and style and paste it onto another.");
        MIRROR = sw(b, "mirror", true, "Swap the left and right side of the pose.");
        RESET = sw(b, "reset", true, "Put the pose back to default.");
        ITEM_FRAMES = sw(b, "item_frames", true, "Make item frames invisible or glowing with the tool.");
        b.pop();

        b.comment("How the tool is used.").push("controls");
        SCROLL_AXIS = sw(b, "scroll_axis", true, "Sneak + scroll the hotbar while holding the tool to switch axis.");
        SWAP_HANDS_NEAREST = sw(b, "swap_hands_nearest", true, "Press the swap-hands key (F) with the tool to edit the nearest stand. Sneak + F steps back.");
        ACTION_BAR = sw(b, "action_bar", true, "Show the mode, axis and value above the hotbar after each edit.");
        UI_SOUNDS = sw(b, "ui_sounds", true, "Click sounds in menus.");
        b.pop();

        b.comment("Extras.").push("extras");
        RENAME_COLORS = sw(b, "rename_colors", true, "Name tags with & color codes give armor stands a colored name.");
        PLAYER_HEADS = sw(b, "player_heads", true, "/armorstand head <name> gives a player head. (restart)");
        CHUNK_LIMIT = sw(b, "chunk_limit", false, "Limit how many armor stands players can place in one chunk (limit in steps.toml).");
        GIVE_TOOL = sw(b, "give_tool", true, "/armorstand tool gives the edit tool. (restart)");
        b.pop();

        b.comment("Who can edit which stands.").push("protection");
        OWNERSHIP = sw(b, "ownership", true, "Stands remember who placed them. Only the owner, the land owner and staff can edit them.");
        LOCK_PROTECTS = sw(b, "lock_protects", true, "Locked stands can't be broken by anyone except the owner and staff.");
        CLAIMS = sw(b, "claims", true, "Check claim mods before every edit.");
        CLAIMS_OPAC = sw(b, "opac", true, "Use Open Parties and Claims for claim checks.");
        CLAIMS_FTB_CHUNKS = sw(b, "ftb_chunks", true, "Use FTB Chunks for claim checks.");
        b.pop();

        b.comment("Turns the editor off when another armor stand editor mod is installed.").push("overlap_guard");
        OVERLAP_GUARD = sw(b, "enabled", true, "Turn the overlap guard on or off. (restart)");
        OVERLAP_FORCE_ON = b.comment("Keep the editor on even when Armor Statues or Armor Poser is installed: add \"editor\".")
                .defineListAllowEmpty("force_on", List.of(), () -> "", o -> o instanceof String);
        b.pop();

        SPEC = b.build();
    }

    private Features() {}

    private static BooleanValue sw(ModConfigSpec.Builder b, String key, boolean def, String comment) {
        return b.comment(comment).define(key, def);
    }

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC, "codex_armor_stands/features.toml");
    }

    /** True when the editor is on (and not turned off by the overlap guard). */
    public static boolean editor() {
        return ENABLED.get() && !com.tac5studios.codexarmorstands.overlap.OverlapGuard.editorBlocked();
    }

    /** True when the editor and this switch are both on. */
    public static boolean on(BooleanValue mode) {
        return editor() && mode.get();
    }

    public static boolean forcedOn(String name) {
        return OVERLAP_FORCE_ON.get().contains(name);
    }
}
