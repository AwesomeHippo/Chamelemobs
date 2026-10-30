package com.awesomehippo.chamelemobs.config;

import com.awesomehippo.chamelemobs.client.ShaderPacks;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("chamelemobs.json");
    private static final List<String> DEFAULT_MOBS = List.of("minecraft:zombie", "minecraft:spider", "minecraft:skeleton", "minecraft:creeper");

    public static boolean enabled = true;
    public static float strength = 1.0f;
    public static float brightness = 0.0f;
    public static boolean locked = false;
    private static boolean localEnabled = true;
    private static float localStrength = 1.0f;
    private static float localBrightness = 0.0f;
    private static final List<String> LOCAL_MOBS = new ArrayList<>(DEFAULT_MOBS);

    private static final List<String> MOB_IDS = new ArrayList<>();
    private static final Map<ResourceLocation, Float> MOBS = new HashMap<>();

    public static void load() {
        if (!Files.exists(FILE)) {
            resetLocal();
            writeFile();
            if (!locked) {
                useLocal();
            }
            return;
        }
        try (Reader reader = Files.newBufferedReader(FILE)) {
            FileData data = GSON.fromJson(reader, FileData.class);
            if (data == null) {
                data = new FileData();
            }
            localEnabled = data.enabled != null ? data.enabled : true;
            if (data.strength != null) {
                localStrength = Math.round(Mth.clamp(data.strength, 0.0f, 1.0f) * 100.0f) / 100.0f;
            } else {
                localStrength = 1.0f;
            }
            if (data.brightness != null) {
                localBrightness = Math.round(Mth.clamp(data.brightness, 0.0f, 1.0f) * 100.0f) / 100.0f;
            } else {
                localBrightness = 0.0f;
            }
            LOCAL_MOBS.clear();
            LOCAL_MOBS.addAll(cleanIds(data.mobs));
            if (!locked) {
                useLocal();
            }
        } catch (Exception e) {
            resetLocal();
            if (!locked) {
                useLocal();
            }
        }
    }

    public static void save() {
        if (!locked) {
            localEnabled = enabled;
            localStrength = Math.round(Mth.clamp(strength, 0.0f, 1.0f) * 100.0f) / 100.0f;
            strength = localStrength;
            localBrightness = Math.round(Mth.clamp(brightness, 0.0f, 1.0f) * 100.0f) / 100.0f;
            brightness = localBrightness;
            LOCAL_MOBS.clear();
            LOCAL_MOBS.addAll(MOB_IDS);
        }
        writeFile();
    }

    public static void applyRemote(boolean enabled, float strength, float brightness, List<String> mobs) {
        locked = true;
        Config.enabled = enabled;
        Config.strength = Math.round(Mth.clamp(strength, 0.0f, 1.0f) * 100.0f) / 100.0f;
        Config.brightness = Math.round(Mth.clamp(brightness, 0.0f, 1.0f) * 100.0f) / 100.0f;
        setMobs(mobs);
    }

    public static void clearRemote() {
        if (!locked) {
            return;
        }
        locked = false;
        load();
    }

    private static void resetLocal() {
        localEnabled = true;
        localStrength = 1.0f;
        localBrightness = 0.0f;
        LOCAL_MOBS.clear();
        LOCAL_MOBS.addAll(DEFAULT_MOBS);
    }

    private static void useLocal() {
        enabled = localEnabled;
        strength = localStrength;
        brightness = localBrightness;
        setMobs(LOCAL_MOBS);
    }

    private static void writeFile() {
        FileData data = new FileData();
        data.enabled = localEnabled;
        data.strength = localStrength;
        data.brightness = localBrightness;
        data.mobs = new ArrayList<>(LOCAL_MOBS);
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE)) {
                GSON.toJson(data, writer);
            }
        } catch (IOException ignored) {}
    }

    public static List<String> mobIds() {
        return MOB_IDS;
    }

    public static void setMobs(List<String> ids) {
        MOB_IDS.clear();
        MOBS.clear();
        for (String id : cleanIds(ids)) {
            MobEntry mob = parseMob(id);
            if (mob != null && MOBS.putIfAbsent(mob.key(), mob.chance()) == null) {
                MOB_IDS.add(id);
            }
        }
    }

    private static List<String> cleanIds(List<String> ids) {
        List<String> out = new ArrayList<>();
        Set<ResourceLocation> seen = new HashSet<>();
        if (ids != null) {
            for (String id : ids) {
                if (id == null || id.isBlank()) {
                    continue;
                }
                String trimmed = id.trim();
                MobEntry mob = parseMob(trimmed);
                if (mob != null && seen.add(mob.key())) {
                    out.add(trimmed);
                }
            }
        }
        if (out.isEmpty()) {
            out.addAll(DEFAULT_MOBS);
        }
        return out;
    }

    private static MobEntry parseMob(String id) {
        String name = id;
        float chance = 1.0f;
        int slash = id.lastIndexOf('/');
        if (slash > 0 && id.substring(slash + 1).trim().matches("\\d{1,3}")) {
            name = id.substring(0, slash).trim();
            chance = Math.min(Integer.parseInt(id.substring(slash + 1).trim()), 100) / 100.0f;
        }
        ResourceLocation key = ResourceLocation.tryParse(name);
        return key == null ? null : new MobEntry(key, chance);
    }

    public static boolean shouldCloak(Entity entity) {
        if (!enabled || strength <= 0.0f || entity == null || ShaderPacks.shadowPass()) {
            return false;
        }
        ResourceLocation key = EntityType.getKey(entity.getType());
        Float chance = MOBS.get(key);
        return chance != null && (entity.getUUID().hashCode() & 0xFFFF) / 65536.0f < chance;
    }

    private record MobEntry(ResourceLocation key, float chance) {}

    private static class FileData {
        Boolean enabled = true;
        Float strength = 1.0f;
        Float brightness = 0.0f;
        List<String> mobs = new ArrayList<>(DEFAULT_MOBS);
    }
}
