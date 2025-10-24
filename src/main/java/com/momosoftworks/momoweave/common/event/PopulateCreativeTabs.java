package com.momosoftworks.momoweave.common.event;

import com.momosoftworks.momoweave.core.init.ItemInit;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)
public class PopulateCreativeTabs
{
    @SubscribeEvent
    public static void addCreativeItems(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS)
        {
            event.getEntries().putAfter(Items.LEATHER.getDefaultInstance(), ItemInit.ROTTEN_LEATHER.get().getDefaultInstance(),
                                        CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }

        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS)
        {
            event.getEntries().putAfter(Items.CARTOGRAPHY_TABLE.getDefaultInstance(), ItemInit.TRADING_POST.get().getDefaultInstance(),
                                        CreativeModeTab.TabVisibility.PARENT_AND_SEARCH_TABS);
        }
    }
}
