package com.momosoftworks.momoweave.util;

import com.momosoftworks.coldsweat.core.event.TaskScheduler;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkStatus;

import java.util.function.Supplier;

public class SchedulerHelper
{
    /**
     * Adds the given task to the {@link TaskScheduler}. <br>
     * If the given area of the world is not loaded, the task re-schedules itself until it is loaded.
     */
    public static void scheduleUntilLoaded(Runnable task, int delay, Supplier<Level> level, Supplier<BlockPos> posGetter)
    {
        TaskScheduler.scheduleServer(() ->
        {
            new Object()
            {
                public void checkIfLoadedAndExecute(Level level, BlockPos pos)
                {
                    if (level == null || (level instanceof ServerLevel serverLevel
                                          && (!serverLevel.getChunkSource().hasChunk(pos.getX() << 4, pos.getZ() << 4)
                                              || level.getChunk(pos).getStatus() != ChunkStatus.FULL)))
                    {   TaskScheduler.scheduleServer(() -> this.checkIfLoadedAndExecute(level, pos), 2);
                    }
                    else
                    {   task.run();
                    }
                }
            }.checkIfLoadedAndExecute(level.get(), level.get() != null
                                                   ? posGetter.get()
                                                   : null);
        }, delay);
    }

    public static void scheduleUntilLoaded(Runnable task, int delay, Level level, BlockPos pos)
    {   scheduleUntilLoaded(task, delay, () -> level, () -> pos);
    }
}
