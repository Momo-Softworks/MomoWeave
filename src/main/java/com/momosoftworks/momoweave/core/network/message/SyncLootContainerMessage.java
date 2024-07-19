package com.momosoftworks.momoweave.core.network.message;

import com.momosoftworks.coldsweat.util.world.WorldHelper;
import com.momosoftworks.momoweave.client.gui.LootTablesSelectionScreen;
import com.momosoftworks.momoweave.common.container.LootTableMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootDataType;
import net.minecraftforge.network.NetworkEvent;

import java.util.*;
import java.util.function.Supplier;

public class SyncLootContainerMessage
{
    Map<ItemStack, Integer> loot;
    ResourceLocation lootTableName;
    List<ResourceLocation> lootTables;

    public SyncLootContainerMessage(Map<ItemStack, Integer> loot, ResourceLocation lootTableName)
    {
        this.loot = loot;
        this.lootTableName = lootTableName;
    }

    private SyncLootContainerMessage(Map<ItemStack, Integer> loot, ResourceLocation lootTableName, List<ResourceLocation> lootTables)
    {
        this(loot, lootTableName);
        this.lootTables = lootTables;
    }

    public static void encode(SyncLootContainerMessage message, FriendlyByteBuf buffer)
    {
        buffer.writeResourceLocation(message.lootTableName);

        List<ResourceLocation> lootTables = new ArrayList<>(WorldHelper.getServer().getLootData().getKeys(LootDataType.TABLE));
        lootTables.removeIf(location ->
        {   return !(location.getPath().contains("chests/") || location.getPath().contains("entities/"));
        });
        lootTables.sort(Comparator.comparing(ResourceLocation::toString));
        buffer.writeInt(lootTables.size());
        for (ResourceLocation location : lootTables)
        {   buffer.writeResourceLocation(location);
        }

        buffer.writeInt(message.loot.size());
        for (Map.Entry<ItemStack, Integer> entry : message.loot.entrySet())
        {
            buffer.writeItem(entry.getKey());
            buffer.writeInt(entry.getValue());
        }
    }

    public static SyncLootContainerMessage decode(FriendlyByteBuf buffer)
    {
        ResourceLocation lootTableName = buffer.readResourceLocation();

        List<ResourceLocation> lootTables = new ArrayList<>();
        int size = buffer.readInt();
        for (int i = 0; i < size; i++)
        {
            lootTables.add(buffer.readResourceLocation());
        }

        Map<ItemStack, Integer> loot = new HashMap<>();
        size = buffer.readInt();
        for (int i = 0; i < size; i++)
        {
            loot.put(buffer.readItem(), buffer.readInt());
        }
        return new SyncLootContainerMessage(loot, lootTableName, lootTables);
    }

    public static void handle(SyncLootContainerMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        if (context.getDirection().getReceptionSide().isClient())
        {
            context.enqueueWork(() ->
            {
                if (Minecraft.getInstance().screen instanceof AbstractContainerScreen<?> screen && screen.getMenu() instanceof LootTableMenu menu)
                {
                    menu.setLootRolls(message.loot);
                    menu.setLootTableName(message.lootTableName);
                    LootTablesSelectionScreen.ALL_LOOT_TABLES.clear();
                    LootTablesSelectionScreen.ALL_LOOT_TABLES.addAll(message.lootTables);
                }
            });
        }
        context.setPacketHandled(true);
    }
}