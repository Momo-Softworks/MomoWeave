package com.momosoftworks.momoweave.common.event;

import com.momosoftworks.momoweave.config.MainSettingsConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
public class DisableFarMobs
{
    public static final List<AABB> PLAYER_ZONES = new ArrayList<>();

    @SubscribeEvent
    public static void calculatePlayerZones(TickEvent.LevelTickEvent event)
    {
        if (event.phase == TickEvent.Phase.END && event.level instanceof ServerLevel level && level.getGameTime() % 20 == 0)
        {
            int maxMobDistance = MainSettingsConfig.maxTickingMonsterDistance.get();

            PLAYER_ZONES.clear();
            for (Player player : event.level.players())
            {
                PLAYER_ZONES.add(new AABB(player.position().x - maxMobDistance, player.position().y - maxMobDistance, player.position().z - maxMobDistance,
                                          player.position().x + maxMobDistance, player.position().y + maxMobDistance, player.position().z + maxMobDistance));
            }

            for (Entity entity : level.getEntities().getAll())
            {
                if (entity instanceof Monster mob && !mob.getType().is(Tags.EntityTypes.BOSSES))
                {
                    if (PLAYER_ZONES.stream().anyMatch(zone -> zone.contains(mob.position())))
                    {   mob.setNoAi(false);
                    }
                    else
                    {   mob.setNoAi(true);
                    }
                }
            }
        }
    }
}
