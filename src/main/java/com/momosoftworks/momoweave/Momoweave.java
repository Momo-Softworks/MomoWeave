package com.momosoftworks.momoweave;

import com.momosoftworks.momoweave.common.capability.BagInventoryCap;
import com.momosoftworks.momoweave.config.MainSettingsConfig;
import com.momosoftworks.momoweave.core.init.*;
import com.momosoftworks.momoweave.core.network.MomoweavePacketHandler;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Momoweave.MOD_ID)
public class Momoweave
{
    public static final String MOD_ID = "momoweave";

    public static final Logger LOGGER = LogManager.getLogger("Momoweave");

    public Momoweave()
    {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        bus.addListener(this::commonSetup);
        bus.addListener(this::registerCaps);

        BiomeCodecInit.BIOME_MODIFIER_SERIALIZERS.register(bus);
        FeatureInit.FEATURES.register(bus);
        BlockInit.BLOCKS.register(bus);
        ItemInit.ITEMS.register(bus);
        BlockEntityInit.BLOCK_ENTITIES.register(bus);
        MenuInit.MENU_TYPES.register(bus);

        MainSettingsConfig.setup();
    }

    public void commonSetup(FMLCommonSetupEvent event)
    {
        event.enqueueWork(() ->
        {
            MomoweavePacketHandler.init();
        });
    }

    public void registerCaps(RegisterCapabilitiesEvent event)
    {
        event.register(BagInventoryCap.class);
    }
}