package com.momosoftworks.momoweave.core.init;

import com.momosoftworks.momoweave.Momoweave;
import com.momosoftworks.momoweave.common.item.DeathBagItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ItemInit
{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Momoweave.MOD_ID);

    public static RegistryObject<Item> ROTTEN_LEATHER = ITEMS.register("rotten_leather",
                                                                       () -> new Item(new Item.Properties()));
    public static RegistryObject<Item> DEATH_BAG = ITEMS.register("death_bag",
                                                                  () -> new DeathBagItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static RegistryObject<Item> TRADING_POST = ITEMS.register("trading_post", () -> new BlockItem(BlockInit.TRADING_POST.get(), new Item.Properties()));
}
