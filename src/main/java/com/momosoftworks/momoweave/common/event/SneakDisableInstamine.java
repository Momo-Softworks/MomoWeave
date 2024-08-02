package com.momosoftworks.momoweave.common.event;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class SneakDisableInstamine
{
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void changeMiningSpeed(PlayerEvent.BreakSpeed event)
    {
        if (event.getNewSpeed() >= 45 && event.getEntity().isCrouching())
        {   event.setNewSpeed(44.5f);
        }
    }
}
