package com.momosoftworks.momoweave.common.event;

import com.momosoftworks.momoweave.Momoweave;
import com.momosoftworks.momoweave.core.init.ItemInit;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;

@Mod.EventBusSubscriber
public class RemapMissingIDs
{
    @SubscribeEvent
    public static void remapMissingItems(MissingMappingsEvent event)
    {
        if (event.getRegistry().getRegistryName().equals(ForgeRegistries.ITEMS.getRegistryName()))
        {
            for (MissingMappingsEvent.Mapping<Item> mapping : event.getAllMappings(ForgeRegistries.Keys.ITEMS))
            {
                ResourceLocation key = mapping.getKey();
                String namespace = key.getNamespace();
                String path = key.getPath();
                if (namespace.equals(Momoweave.MOD_ID))
                {
                    switch (path)
                    {
                        case "bag_of_the_perished" : mapping.remap(ItemInit.DEATH_BAG.get()); break;
                        default: break;
                    }
                }
            }
        }
    }
}