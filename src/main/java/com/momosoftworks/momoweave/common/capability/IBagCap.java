package com.momosoftworks.momoweave.common.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.Collection;

public interface IBagCap extends INBTSerializable<CompoundTag>
{
    Collection<ItemStack> getItems();
    void addItem(ItemStack item);
    void removeItem(ItemStack item);
    void clearItems();
}
