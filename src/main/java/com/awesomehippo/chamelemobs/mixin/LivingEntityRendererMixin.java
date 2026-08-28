package com.awesomehippo.chamelemobs.mixin;

import com.awesomehippo.chamelemobs.client.CloakRenderType;
import com.awesomehippo.chamelemobs.client.SceneCopy;
import com.awesomehippo.chamelemobs.config.Config;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void chamelemobs$cloak(T entity, boolean bodyVisible, boolean translucent, boolean glowing, CallbackInfoReturnable<RenderType> cir) {
        if (!bodyVisible || !Config.shouldCloak(entity) || CloakRenderType.get() == null || !SceneCopy.ready()) {
            return;
        }
        LivingEntityRenderer<T, M> renderer = (LivingEntityRenderer<T, M>) (Object) this;
        cir.setReturnValue(CloakRenderType.cloak(renderer.getTextureLocation(entity)));
    }
}
