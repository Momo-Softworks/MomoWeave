package com.momosoftworks.momoweave.data.tag;

import com.momosoftworks.momoweave.Momoweave;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class ModItemTags
{
    private static TagKey<Item> createTag(String name)
    {
        return ItemTags.create(new ResourceLocation(Momoweave.MOD_ID, name));
    }
}
