package com.momosoftworks.momoweave.common.event;

import com.momosoftworks.momoweave.Momoweave;
import com.momosoftworks.momoweave.common.item.DeathBagItem;
import com.momosoftworks.momoweave.common.level.save_data.LostDeathBagsData;
import com.momosoftworks.momoweave.config.MainSettingsConfig;
import com.momosoftworks.momoweave.core.init.ItemInit;
import com.momosoftworks.momoweave.event.common.EntityRemovedEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingExperienceDropEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber
public class DeathBagHandler
{
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDrops(LivingDropsEvent event)
    {
        if (!MainSettingsConfig.enableDeathBag.get()) return;

        if (event.getEntity() instanceof Player player)
        {
            if (player.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY))
            {   return;
            }
            // Fill bag with dropped items
            ItemStack bag = new ItemStack(ItemInit.DEATH_BAG.get());
            List<ItemStack> droppedItems = new ArrayList<>();
            event.getDrops().removeIf(drop ->
            {
                ItemStack dropStack = drop.getItem();
                if (!dropStack.isEmpty() && !dropStack.is(ItemInit.DEATH_BAG.get()))
                {   droppedItems.add(dropStack.copy());
                    return true;
                }
                return false;
            });
            if (droppedItems.isEmpty() && player.totalExperience <= 0) return;
            // Spawn bag item
            CompoundTag bagNBT = bag.getOrCreateTag();
            DeathBagItem.serializeContents(bag, droppedItems);
            bagNBT.putUUID("Owner", player.getUUID());
            bagNBT.putUUID("ID", UUID.randomUUID());
            bagNBT.putLong("DeathTime", System.currentTimeMillis());
            bagNBT.putInt("Experience", player.totalExperience);
            bag.setHoverName(Component.translatable("tooltip.momoweave.bag_of_the_perished", player.getDisplayName().getString()));
            ItemEntity bagEntity = player.drop(bag, true, true);
            if (bagEntity != null)
            {   bagEntity.setGlowingTag(true);
                bagEntity.lifespan = MainSettingsConfig.bagExpirationTime.get();
                player.level().addFreshEntity(bagEntity);
            }
        }
    }

    @SubscribeEvent
    public static void cancelExperienceDrop(LivingExperienceDropEvent event)
    {
        if (event.getEntity() instanceof Player)
        {   event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void updateForcedChunksIfBagMoves(EntityEvent.EnteringSection event)
    {
        if (event.getEntity() instanceof ItemEntity itemEntity
        && itemEntity.getItem().is(ItemInit.DEATH_BAG.get())
        && event.didChunkChange())
        {
            ChunkPos oldPos = new ChunkPos(event.getOldPos().x(), event.getOldPos().z());
            ChunkPos newPos = new ChunkPos(event.getNewPos().x(), event.getNewPos().z());
            itemEntity.level().getChunkSource().updateChunkForced(oldPos, false);
            itemEntity.level().getChunkSource().updateChunkForced(newPos, true);
        }
    }

    @SubscribeEvent
    public static void forceChunksOnBagDropped(EntityJoinLevelEvent event)
    {   setChunkForcedForItem(event, true);
    }

    @SubscribeEvent
    public static void unforceChunksOnBagRemoved(EntityRemovedEvent event)
    {   setChunkForcedForItem(event, false);
    }

    private static void setChunkForcedForItem(EntityEvent event, boolean forced)
    {
        Entity entity = event.getEntity();
        if (entity instanceof ItemEntity itemEntity)
        {
            if (itemEntity.getItem().is(ItemInit.DEATH_BAG.get()))
            {
                ChunkPos chunkPos = new ChunkPos(itemEntity.blockPosition());
                entity.level().getChunkSource().updateChunkForced(chunkPos, forced);
            }
        }
    }

    @SubscribeEvent
    public static void onBagDestroyed(EntityRemovedEvent event)
    {
        if (event.getEntity() instanceof ItemEntity itemEntity
        && event.getReason() != Entity.RemovalReason.UNLOADED_TO_CHUNK
        && event.getReason() != Entity.RemovalReason.CHANGED_DIMENSION)
        {
            ItemStack stack = itemEntity.getItem();
            if (stack.is(ItemInit.DEATH_BAG.get()))
            {
                if (!stack.getOrCreateTag().hasUUID("Owner"))
                {   Momoweave.LOGGER.warn("Death bag without owner UUID despawned/destroyed!");
                    return;
                }
                UUID uuid = stack.getOrCreateTag().getUUID("Owner");
                LostDeathBagsData.INSTANCE.addLostBag(uuid, stack.copy(), System.currentTimeMillis());
            }
        }
    }

    @SubscribeEvent
    public static void onTradeWithWanderingTrader(TradeWithVillagerEvent event)
    {
        ItemStack result = event.getMerchantOffer().getResult();
        if (event.getAbstractVillager() instanceof WanderingTrader trader && result.is(ItemInit.DEATH_BAG.get()))
        {
            UUID playerId = event.getEntity().getUUID();
            LostDeathBagsData.INSTANCE.removeLostBag(playerId, result);
            removeBagCharge(trader, playerId, result);
        }
    }

    public static void removeBagCharge(LivingEntity trader, UUID player, ItemStack bag)
    {
        CompoundTag deathBags = trader.getPersistentData().getCompound("DeathBags");
        if (deathBags.contains(player.toString()))
        {
            ListTag deathBagList = deathBags.getList(player.toString(), 10);
            for (int i = 0; i < deathBagList.size(); i++)
            {
                CompoundTag deathBagTag = deathBagList.getCompound(i);
                ItemStack deathBagStack = ItemStack.of(deathBagTag.getCompound("BagItem"));
                if (ItemStack.isSameItemSameTags(deathBagStack, bag))
                {
                    deathBagList.remove(i);
                    break;
                }
            }
        }
    }

    public static void addBagCharge(LivingEntity trader, UUID player, LostDeathBagsData.BagData bagData, ItemStack charge0, ItemStack charge1)
    {
        CompoundTag deathBags = trader.getPersistentData().getCompound("DeathBags");
        ListTag deathBagList;
        if (deathBags.contains(player.toString()))
        {   deathBagList = deathBags.getList(player.toString(), 10);
        }
        else
        {   deathBagList = new ListTag();
            deathBags.put(player.toString(), deathBagList);
        }

        CompoundTag deathBagTag = new CompoundTag();
        deathBagTag.putLong("LostTime", bagData.lostTime());
        deathBagTag.put("charge0", charge0.save(new CompoundTag()));
        deathBagTag.put("charge1", charge1.save(new CompoundTag()));
        deathBagList.add(deathBagTag);
        trader.getPersistentData().put("DeathBags", deathBags);
    }

    public static ItemStack[] getBagCharges(LivingEntity trader, UUID player, LostDeathBagsData.BagData bagData)
    {
        CompoundTag deathBags = trader.getPersistentData().getCompound("DeathBags");
        if (deathBags.contains(player.toString()))
        {
            long bagLostTime = bagData.lostTime();
            ListTag deathBagList = deathBags.getList(player.toString(), 10);
            for (int i = 0; i < deathBagList.size(); i++)
            {
                CompoundTag deathBagTag = deathBagList.getCompound(i);
                long lostTime = deathBagTag.getLong("LostTime");
                if (lostTime == bagLostTime)
                {
                    ItemStack charge0 = ItemStack.of(deathBagTag.getCompound("charge0"));
                    ItemStack charge1 = ItemStack.of(deathBagTag.getCompound("charge1"));
                    return new ItemStack[]{charge0, charge1};
                }
            }
        }
        return new ItemStack[]{ItemStack.EMPTY, ItemStack.EMPTY};
    }
}
