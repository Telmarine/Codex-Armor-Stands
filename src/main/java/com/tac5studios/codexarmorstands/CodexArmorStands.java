package com.tac5studios.codexarmorstands;

import com.tac5studios.codexarmorstands.config.Features;
import com.tac5studios.codexarmorstands.config.Steps;
import com.tac5studios.codexarmorstands.config.ToolConfig;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Codex: Armor Stands — a server-side armor stand editor.
 * Dedicated servers only. Players use a tool and vanilla chest menus, so they install nothing.
 *
 * <p>Everything else starts from server events (each class subscribes itself):
 *   messages.Msg          read messages.toml
 *   edit.Presets          read presets/ (one file per pose)
 *   perms.Perm            register permission nodes
 *   overlap.OverlapGuard  turn the editor off when Armor Statues or Armor Poser is installed
 *   edit.ToolEvents       clicks, hotbar scroll, swap-hands key, placement, chunk limit
 *   commands.Commands     /armorstand
 */
@Mod(value = CodexArmorStands.MOD_ID, dist = Dist.DEDICATED_SERVER)
public class CodexArmorStands {

    public static final String MOD_ID = "codex_armor_stands";
    public static final Logger LOGGER = LoggerFactory.getLogger("CodexArmorStands");

    public CodexArmorStands(ModContainer container) {
        // config/codex_armor_stands/features.toml, tool.toml, steps.toml
        Features.register(container);
        ToolConfig.register(container);
        Steps.register(container);
        LOGGER.info("[ArmorStands] Codex: Armor Stands loading.");
    }
}
