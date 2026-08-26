package com.awesomehippo.chamelemobs.mixin;

import com.awesomehippo.chamelemobs.client.CloakRenderType;
import com.awesomehippo.chamelemobs.client.SceneCopy;
import com.awesomehippo.chamelemobs.config.Config;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Inject(method = "render(Lnet/minecraft/world/entity/Entity;DDDFFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V", at = @At("HEAD"), cancellable = true)
    private void chamelemobs$defer(Entity entity, double x, double y, double z, float yaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo ci) {
        if (!SceneCopy.afterWater && SceneCopy.cloaksAfterWater(entity)) {
            ci.cancel();
        }
    }

    @Inject(method = "renderShadow", at = @At("HEAD"), cancellable = true)
    private static void chamelemobs$noShadow(PoseStack poseStack, MultiBufferSource bufferSource, Entity entity, float darkness, float partialTicks, LevelReader level, float radius, CallbackInfo ci) {
        if (Config.shouldCloak(entity) && CloakRenderType.get() != null && SceneCopy.ready()) {
            ci.cancel();
        }
    }
}
