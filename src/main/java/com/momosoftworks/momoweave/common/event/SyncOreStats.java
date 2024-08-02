package com.momosoftworks.momoweave.common.event;

import com.momosoftworks.coldsweat.core.event.TaskScheduler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class SyncOreStats
{
    @SubscribeEvent
    public static void sendStatsOnOreBroken(BlockEvent.BreakEvent event)
    {
        if (event.getPlayer() instanceof ServerPlayer serverPlayer)
        {
            if (event.getState().is(Tags.Blocks.ORES))
            {
                TaskScheduler.scheduleServer(() -> serverPlayer.getStats().sendStats(serverPlayer), 2);
            }
        }
    }

    @SubscribeEvent
    public static void sendStatsOnJoin(EntityJoinLevelEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer serverPlayer)
        {   serverPlayer.getStats().sendStats(serverPlayer);
        }
    }
}
