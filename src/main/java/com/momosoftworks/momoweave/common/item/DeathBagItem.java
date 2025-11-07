package com.momosoftworks.momoweave.common.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

public class DeathBagItem extends Item
{
    public DeathBagItem(Properties properties)
    {   super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        player.giveExperiencePoints(stack.getOrCreateTag().getInt("Experience"));
        List<ItemStack> bagItems = deserializeContents(stack);
        for (ItemStack item : bagItems)
        {
            if (!item.isEmpty() && !player.addItem(item))
            {   player.drop(item, false);
            }
        }
        player.setItemInHand(hand, ItemStack.EMPTY);
        return InteractionResultHolder.success(stack);
    }

    public static List<ItemStack> deserializeContents(ItemStack bag)
    {
        List<ItemStack> items = new ArrayList<>();
        ListTag itemsNBT = bag.getOrCreateTag().getList("Items", 10);
        for (int i = 0; i < itemsNBT.size(); i++)
        {
            items.add(ItemStack.of(itemsNBT.getCompound(i)));
        }
        items.removeIf(ItemStack::isEmpty);
        return items;
    }

    public static void serializeContents(ItemStack bag, List<ItemStack> items)
    {
        ListTag itemsNBT = new ListTag();
        for (ItemStack item : items)
        {   itemsNBT.add(item.save(new CompoundTag()));
        }
        bag.getOrCreateTag().put("Items", itemsNBT);
    }
}