package com.momosoftworks.momoweave.core.init;

import com.momosoftworks.momoweave.Momoweave;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemInit
{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Momoweave.MOD_ID);

    public static RegistryObject<Item> ROTTEN_LEATHER = ITEMS.register("rotten_leather", () -> new Item(new Item.Properties()));
}
