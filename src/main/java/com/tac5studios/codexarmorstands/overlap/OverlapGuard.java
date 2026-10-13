package com.tac5studios.codexarmorstands.overlap;

import com.tac5studios.codexarmorstands.CodexArmorStands;
import com.tac5studios.codexarmorstands.config.Features;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the editor off when another armor stand editor is installed, so players never get two editors
 * fighting over the same click. features.toml is not changed. overlap_guard.force_on = ["editor"] keeps it on.
 */
@EventBusSubscriber(modid = CodexArmorStands.MOD_ID, value = Dist.DEDICATED_SERVER)
public final class OverlapGuard {

    private static final Map<String, String> MODS = new LinkedHashMap<>();

    static {
        MODS.put("armorstatues", "Armor Statues");
        MODS.put("armorposer", "Armor Poser");
    }

    private static volatile Boolean blocked;
    private static final List<String> FOUND = new ArrayList<>();

    private OverlapGuard() {}

    public static boolean editorBlocked() {
        Boolean b = blocked;
        return b != null ? b : decide();
    }

    private static synchronized boolean decide() {
        if (blocked != null) return blocked;
        FOUND.clear();
        boolean out = false;
        if (Features.OVERLAP_GUARD.get()) {
            MODS.forEach((id, name) -> {
                if (ModList.get().isLoaded(id)) FOUND.add(name);
            });
            out = !FOUND.isEmpty() && !Features.forcedOn("editor");
        }
        blocked = out;
        return out;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAboutToStart(ServerAboutToStartEvent e) {
        decide();
        if (FOUND.isEmpty()) return;
        if (blocked) {
            CodexArmorStands.LOGGER.info("[ArmorStands] {} found: the editor is turned off. Add \"editor\" to overlap_guard.force_on to keep it.",
                    String.join(", ", FOUND));
        } else {
            CodexArmorStands.LOGGER.info("[ArmorStands] {} found: the editor is kept on (overlap_guard.force_on).",
                    String.join(", ", FOUND));
        }
    }
}
