package com.tac5studios.codexarmorstands.config;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

/** config/codex_armor_stands/tool.toml — which item is the edit tool. */
public final class ToolConfig {

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> ITEM;
    public static final ModConfigSpec.ConfigValue<String> REQUIRED_NAME;
    public static final ModConfigSpec.DoubleValue NEAREST_RANGE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.comment("The armor stand edit tool.").push("tool");
        ITEM = b.comment("Item id of the tool.").define("item", "minecraft:flint");
        REQUIRED_NAME = b.comment("The tool must have this exact name (rename it in an anvil). Empty = any item of that kind works.",
                "Color codes are ignored when comparing.").define("required_name", "");
        NEAREST_RANGE = b.comment("How far the swap-hands key (F) looks for the nearest stand, in blocks.")
                .defineInRange("nearest_range", 5.0, 1.0, 16.0);
        b.pop();
        SPEC = b.build();
    }

    private ToolConfig() {}

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC, "codex_armor_stands/tool.toml");
    }

    public static Item item() {
        ResourceLocation id = ResourceLocation.tryParse(ITEM.get());
        if (id == null) return Items.FLINT;
        Item i = BuiltInRegistries.ITEM.get(id);
        return i == Items.AIR ? Items.FLINT : i;
    }

    /** True when this stack counts as the edit tool. */
    public static boolean isTool(ItemStack stack) {
        if (stack.isEmpty() || !stack.is(item())) return false;
        String need = REQUIRED_NAME.get();
        if (need.isEmpty()) return true;
        Component name = stack.get(DataComponents.CUSTOM_NAME);
        return name != null && strip(name.getString()).equals(strip(need));
    }

    /** A fresh tool, named when a name is required. */
    public static ItemStack create() {
        ItemStack s = new ItemStack(item());
        String need = REQUIRED_NAME.get();
        if (!need.isEmpty()) {
            s.set(DataComponents.CUSTOM_NAME, com.tac5studios.codexarmorstands.ui.Icons.plain(
                    com.tac5studios.codexarmorstands.util.Text.color(need)));
        }
        return s;
    }

    private static String strip(String s) {
        return s.replaceAll("(?i)&(#[0-9a-f]{6}|[0-9a-fk-or])", "").replaceAll("(?i)§[0-9a-fk-or]", "").trim();
    }
}
