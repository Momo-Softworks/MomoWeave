package com.momosoftworks.momoweave.common.container;

import com.momosoftworks.coldsweat.core.event.TaskScheduler;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import com.momosoftworks.momoweave.core.init.MenuInit;
import com.momosoftworks.momoweave.core.network.MomoweavePacketHandler;
import com.momosoftworks.momoweave.core.network.message.SyncLootContainerMessage;
import net.minecraft.core.NonNullList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.util.*;

@Mod.EventBusSubscriber
public class LootTableMenu extends AbstractContainerMenu
{
    private static final int SIZE = 288;

    LootInventory lootInventory;
    ResourceLocation lootTableName = new ResourceLocation("");
    Map<ItemStack, Integer> lootRolls = new HashMap<>();

    @Override
    public void sendAllDataToRemote()
    {
        super.sendAllDataToRemote();
    }

    public LootTableMenu(int containerId, Inventory playerInv, FriendlyByteBuf friendlyByteBuf)
    {
        super(MenuInit.LOOT_MENU_TYPE.get(), containerId);
        this.lootInventory = new LootInventory(this);

        int slotId = 0;
        for (int i = 0; i < SIZE; i++)
        {
            Slot slot = new Slot(this.lootInventory, slotId, -110 + (slotId % 16) * 24, 18 + (slotId / 16) * 32)
            {
                @Override
                public boolean mayPickup(Player pPlayer)
                {   return false;
                }

                @Override
                public boolean mayPlace(ItemStack pStack)
                {   return false;
                }
            };
            this.addSlot(slot);
            slotId++;
        }
    }

    public LootTableMenu(int containerId, ResourceLocation lootTableName, ServerPlayer opener)
    {
        this(containerId, (Inventory) null, (FriendlyByteBuf) null);
        this.lootTableName = lootTableName;

        LootParams params = new LootParams.Builder(((ServerLevel) opener.level()))
                .withParameter(LootContextParams.ORIGIN, opener.position())
                .create(LootContextParamSets.CHEST);
        LootTable lootTable = opener.server.getLootData().getLootTable(lootTableName);

        // Add the item stacks from this loot table to the lootRolls map, rolling the loot table 100 times
        // Merge items if they are the same
        try
        {
            for (int i = 0; i < 100; i++)
            {
                for (ItemStack stack : lootTable.getRandomItems(params))
                {
                    replaceTags(stack);
                    lootRoll:
                    {
                        for (ItemStack lootRoll : lootRolls.keySet())
                        {
                            replaceTags(lootRoll);
                            if (ItemStack.isSameItemSameTags(lootRoll, stack))
                            {
                                lootRoll.grow(stack.getCount());
                                lootRolls.put(lootRoll, lootRoll.getCount());
                                break lootRoll;
                            }
                        }
                        lootRolls.put(stack, stack.getCount());
                    }
                }
            }
        }
        catch (Exception e)
        {   e.printStackTrace();
        }

        // We have the counts for all item stacks stored. Set the counts to 1
        for (ItemStack itemStack : lootRolls.keySet())
        {   itemStack.setCount(1);
        }

        // Sort by item names
        lootRolls = lootRolls.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(o -> ForgeRegistries.ITEMS.getKey(o.getItem()).getPath())))
                .collect(LinkedHashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), Map::putAll);

        int slotId = 0;
        for (ItemStack stack : lootRolls.keySet())
        {
            if (slotId >= SIZE)
            {   break;
            }
            this.getSlot(slotId).set(stack);
            slotId++;
        }
        TaskScheduler.scheduleServer(() -> MomoweavePacketHandler.INSTANCE.send(PacketDistributor.PLAYER.with(() -> opener), new SyncLootContainerMessage(lootRolls, this.lootTableName)), 2);
    }

    public Map<ItemStack, Integer> getLootRolls()
    {   return lootRolls;
    }

    public void setLootRolls(Map<ItemStack, Integer> lootRolls)
    {   this.lootRolls = lootRolls;
    }

    public ResourceLocation getLootTableName()
    {   return lootTableName;
    }

    public void setLootTableName(ResourceLocation lootTableName)
    {   this.lootTableName = lootTableName;
    }

    @Override
    public ItemStack quickMoveStack(Player pPlayer, int pIndex)
    {   return null;
    }

    @Override
    public boolean stillValid(Player pPlayer)
    {   return true;
    }

    public static class LootInventory implements Container
    {
        private final NonNullList<ItemStack> stackList;
        private final LootTableMenu menu;

        public LootInventory(LootTableMenu menu)
        {
            this.stackList = NonNullList.withSize(SIZE, ItemStack.EMPTY);
            this.menu = menu;
        }

        @Override
        public int getContainerSize()
        {   return SIZE;
        }

        @Override
        public boolean isEmpty()
        {   return stackList.stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack removeItem(int index, int count)
        {
            ItemStack itemstack = ContainerHelper.removeItem(this.stackList, index, count);
            if (!itemstack.isEmpty())
            {   this.menu.slotsChanged(this);
            }

            return itemstack;
        }

        @Override
        public ItemStack removeItemNoUpdate(int index)
        {   return ContainerHelper.takeItem(this.stackList, index);
        }

        @Override
        public ItemStack getItem(int pSlot)
        {   return stackList.get(pSlot);
        }

        @Override
        public void setItem(int slot, ItemStack stack)
        {   stackList.set(slot, stack);
            menu.slotsChanged(this);
        }

        @Override
        public void setChanged()
        {   menu.slotsChanged(this);
        }

        @Override
        public boolean stillValid(Player pPlayer)
        {   return true;
        }

        @Override
        public void clearContent()
        {   Collections.fill(stackList, ItemStack.EMPTY);
        }
    }

    private static void replaceTags(ItemStack stack)
    {
        if (!EnchantmentHelper.getEnchantments(stack).isEmpty())
        {   stack.getOrCreateTag().remove("Enchantments");
            stack.getOrCreateTag().remove("StoredEnchantments");
            stack.getOrCreateTag().putBoolean("momo:Enchanted", true);
        }
        if (NBTHelper.getTagOrEmpty(stack).contains("affixes"))
        {   stack.getOrCreateTag().remove("affixes");
            stack.getOrCreateTag().putBoolean("momo:Affixed", true);
        }
    }

    static final Field ENTRIES;
    static final Field POOLS;
    static final Field CONTAINER_ITEMS;
    static
    {
        POOLS = ObfuscationReflectionHelper.findField(LootTable.class, "f_79109_");
        ENTRIES = ObfuscationReflectionHelper.findField(LootPool.class, "f_79023_");
        CONTAINER_ITEMS = ObfuscationReflectionHelper.findField(SimpleContainer.class, "f_19147_");
        POOLS.setAccessible(true);
        ENTRIES.setAccessible(true);
        CONTAINER_ITEMS.setAccessible(true);
    }
}
