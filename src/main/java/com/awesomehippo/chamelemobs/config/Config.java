package com.awesomehippo.chamelemobs.config;

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Config {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FMLPaths.CONFIGDIR.get().resolve("chamelemobs.json");
    private static final List<String> DEFAULT_MOBS = List.of("minecraft:zombie", "minecraft:spider", "minecraft:skeleton", "minecraft:creeper");

    public static boolean enabled = true;
    public static float strength = 1.0f;

    private static final List<String> MOB_IDS = new ArrayList<>();
    private static final Set<ResourceLocation> MOBS = new HashSet<>();

    public static void load() {
        if (!Files.exists(FILE)) {
            enabled = true;
            strength = 1.0f;
            setMobs(DEFAULT_MOBS);
            save();
            return;
        }
        try (Reader reader = Files.newBufferedReader(FILE)) {
            FileData data = GSON.fromJson(reader, FileData.class);
            if (data == null) {
                data = new FileData();
            }
            if (data.enabled != null) {
                enabled = data.enabled;
            }
            if (data.strength != null) {
                strength = Math.round(Mth.clamp(data.strength, 0.0f, 1.0f) * 100.0f) / 100.0f;
            }
            setMobs(data.mobs);
        } catch (Exception e) {
            enabled = true;
            strength = 1.0f;
            setMobs(DEFAULT_MOBS);
        }
    }

    public static void save() {
        strength = Math.round(Mth.clamp(strength, 0.0f, 1.0f) * 100.0f) / 100.0f;
        FileData data = new FileData();
        data.enabled = enabled;
        data.strength = strength;
        data.mobs = new ArrayList<>(MOB_IDS);
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
        if (ids != null) {
            for (String id : ids) {
                if (id == null || id.isBlank()) {
                    continue;
                }
                String trimmed = id.trim();
                ResourceLocation loc = ResourceLocation.tryParse(trimmed);
                if (loc == null) {
                    continue;
                }
                if (MOBS.add(loc)) {
                    MOB_IDS.add(trimmed);
                }
            }
        }
        if (MOBS.isEmpty()) {
            for (String id : DEFAULT_MOBS) {
                ResourceLocation loc = ResourceLocation.tryParse(id);
                if (loc != null && MOBS.add(loc)) {
                    MOB_IDS.add(id);
                }
            }
        }
    }

    public static boolean shouldCloak(Entity entity) {
        if (!enabled || strength <= 0.0f || entity == null) {
            return false;
        }
        ResourceLocation key = EntityType.getKey(entity.getType());
        return MOBS.contains(key);
    }

    private static class FileData {
        Boolean enabled = true;
        Float strength = 1.0f;
        List<String> mobs = new ArrayList<>(DEFAULT_MOBS);
    }
}
