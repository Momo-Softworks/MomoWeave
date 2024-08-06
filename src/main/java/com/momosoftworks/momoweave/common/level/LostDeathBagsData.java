package com.momosoftworks.momoweave.common.level;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import com.momosoftworks.momoweave.Momoweave;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.*;

public class LostDeathBagsData extends SavedData
{
    public static final int BAG_EXPIRATION_TIME = 20*60*7;

    private final Multimap<UUID, ItemStack> lostBags = HashMultimap.create();

    private final Map<UUID, Integer> wanderingTraderSpawnTicker = new HashMap<>();

    public LostDeathBagsData()
    {}

    public Multimap<UUID, ItemStack> getLostBags()
    {   return lostBags;
    }

    public void addLostBag(UUID uuid, ItemStack bag)
    {   lostBags.put(uuid, bag);
        this.setDirty();
    }

    public void removeLostBag(UUID uuid, ItemStack bag)
    {   lostBags.remove(uuid, bag);
        this.setDirty();
    }

    public void tick()
    {   this.tickTraderCountdowns();
    }

    public void tickTraderCountdowns()
    {
        Iterator<Map.Entry<UUID, Integer>> iterator = wanderingTraderSpawnTicker.entrySet().iterator();
        while (iterator.hasNext())
        {
            Map.Entry<UUID, Integer> entry = iterator.next();
            int ticks = entry.getValue();
            if (ticks <= 0)
            {
                iterator.remove();
                Level overworld = CSMath.getIfNotNull(WorldHelper.getServer(), MinecraftServer::overworld, null);
                if (overworld == null)
                {   Momoweave.LOGGER.error("Failed to spawn wandering trader because the overworld could not be found.");
                }
                else spawnWanderingTrader(overworld, entry.getKey());
            }
            else
            {   wanderingTraderSpawnTicker.put(entry.getKey(), ticks - 1);
            }
        }
    }

    public void scheduleWanderingTrader(UUID player)
    {
        // Random delay between 30 seconds and 2 minutes
        int traderSpawnTime = new Random().nextInt(20*30, 20*60*2);
        // Add the wandering trader to the scheduler
        wanderingTraderSpawnTicker.computeIfAbsent(player, p ->
        {
            this.setDirty();
            return traderSpawnTime;
        });
    }

    public static void spawnWanderingTrader(Level level, UUID playerId)
    {
        BlockPos pPos;
        // Try to spawn the wandering trader on the player that died, if they exist
        Player playerThatDied = level.getPlayerByUUID(playerId);
        if (playerThatDied != null)
        {   pPos = playerThatDied.blockPosition();
        }
        // Otherwise, spawn the trader at the world's default spawn point
        else pPos = level.getSharedSpawnPos();

        // Try to find a suitable spawn position
        BlockPos spawnPosition = null;
        int maxDistance = 16;
        for (int i = 0; i < 10; ++i)
        {
            int j = pPos.getX() + level.random.nextInt(maxDistance * 2) - maxDistance;
            int k = pPos.getZ() + level.random.nextInt(maxDistance * 2) - maxDistance;
            int l = level.getHeight(Heightmap.Types.WORLD_SURFACE, j, k);
            BlockPos blockpos1 = new BlockPos(j, l, k);
            if (NaturalSpawner.isSpawnPositionOk(SpawnPlacements.Type.ON_GROUND, level, blockpos1, EntityType.WANDERING_TRADER))
            {
                spawnPosition = blockpos1;
                break;
            }
        }
        if (spawnPosition == null)
        {   Momoweave.LOGGER.error("Failed to spawn wandering trader because a suitable spawn position could not be found.");
            return;
        }

        // Prevent wandering trader spawn if there's already one nearby
        AABB checkArea = new AABB(spawnPosition).inflate(maxDistance * 2);
        if (!level.getEntitiesOfClass(WanderingTrader.class, checkArea).isEmpty())
        {   return;
        }

        // Spawn the wandering trader
        WanderingTrader wanderingTrader = new WanderingTrader(EntityType.WANDERING_TRADER, level);
        wanderingTrader.setPos(CSMath.getCenterPos(spawnPosition));
        wanderingTrader.addEffect(new MobEffectInstance(MobEffects.GLOWING, 20*60));
        level.addFreshEntity(wanderingTrader);
    }

    @Override
    public CompoundTag save(CompoundTag nbt)
    {
        CompoundTag bagsNBT = new CompoundTag();
        for (Map.Entry<UUID, ItemStack> entry : lostBags.entries())
        {
            String playerId = entry.getKey().toString();
            if (!bagsNBT.contains(playerId))
            {   bagsNBT.put(playerId, new ListTag());
            }
            bagsNBT.getList(playerId, 10).add(entry.getValue().save(new CompoundTag()));
        }
        nbt.put("DeathBags", bagsNBT);

        ListTag wanderingTraderSpawnTimes = new ListTag();
        for (Map.Entry<UUID, Integer> entry : wanderingTraderSpawnTicker.entrySet())
        {
            CompoundTag spawnNBT = new CompoundTag();
            spawnNBT.putUUID("Player", entry.getKey());
            spawnNBT.putInt("Ticks", entry.getValue());
            wanderingTraderSpawnTimes.add(spawnNBT);
        }
        nbt.put("WanderingTraderSpawnTimes", wanderingTraderSpawnTimes);
        return nbt;
    }

    public static LostDeathBagsData load(CompoundTag nbt)
    {
        LostDeathBagsData data = new LostDeathBagsData();
        ListTag bags = nbt.getList("Bags", 10);
        for (int i = 0; i < bags.size(); i++)
        {
            CompoundTag bag = bags.getCompound(i);
            UUID uuid = bag.getUUID("UUID");
            ItemStack stack = ItemStack.of(bag.getCompound("Bag"));
            data.lostBags.put(uuid, stack);
        }

        ListTag wanderingTraderSpawnTimes = nbt.getList("WanderingTraderSpawnTimes", 10);
        for (int i = 0; i < wanderingTraderSpawnTimes.size(); i++)
        {
            CompoundTag spawnNBT = wanderingTraderSpawnTimes.getCompound(i);
            UUID player = spawnNBT.getUUID("Player");
            int ticks = spawnNBT.getInt("Ticks");
            data.wanderingTraderSpawnTicker.put(player, ticks);
        }
        return data;
    }
}
