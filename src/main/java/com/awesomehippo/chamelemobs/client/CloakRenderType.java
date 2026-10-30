package com.awesomehippo.chamelemobs.client;

import com.awesomehippo.chamelemobs.Chamelemobs;
import com.awesomehippo.chamelemobs.config.Config;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterShadersEvent;

import java.io.IOException;
import java.util.function.Function;

@EventBusSubscriber(modid = Chamelemobs.MOD_ID, value = Dist.CLIENT)
public class CloakRenderType extends RenderType {
    private static ShaderInstance instance;

    private static final ShaderStateShard CLOAK_SHADER = new ShaderStateShard(CloakRenderType::get);

    private static final TexturingStateShard SCENE_BIND = new TexturingStateShard("chamelemobs_scene", () -> {
        RenderSystem.setShaderTexture(3, SceneCopy.getColorTextureId());
        ShaderInstance shader = get();
        if (shader == null) {
            return;
        }
        Uniform strength = shader.getUniform("Strength");
        if (strength != null) {
            strength.set(Config.strength);
        }
        Uniform brightness = shader.getUniform("Brightness");
        if (brightness != null) {
            brightness.set(Config.brightness);
        }
    }, () -> {
        RenderSystem.setShaderTexture(3, 0);
    });

    private static final Function<ResourceLocation, RenderType> CLOAK = Util.memoize(texture ->
            create("chamelemobs_cloak", DefaultVertexFormat.NEW_ENTITY, VertexFormat.Mode.QUADS, 256, true, false,
                    CompositeState.builder()
                            .setShaderState(CLOAK_SHADER)
                            .setTextureState(new TextureStateShard(texture, false, false))
                            .setTexturingState(SCENE_BIND)
                            .setTransparencyState(NO_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .createCompositeState(true)));

    private CloakRenderType(String name, VertexFormat format, VertexFormat.Mode mode, int bufferSize, boolean affectsCrumbling, boolean sortOnUpload, Runnable setup, Runnable clear) {
        super(name, format, mode, bufferSize, affectsCrumbling, sortOnUpload, setup, clear);
    }

    @SubscribeEvent
    public static void register(RegisterShadersEvent event) throws IOException {
        event.registerShader(new ShaderInstance(event.getResourceProvider(), ResourceLocation.fromNamespaceAndPath(Chamelemobs.MOD_ID, "rendertype_cloak"), DefaultVertexFormat.NEW_ENTITY), shader -> instance = shader);
    }

    public static ShaderInstance get() {
        return instance;
    }

    public static RenderType cloak(ResourceLocation texture) {
        return CLOAK.apply(texture);
    }
}
