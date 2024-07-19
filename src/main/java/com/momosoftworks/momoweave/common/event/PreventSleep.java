package com.momosoftworks.momoweave.common.event;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PreventSleep
{
    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent event)
    {
        event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
        event.getEntity().displayClientMessage(Component.literal("You may not rest; it is forbidden."), true);
    }
}

