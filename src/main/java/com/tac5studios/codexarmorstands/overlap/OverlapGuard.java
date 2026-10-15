package com.tac5studios.codexarmorstands.overlap;

import com.tac5studios.codexarmorstands.CodexArmorStands;
import com.tac5studios.codexarmorstands.config.Features;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the editor off when another armor stand editor is installed, so players never get two editors
 * fighting over the same click. features.toml is not changed. overlap_guard.force_on = ["editor"] keeps it on.
 *
 * <p>Also hands the chunk limit over to Elements: Claims or Elements: Nexus (1.0.2 and up), which have
 * their own entity limits. Their settings are used, even when their limits are switched off.
 * overlap_guard.force_on = ["chunk_limit"] keeps this mod's limit on.
 */
@EventBusSubscriber(modid = CodexArmorStands.MOD_ID, value = Dist.DEDICATED_SERVER)
public final class OverlapGuard {

    private static final Map<String, String> MODS = new LinkedHashMap<>();

    static {
        MODS.put("armorstatues", "Armor Statues");
        MODS.put("armorposer", "Armor Poser");
    }

    /** First Nexus version with entity limits. */
    private static final DefaultArtifactVersion NEXUS_LIMITS = new DefaultArtifactVersion("1.0.2");

    private static volatile Boolean blocked;
    private static volatile Boolean limitDeferred;
    private static final List<String> FOUND = new ArrayList<>();
    private static String limitOwner;

    private OverlapGuard() {}

    public static boolean editorBlocked() {
        Boolean b = blocked;
        return b != null ? b : decide();
    }

    /** True when an Elements mod handles armor stand limits instead of this mod. */
    public static boolean chunkLimitDeferred() {
        Boolean b = limitDeferred;
        return b != null ? b : decideLimit();
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

    private static synchronized boolean decideLimit() {
        if (limitDeferred != null) return limitDeferred;
        limitOwner = null;
        if (ModList.get().isLoaded("elements_claims")) {
            limitOwner = "Elements: Claims";
        } else if (nexusHasLimits()) {
            limitOwner = "Elements: Nexus";
        }
        boolean out = limitOwner != null && Features.OVERLAP_GUARD.get() && !Features.forcedOn("chunk_limit");
        limitDeferred = out;
        return out;
    }

    private static boolean nexusHasLimits() {
        return ModList.get().getModContainerById("elements_nexus")
                .map(c -> c.getModInfo().getVersion().compareTo(NEXUS_LIMITS) >= 0)
                .orElse(false);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onAboutToStart(ServerAboutToStartEvent e) {
        decide();
        decideLimit();
        if (!FOUND.isEmpty()) {
            if (blocked) {
                CodexArmorStands.LOGGER.info("[ArmorStands] {} found: the editor is turned off. Add \"editor\" to overlap_guard.force_on to keep it.",
                        String.join(", ", FOUND));
            } else {
                CodexArmorStands.LOGGER.info("[ArmorStands] {} found: the editor is kept on (overlap_guard.force_on).",
                        String.join(", ", FOUND));
            }
        }
        if (limitOwner != null && Features.CHUNK_LIMIT.get()) {
            if (limitDeferred) {
                CodexArmorStands.LOGGER.info("[ArmorStands] {} found: its entity limits are used, chunk_limit is turned off. Add \"chunk_limit\" to overlap_guard.force_on to keep it.",
                        limitOwner);
            } else {
                CodexArmorStands.LOGGER.info("[ArmorStands] {} found: chunk_limit is kept on (overlap_guard.force_on).", limitOwner);
            }
        }
    }
}
