package com.awesomehippo.chamelemobs;

import com.awesomehippo.chamelemobs.client.ClientEvents;
import com.awesomehippo.chamelemobs.config.Config;
import com.awesomehippo.chamelemobs.network.Network;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Chamelemobs.MOD_ID)
public class Chamelemobs {
    public static final String MOD_ID = "chamelemobs";

    public Chamelemobs() {
        Config.load();
        Network.register();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ClientEvents.init();
        }
    }
}
