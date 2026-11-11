package dev.muon.irons_origins;

import com.mojang.logging.LogUtils;
import dev.muon.irons_origins.action.IronsOriginsActions;
import dev.muon.irons_origins.condition.IronsOriginsConditions;
import dev.muon.irons_origins.network.IronsOriginsPayloads;
import dev.muon.irons_origins.power.IronsOriginsPowers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(IronsOrigins.MODID)
public class IronsOrigins {
    public static final String MODID = "irons_origins";
    public static final Logger LOGGER = LogUtils.getLogger();

    public IronsOrigins(IEventBus modBus) {
        IronsOriginsActions.register();
        IronsOriginsConditions.register();
        IronsOriginsPowers.register();

        modBus.addListener(IronsOriginsPayloads::register);
    }

    public static ResourceLocation loc(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
