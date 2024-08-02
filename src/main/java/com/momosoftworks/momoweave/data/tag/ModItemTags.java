package com.momosoftworks.momoweave.data.tag;

import com.momosoftworks.momoweave.Momoweave;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModItemTags
{
    public static final TagKey<Item> BUYBACK_PIDDLING = createTag("trader_buyback/piddling");
    public static final TagKey<Item> BUYBACK_CHEAP = createTag("trader_buyback/cheap");
    public static final TagKey<Item> BUYBACK_COSTLY = createTag("trader_buyback/costly");
    public static final TagKey<Item> BUYBACK_EXTORTIONATE = createTag("trader_buyback/extortionate");

    private static TagKey<Item> createTag(String name)
    {
        return ItemTags.create(new ResourceLocation(Momoweave.MOD_ID, name));
    }
}
