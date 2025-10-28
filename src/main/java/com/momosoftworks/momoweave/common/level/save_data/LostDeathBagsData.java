package com.momosoftworks.momoweave.common.level.save_data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.UUID;

public class LostDeathBagsData extends SavedData
{
    private final Multimap<UUID, BagData> lostBags = HashMultimap.create();

    public LostDeathBagsData()
    {}

    public Multimap<UUID, BagData> getLostBags()
    {   return lostBags;
    }

    public void addLostBag(UUID uuid, ItemStack bag, long timestamp)
    {   lostBags.put(uuid, new BagData(bag, timestamp));
        this.setDirty();
    }

    public void removeLostBag(UUID uuid, ItemStack bag)
    {
        lostBags.entries().removeIf(entry -> entry.getKey().equals(uuid) && ItemStack.isSameItemSameTags(entry.getValue().bag(), bag));
        this.setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag nbt)
    {
        CompoundTag bagsNBT = new CompoundTag();
        for (Map.Entry<UUID, BagData> entry : lostBags.entries())
        {
            BagData bagData = entry.getValue();
            String playerId = entry.getKey().toString();
            if (!bagsNBT.contains(playerId))
            {   bagsNBT.put(playerId, new ListTag());
            }
            CompoundTag bagNBT = new CompoundTag();
            bagNBT.put("BagItem", bagData.bag().save(new CompoundTag()));
            bagNBT.putLong("LostTime", bagData.lostTime);
            bagsNBT.getList(playerId, 10).add(bagNBT);
        }
        nbt.put("DeathBags", bagsNBT);
        return nbt;
    }

    public static LostDeathBagsData load(CompoundTag nbt)
    {
        LostDeathBagsData data = new LostDeathBagsData();
        CompoundTag bagsNBT = nbt.getCompound("DeathBags");
        for (String playerId : bagsNBT.getAllKeys())
        {
            ListTag bagListNBT = bagsNBT.getList(playerId, 10);
            UUID uuid = UUID.fromString(playerId);
            for (int i = 0; i < bagListNBT.size(); i++)
            {
                CompoundTag bagNBT = bagListNBT.getCompound(i);
                ItemStack bag = ItemStack.of(bagNBT.getCompound("BagItem"));
                long lostTime = bagNBT.getLong("LostTime");
                data.lostBags.put(uuid, new BagData(bag, lostTime));
            }
        }
        return data;
    }

    public record BagData(ItemStack bag, long lostTime)
    {}
}
