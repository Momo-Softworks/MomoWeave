package com.momosoftworks.momoweave.common.event;

import com.momosoftworks.momoweave.config.MainSettingsConfig;
import com.momosoftworks.momoweave.util.SchedulerHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber
public class AnimalRepopulation
{
    public static final int MAX_ANIMALS_PER_CHUNK = 4;

    @SubscribeEvent
    public static void timestampAnimalUnload(EntityLeaveLevelEvent event)
    {
        if (event.getEntity() instanceof AgeableMob)
        {   event.getEntity().getPersistentData().putLong("UnloadedTimestamp", System.currentTimeMillis() / 1000L);
        }
    }

    @SubscribeEvent
    public static void repopulateAnimalsOnLoad(EntityJoinLevelEvent event)
    {
        if (event.getLevel().isClientSide || !(event.getEntity() instanceof AgeableMob entity))
        {   return;
        }
        int animalBreedTime = MainSettingsConfig.averageAnimalBreedingTimeSecs.get();
        Level level = event.getLevel();
        // Make a new object that keeps registering itself to the Task Scheduler until the chunk is loaded
            RandomSource rand = level.getRandom();
            AABB partnerSearchBounds = new AABB(entity.blockPosition()).inflate(32, 32, 32);
            long unloadedTimestamp = entity.getPersistentData().getLong("UnloadedTimestamp");
            long currentTime = System.currentTimeMillis() / 1000L;

            // Don't breed if this entity is a baby
            if (unloadedTimestamp != 0)
            {   entity.ageUp((int) (currentTime - unloadedTimestamp));
            }
            else return;
            if (entity.isBaby()) return;

            // Search for partners to repopulate with
            List<Entity> availablePartners = level.getEntities(entity, partnerSearchBounds, ent -> ent.getType() == entity.getType() && ent instanceof AgeableMob);
            int entitiesInRange = availablePartners.size();
            // Breed with partners
            for (Entity nearbyEntity : availablePartners)
            {
                AgeableMob partner = (AgeableMob) nearbyEntity;
                if (nearbyEntity.getPersistentData().contains("UnloadedTimestamp"))
                {
                    long partnerUnloadedTimestamp = nearbyEntity.getPersistentData().getLong("UnloadedTimestamp");
                    long timeSinceUnload = currentTime - Math.min(unloadedTimestamp, partnerUnloadedTimestamp);
                    partner.ageUp((int) (currentTime - partnerUnloadedTimestamp));

                    try
                    {
                        // Don't breed with babies!
                        if (partner.isBaby()) continue;

                        while (timeSinceUnload > 0)
                        {
                            if (entitiesInRange < MAX_ANIMALS_PER_CHUNK)
                            {
                                if (rand.nextInt(((int) timeSinceUnload)) > 2
                                && level instanceof ServerLevel serverLevel)
                                {
                                    AgeableMob baby = entity.getBreedOffspring(serverLevel, partner);
                                    if (baby != null)
                                    {
                                        baby.setPos(entity.getX(), entity.getY(), entity.getZ());
                                        baby.setAge(-rand.nextInt(12000, 36000));
                                        level.addFreshEntity(baby);
                                        entitiesInRange++;
                                    }
                                }
                                timeSinceUnload -= animalBreedTime;
                            }
                            else break;
                        }
                    }
                    finally
                    {   // Invalidate this partner for further breeding
                        partner.getPersistentData().remove("UnloadedTimestamp");
                    }
                }
            }
            // Invalidate this entity for further breeding
            entity.getPersistentData().remove("UnloadedTimestamp");
    }
}
