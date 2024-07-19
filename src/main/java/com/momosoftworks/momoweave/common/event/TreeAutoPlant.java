package com.momosoftworks.momoweave.common.event;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class TreeAutoPlant
{
    @SubscribeEvent
    public static void autoPlantSaplings(ItemExpireEvent event)
    {
        ItemStack item = event.getEntity().getItem();
        Level level = event.getEntity().level();
        BlockPos pos = event.getEntity().blockPosition();
        if (item.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof SaplingBlock
        && level.getBlockState(pos).canBeReplaced())
        {
            BlockHitResult hit = new BlockHitResult(event.getEntity().position(), Direction.UP, pos, false);
            BlockPlaceContext context = new BlockPlaceContext(event.getEntity().level(), null, InteractionHand.MAIN_HAND, item, hit);
            blockItem.place(context);
        }
    }
}
