package com.momosoftworks.momoweave.common.item;

import com.momosoftworks.momoweave.common.capability.ModCapabilities;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BagOfThePerishedItem extends Item
{
    public BagOfThePerishedItem(Properties properties)
    {   super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand)
    {
        ItemStack stack = player.getItemInHand(hand);
        stack.getCapability(ModCapabilities.DEATH_POUCH_ITEMS).ifPresent(bag ->
        {
            for (ItemStack item : bag.getItems())
            {
                if (!stack.isEmpty())
                {
                    if (!player.addItem(item))
                    {   player.drop(item, false);
                    }
                }
            }
            player.setItemInHand(hand, ItemStack.EMPTY);
        });
        return InteractionResultHolder.success(stack);
    }
}