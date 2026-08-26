package com.awesomehippo.chamelemobs;

import com.awesomehippo.chamelemobs.config.Config;
import com.awesomehippo.chamelemobs.config.ConfigScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Chamelemobs.MOD_ID)
public class Chamelemobs {
    public static final String MOD_ID = "chamelemobs";

    public Chamelemobs() {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            Config.load();
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new ConfigScreen(screen)));
        }
    }
}
