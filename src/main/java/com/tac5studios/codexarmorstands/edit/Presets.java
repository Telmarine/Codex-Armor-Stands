package com.tac5studios.codexarmorstands.edit;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.tac5studios.codexarmorstands.CodexArmorStands;
import net.minecraft.core.Rotations;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Preset poses: one file per pose in config/codex_armor_stands/presets/<id>.json.
 * <p>
 *  - First start: every built-in preset is copied out of the jar, plus a README.txt for the format.
 *  - Later versions: new built-in presets are copied once. ".installed" remembers what was copied,
 *    so a built-in file the owner deleted stays deleted.
 *  - Anyone can add a pose by dropping a .json file in the folder and running /armorstand reload.
 *  - An old single presets.json (1.0.0 test builds) is split into files once and renamed presets.json.old.
 */
@EventBusSubscriber(modid = CodexArmorStands.MOD_ID, value = Dist.DEDICATED_SERVER)
public final class Presets {

    public record Preset(String id, String name, String group, Item icon, int order, Stands.Pose pose) {}

    private static final String JAR_DIR = "/defaults/presets/";
    private static volatile List<Preset> list = List.of();

    private Presets() {}

    @SubscribeEvent
    @SuppressWarnings("unused") // the event bus needs the parameter
    public static void onAboutToStart(ServerAboutToStartEvent e) {
        load();
    }

    public static List<Preset> all() {
        return list;
    }

    public static Preset get(String id) {
        for (Preset p : list) if (p.id().equals(id)) return p;
        return null;
    }

    public static Path folder() {
        return FMLPaths.CONFIGDIR.get().resolve(CodexArmorStands.MOD_ID).resolve("presets");
    }

    /** Reads every preset file. A broken file is skipped and named in the log; the rest still load. */
    public static void load() {
        Path dir = folder();
        try {
            Files.createDirectories(dir);
            splitOldFile(dir);
            installDefaults(dir);
        } catch (Exception ex) {
            CodexArmorStands.LOGGER.error("[ArmorStands] Could not set up {}: {}", dir, ex.toString());
        }

        List<Preset> out = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : (Iterable<Path>) files.sorted()::iterator) {
                String file = f.getFileName().toString();
                if (!file.endsWith(".json")) continue;
                String id = file.substring(0, file.length() - 5);
                try (Reader r = Files.newBufferedReader(f, StandardCharsets.UTF_8)) {
                    JsonObject o = JsonParser.parseReader(r).getAsJsonObject();
                    if (o.has("enabled") && !o.get("enabled").getAsBoolean()) continue;
                    out.add(parse(id, o));
                } catch (Exception ex) {
                    CodexArmorStands.LOGGER.error("[ArmorStands] Skipped preset {}: {}", file, ex.toString());
                }
            }
        } catch (Exception ex) {
            CodexArmorStands.LOGGER.error("[ArmorStands] Could not read {}: {}", dir, ex.toString());
        }
        out.sort(Comparator.comparingInt(Preset::order).thenComparing(Preset::name, String.CASE_INSENSITIVE_ORDER));
        list = Collections.unmodifiableList(out);
        CodexArmorStands.LOGGER.info("[ArmorStands] {} preset poses loaded.", list.size());
    }

    // ---------- built-in files ----------

    /** Copies built-in presets that were never installed before. Never overwrites a file. */
    private static void installDefaults(Path dir) throws Exception {
        Path marker = dir.resolve(".installed");
        Set<String> installed = new LinkedHashSet<>();
        if (Files.exists(marker)) {
            for (String line : Files.readAllLines(marker, StandardCharsets.UTF_8)) {
                if (!line.isBlank()) installed.add(line.trim());
            }
        }
        boolean changed = false;
        for (String id : jarIndex()) {
            if (installed.contains(id)) continue;
            Path target = dir.resolve(id + ".json");
            if (!Files.exists(target)) copyFromJar(id + ".json", target);
            installed.add(id);
            changed = true;
        }
        Path readme = dir.resolve("README.txt");
        if (!Files.exists(readme)) copyFromJar("README.txt", readme);
        if (changed) Files.write(marker, installed, StandardCharsets.UTF_8);
    }

    private static List<String> jarIndex() throws Exception {
        List<String> ids = new ArrayList<>();
        try (InputStream in = CodexArmorStands.class.getResourceAsStream(JAR_DIR + "_index.txt")) {
            if (in == null) return ids;
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) if (!line.isBlank()) ids.add(line.trim());
        }
        return ids;
    }

    private static void copyFromJar(String name, Path target) throws Exception {
        try (InputStream in = CodexArmorStands.class.getResourceAsStream(JAR_DIR + name)) {
            if (in != null) Files.copy(in, target);
        }
    }

    /** An old single presets.json is split into one file per pose (existing files are left alone). */
    private static void splitOldFile(Path dir) throws Exception {
        Path old = dir.getParent().resolve("presets.json");
        if (!Files.exists(old)) return;
        JsonArray arr;
        try (Reader r = Files.newBufferedReader(old, StandardCharsets.UTF_8)) {
            arr = JsonParser.parseReader(r).getAsJsonObject().getAsJsonArray("presets");
        }
        int order = 0;
        for (JsonElement el : arr) {
            JsonObject o = el.getAsJsonObject();
            order += 10;
            String id = o.get("id").getAsString().replaceAll("[^a-z0-9_\\-]", "_");
            Path target = dir.resolve(id + ".json");
            if (Files.exists(target)) continue;
            o.remove("id");
            if (!o.has("order")) o.addProperty("order", order);
            Files.writeString(target, new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(o), StandardCharsets.UTF_8);
        }
        Files.move(old, old.resolveSibling("presets.json.old"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        CodexArmorStands.LOGGER.info("[ArmorStands] Split presets.json into one file per pose in {}.", dir);
    }

    // ---------- parsing ----------

    private static Preset parse(String id, JsonObject o) {
        String name = o.has("name") ? o.get("name").getAsString() : id;
        String group = o.has("group") ? o.get("group").getAsString() : "";
        int order = o.has("order") ? o.get("order").getAsInt() : 1000;
        Item icon = Items.ARMOR_STAND;
        if (o.has("icon")) {
            ResourceLocation rl = ResourceLocation.tryParse(o.get("icon").getAsString());
            if (rl != null && BuiltInRegistries.ITEM.containsKey(rl)) icon = BuiltInRegistries.ITEM.get(rl);
        }
        // A part that's left out is straight (0, 0, 0).
        Stands.Pose pose = new Stands.Pose(rot(o, "head"), rot(o, "body"), rot(o, "left_arm"), rot(o, "right_arm"),
                rot(o, "left_leg"), rot(o, "right_leg"));
        return new Preset(id, name, group, icon, order, pose);
    }

    private static Rotations rot(JsonObject o, String key) {
        if (!o.has(key)) return new Rotations(0, 0, 0);
        JsonArray a = o.getAsJsonArray(key);
        if (a.size() != 3) throw new IllegalArgumentException(key + " needs 3 numbers");
        return new Rotations(a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat());
    }
}
