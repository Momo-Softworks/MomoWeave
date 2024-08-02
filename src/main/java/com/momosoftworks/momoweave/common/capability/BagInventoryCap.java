package com.momosoftworks.momoweave.common.capability;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class BagInventoryCap implements IBagCap
{
    private final List<ItemStack> items = new ArrayList<>();

    @Override
    public Collection<ItemStack> getItems()
    {   return items;
    }

    @Override
    public void addItem(ItemStack item)
    {   items.add(item);
    }

    @Override
    public void removeItem(ItemStack item)
    {   items.remove(item);
    }

    @Override
    public void clearItems()
    {   items.clear();
    }

    @Override
    public CompoundTag serializeNBT()
    {
        CompoundTag nbt = new CompoundTag();
        ListTag itemsNBT = new ListTag();
        for (ItemStack item : this.items)
        {   itemsNBT.add(item.save(new CompoundTag()));
        }
        nbt.put("Items", itemsNBT);
        return nbt;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt)
    {
        ListTag itemsNBT = nbt.getList("Items", 10);
        for (int i = 0; i < itemsNBT.size(); i++)
        {   this.items.add(ItemStack.of(itemsNBT.getCompound(i)));
        }
    }
}
