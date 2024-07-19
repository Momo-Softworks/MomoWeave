package com.momosoftworks.momoweave.common.event;


import com.momosoftworks.coldsweat.core.event.TaskScheduler;
import com.momosoftworks.momoweave.common.level.EndInitializationStatus;
import com.momosoftworks.momoweave.common.level.SavedDataHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Method;

@Mod.EventBusSubscriber
public class InitializeEnd
{
    private static final Method SPAWN_EXIT_PORTAL = ObfuscationReflectionHelper.findMethod(EndDragonFight.class, "m_64093_", boolean.class);
    static
    {   SPAWN_EXIT_PORTAL.setAccessible(true);
    }

    public static void spawnExitPortal(EndDragonFight fight, boolean isLit)
    {
        try
        {   SPAWN_EXIT_PORTAL.invoke(fight, isLit);
        }
        catch (Exception e)
        {   e.printStackTrace();
        }
    }

    @SubscribeEvent
    public static void initializeEndPortal(EntityJoinLevelEvent event)
    {
        Level level = event.getLevel();
        if (level.isClientSide || !(level instanceof ServerLevel serverLevel))
        {   return;
        }
        if (level.dimension() == Level.END)
        {
            TaskScheduler.scheduleServer(() ->
            {
                EndInitializationStatus status = SavedDataHelper.getInitializationStatus(serverLevel);
                if (!status.isInitialized())
                {
                    for (int x = -1; x < 1; x++)
                    {
                        for (int z = -1; z < 1; z++)
                        {   serverLevel.setChunkForced(x, z, true);
                        }
                    }
                    spawnExitPortal(serverLevel.getDragonFight(), true);
                    status.setInitialized(true);
                }
            }, 20);
        }
    }
}
