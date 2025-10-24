package com.momosoftworks.momoweave.common.event;

import com.momosoftworks.coldsweat.core.event.TaskScheduler;
import com.momosoftworks.momoweave.config.MainSettingsConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.event.entity.player.SleepingTimeCheckEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PreventSleep
{
    @SubscribeEvent
    public static void onSleep(SleepingTimeCheckEvent event)
    {
        if (MainSettingsConfig.preventSleep.get())
        {
            event.setResult(Event.Result.DENY);
            TaskScheduler.scheduleServer(() ->
            {   event.getEntity().displayClientMessage(Component.literal("You may not rest; it is forbidden."), true);
            }, 0);
        }
    }
}