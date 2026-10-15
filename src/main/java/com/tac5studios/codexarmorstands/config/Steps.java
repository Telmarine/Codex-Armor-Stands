package com.tac5studios.codexarmorstands.config;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** config/codex_armor_stands/steps.toml — step sizes and limits. */
public final class Steps {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue POSE_COARSE, POSE_FINE, ROTATE_COARSE, ROTATE_FINE,
            MOVE_COARSE, MOVE_FINE, SCALE_COARSE, SCALE_FINE, SCALE_MIN, SCALE_MAX;
    public static final ModConfigSpec.IntValue CHUNK_LIMIT, COPY_SLOTS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("Step sizes for one click. Coarse/fine is switched in the tool menu.").push("steps");
        POSE_COARSE = b.comment("Pose, degrees per click (coarse).").defineInRange("pose_coarse", 30.0, 0.1, 180.0);
        POSE_FINE = b.comment("Pose, degrees per click (fine).").defineInRange("pose_fine", 3.0, 0.1, 180.0);
        ROTATE_COARSE = b.comment("Rotate, degrees per click (coarse).").defineInRange("rotate_coarse", 45.0, 0.1, 180.0);
        ROTATE_FINE = b.comment("Rotate, degrees per click (fine).").defineInRange("rotate_fine", 3.0, 0.1, 180.0);
        MOVE_COARSE = b.comment("Move, blocks per click (coarse).").defineInRange("move_coarse", 1.0, 0.01, 4.0);
        MOVE_FINE = b.comment("Move, blocks per click (fine). 0.03125 = 1/32 block.").defineInRange("move_fine", 0.03125, 0.001, 4.0);
        SCALE_COARSE = b.comment("Scale per click (coarse).").defineInRange("scale_coarse", 0.1, 0.001, 1.0);
        SCALE_FINE = b.comment("Scale per click (fine).").defineInRange("scale_fine", 0.01, 0.001, 1.0);
        b.pop();

        b.comment("Limits.").push("limits");
        SCALE_MIN = b.comment("Smallest scale players can set.").defineInRange("scale_min", 0.5, 0.0625, 16.0);
        SCALE_MAX = b.comment("Largest scale players can set.").defineInRange("scale_max", 2.0, 0.0625, 16.0);
        CHUNK_LIMIT = b.comment("Most armor stands players can place in one chunk (when chunk_limit is on in features.toml).",
                        "Not used when Elements: Claims or Elements: Nexus 1.0.2+ is installed.")
                .defineInRange("per_chunk", 32, 1, 10000);
        COPY_SLOTS = b.comment("How many copies each player can keep (1 to 9).").defineInRange("copy_slots", 9, 1, 9);
        b.pop();
        SPEC = b.build();
    }

    private Steps() {}

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC, "codex_armor_stands/steps.toml");
    }
}
