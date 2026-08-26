package com.awesomehippo.chamelemobs.client;

import com.awesomehippo.chamelemobs.Chamelemobs;
import com.awesomehippo.chamelemobs.config.Config;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = Chamelemobs.MOD_ID, value = Dist.CLIENT)
public class ClientEvents {
    @SubscribeEvent
    public static void onStage(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS) {
            SceneCopy.afterWater = false;
            SceneCopy.capture();
            return;
        }
        if (Minecraft.useShaderTransparency()) {
            if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_LEVEL) {
                drawAfterWater(event, true);
            }
        } else if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            drawAfterWater(event, false);
        }
    }

    private static void drawAfterWater(RenderLevelStageEvent event, boolean rebuildPose) {
        if (!Config.enabled || Config.strength <= 0.0f || CloakRenderType.get() == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        boolean any = false;
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (SceneCopy.cloaksAfterWater(entity)) {
                any = true;
                break;
            }
        }
        if (!any) {
            return;
        }
        mc.getMainRenderTarget().bindWrite(false);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        SceneCopy.capture();
        if (!SceneCopy.ready()) {
            return;
        }
        PoseStack pose = event.getPoseStack();
        if (rebuildPose) {
            Camera camera = event.getCamera();
            pose = new PoseStack();
            pose.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
            pose.mulPose(Axis.YP.rotationDegrees(camera.getYRot() + 180.0F));
        }
        SceneCopy.afterWater = true;
        try {
            renderCloaked(mc, event, pose);
        } finally {
            SceneCopy.afterWater = false;
        }
    }

    private static void renderCloaked(Minecraft mc, RenderLevelStageEvent event, PoseStack pose) {
        Camera camera = event.getCamera();
        Vec3 cam = camera.getPosition();
        float partialTick = event.getPartialTick();
        Frustum frustum = event.getFrustum();
        EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (!SceneCopy.cloaksAfterWater(entity)) {
                continue;
            }
            if (!dispatcher.shouldRender(entity, frustum, cam.x, cam.y, cam.z) && !entity.hasIndirectPassenger(mc.player)) {
                continue;
            }
            BlockPos pos = entity.blockPosition();
            if (!mc.level.isOutsideBuildHeight(pos.getY()) && !event.getLevelRenderer().isChunkCompiled(pos)) {
                continue;
            }
            if (entity == camera.getEntity() && !camera.isDetached() && !(camera.getEntity() instanceof LivingEntity living && living.isSleeping())) {
                continue;
            }
            if (entity instanceof LocalPlayer && camera.getEntity() != entity) {
                continue;
            }
            double x = Mth.lerp(partialTick, entity.xOld, entity.getX());
            double y = Mth.lerp(partialTick, entity.yOld, entity.getY());
            double z = Mth.lerp(partialTick, entity.zOld, entity.getZ());
            float yRot = Mth.lerp(partialTick, entity.yRotO, entity.getYRot());
            dispatcher.render(entity, x - cam.x, y - cam.y, z - cam.z, yRot, partialTick, pose, buffers, dispatcher.getPackedLightCoords(entity, partialTick));
        }
        buffers.endLastBatch();
        buffers.endBatch();
    }
}
