package com.momosoftworks.momoweave.core.init;

import com.momosoftworks.momoweave.client.gui.LootTableScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ScreenInit
{
    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent event)
    {   MenuScreens.register(MenuInit.LOOT_MENU_TYPE.get(), LootTableScreen::new);
    }
}
