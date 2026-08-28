package com.awesomehippo.chamelemobs.client;

import com.awesomehippo.chamelemobs.config.Config;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.material.FogType;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

public class SceneCopy {
    public static boolean afterWater;
    private static RenderTarget copy;

    public static boolean cloaksAfterWater(Entity entity) {
        if (!Config.shouldCloak(entity) || CloakRenderType.get() == null || entity.isUnderWater()) {
            return false;
        }
        return Minecraft.getInstance().gameRenderer.getMainCamera().getFluidInCamera() != FogType.WATER;
    }

    public static void capture() {
        if (!Config.enabled || Config.strength <= 0.0f) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        RenderTarget main = mc.getMainRenderTarget();
        if (main.width <= 0 || main.height <= 0) {
            return;
        }
        ensure(main.width, main.height);
        if (copy == null) {
            return;
        }
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, main.frameBufferId);
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, copy.frameBufferId);
        GL30.glBlitFramebuffer(0, 0, main.width, main.height, 0, 0, copy.width, copy.height, GL11.GL_COLOR_BUFFER_BIT, GL11.GL_NEAREST);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, main.frameBufferId);
    }

    public static int getColorTextureId() {
        return copy == null ? 0 : copy.getColorTextureId();
    }

    public static boolean ready() {
        return copy != null;
    }

    private static void ensure(int w, int h) {
        if (copy == null) {
            copy = new TextureTarget(w, h, false, Minecraft.ON_OSX);
            copy.setClearColor(0, 0, 0, 0);
            copy.clear(Minecraft.ON_OSX);
        } else if (copy.width != w || copy.height != h) {
            copy.resize(w, h, Minecraft.ON_OSX);
        }
    }
}
