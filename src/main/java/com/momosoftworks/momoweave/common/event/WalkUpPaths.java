package com.momosoftworks.momoweave.common.event;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber
public class WalkUpPaths
{
    private static final UUID WALK_UP_PATHS_UUID = UUID.fromString("f5f6f7f8-f9fa-fbfc-fdfd-feff00000000");

    @SubscribeEvent
    public static void walkUpPaths(TickEvent.PlayerTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END)
        {
            Player player = event.player;
            AttributeModifier modifier = new AttributeModifier(WALK_UP_PATHS_UUID, "walk_up_paths", 1, AttributeModifier.Operation.ADDITION);
            AttributeInstance stepHeight = event.player.getAttributes().getInstance(ForgeMod.STEP_HEIGHT_ADDITION.get());
            if (stepHeight == null) return;

            // Create bounding boxes to check for valid packs
            float playerWidth = player.getBbWidth();
            AABB aabbInMotionDirection = new AABB(player.position().x - playerWidth/2, player.position().y, player.position().z - playerWidth/2,
                                             player.position().x + playerWidth/2, player.position().y+1, player.position().z + playerWidth/2).move(player.getDeltaMovement().multiply(2, 0, 2));

            boolean isWalkingOnPath = player.getBlockStateOn().is(Blocks.DIRT_PATH)
                                   || BlockPos.betweenClosedStream(aabbInMotionDirection).anyMatch(pos -> player.level().getBlockState(pos).is(Blocks.DIRT_PATH));

            if (isWalkingOnPath)
            {
                if (!player.getAttributes().hasModifier(ForgeMod.STEP_HEIGHT_ADDITION.get(), WALK_UP_PATHS_UUID))
                {   stepHeight.addTransientModifier(modifier);
                }
            }
            else
            {   stepHeight.removeModifier(WALK_UP_PATHS_UUID);
            }
        }
    }
}
