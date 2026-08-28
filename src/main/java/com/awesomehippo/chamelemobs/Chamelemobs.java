package com.awesomehippo.chamelemobs;

import com.awesomehippo.chamelemobs.client.ClientEvents;
import com.awesomehippo.chamelemobs.config.Config;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(Chamelemobs.MOD_ID)
public class Chamelemobs {
    public static final String MOD_ID = "chamelemobs";

    public Chamelemobs() {
        Config.load();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientEvents.init();
        }
    }
}
