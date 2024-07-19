package com.momosoftworks.momoweave.common.event;

import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber
public class PreventFlight
{
    @SubscribeEvent
    public static void preventCreativeFlight(TickEvent.PlayerTickEvent event)
    {
        Player player = event.player;
        if (player.getAbilities().flying && !(player.isCreative() || player.isSpectator()))
        {   player.getAbilities().flying = false;
        }
    }
}
