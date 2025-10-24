package com.momosoftworks.momoweave.common.event;

import com.momosoftworks.momoweave.Momoweave;
import com.momosoftworks.momoweave.common.capability.BagInventoryCap;
import com.momosoftworks.momoweave.common.capability.IBagCap;
import com.momosoftworks.momoweave.common.capability.ModCapabilities;
import com.momosoftworks.momoweave.common.level.LostDeathBagsData;
import com.momosoftworks.momoweave.common.level.SavedDataHelper;
import com.momosoftworks.momoweave.config.MainSettingsConfig;
import com.momosoftworks.momoweave.core.init.ItemInit;
import com.momosoftworks.momoweave.event.common.EntityRemovedEvent;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.item.ItemEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.UUID;

@Mod.EventBusSubscriber
public class InventoryDeathBagHandler
{
    @SubscribeEvent
    public static void giveBagCapability(AttachCapabilitiesEvent<ItemStack> event)
    {
        if (event.getObject().getItem() == ItemInit.BAG_OF_THE_PERISHED.get())
        {
            // Make a new capability instance to attach to the item
            IBagCap itemHolderCap = new BagInventoryCap();
            // Optional that holds the capability instance
            final LazyOptional<IBagCap> capOptional = LazyOptional.of(() -> itemHolderCap);
            Capability<IBagCap> capability = ModCapabilities.DEATH_POUCH_ITEMS;

            ICapabilityProvider provider = new ICapabilitySerializable<CompoundTag>()
            {
                @Nonnull
                @Override
                public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, @Nullable Direction direction)
                {
                    // If the requested cap is the insulation cap, return the insulation cap
                    if (cap == capability)
                    {   return capOptional.cast();
                    }
                    return LazyOptional.empty();
                }

                @Override
                public CompoundTag serializeNBT()
                {   return itemHolderCap.serializeNBT();
                }

                @Override
                public void deserializeNBT(CompoundTag nbt)
                {   itemHolderCap.deserializeNBT(nbt);
                }
            };

            // Attach the capability to the item
            event.addCapability(new ResourceLocation(Momoweave.MOD_ID, "item_holding"), provider);
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerDrops(LivingDropsEvent event)
    {
        if (event.getEntity() instanceof Player player)
        {
            // Fill bag with dropped items
            ItemStack bag = new ItemStack(ItemInit.BAG_OF_THE_PERISHED.get());
            bag.getCapability(ModCapabilities.DEATH_POUCH_ITEMS).ifPresent(cap ->
            {
                event.getDrops().removeIf(drop ->
                {
                    ItemStack dropStack = drop.getItem();
                    if (!dropStack.isEmpty() && !dropStack.is(ItemInit.BAG_OF_THE_PERISHED.get()))
                    {   cap.addItem(dropStack.copy());
                        return true;
                    }
                    return false;
                });
            });
            // Spawn bag item
            bag.getOrCreateTag().putUUID("Owner", player.getUUID());
            bag.getOrCreateTag().putUUID("ID", UUID.randomUUID());
            bag.getOrCreateTag().putLong("DeathTime", player.level().getGameTime());
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
    public static void updateForcedChunksIfBagMoves(EntityEvent.EnteringSection event)
    {
        if (event.getEntity() instanceof ItemEntity itemEntity
        && itemEntity.getItem().is(ItemInit.BAG_OF_THE_PERISHED.get()))
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
    public static void forceChunksOnBagDropped(EntityLeaveLevelEvent event)
    {   setChunkForcedForItem(event, false);
    }

    private static void setChunkForcedForItem(EntityEvent event, boolean forced)
    {
        Entity entity = event.getEntity();
        if (entity instanceof ItemEntity itemEntity)
        {
            if (itemEntity.getItem().is(ItemInit.BAG_OF_THE_PERISHED.get()))
            {
                ChunkPos chunkPos = new ChunkPos(itemEntity.blockPosition());
                entity.level().getChunkSource().updateChunkForced(chunkPos, forced);
            }
        }
    }

    @SubscribeEvent
    public static void onBagDestroyed(EntityRemovedEvent event)
    {
        if (event.getEntity() instanceof ItemEntity itemEntity)
        {
            ItemStack stack = itemEntity.getItem();
            ServerLevel overworld = event.getEntity().getServer().overworld();
            if (stack.is(ItemInit.BAG_OF_THE_PERISHED.get()))
            {
                if (!stack.getOrCreateTag().hasUUID("Owner"))
                {   Momoweave.LOGGER.warn("Death bag without owner UUID despawned/destroyed!");
                    return;
                }
                UUID uuid = stack.getOrCreateTag().getUUID("Owner");
                LostDeathBagsData lostBagData = SavedDataHelper.getLostDeathBags(overworld);
                lostBagData.addLostBag(uuid, stack.copy());
                lostBagData.scheduleWanderingTrader(uuid);
            }
        }
    }

    @SubscribeEvent
    public static void onTradeWithWanderingTrader(TradeWithVillagerEvent event)
    {
        ItemStack result = event.getMerchantOffer().getResult();
        if (event.getAbstractVillager() instanceof WanderingTrader trader && result.is(ItemInit.BAG_OF_THE_PERISHED.get()))
        {
            UUID playerId = event.getEntity().getUUID();
            SavedDataHelper.getLostDeathBags((ServerLevel) trader.level()).removeLostBag(playerId, result);
            trader.getPersistentData().getCompound("DeathBags").remove(playerId.toString());
        }
    }

    @SubscribeEvent
    public static void tickLevelBagData(TickEvent.LevelTickEvent event)
    {
        if (event.level instanceof ServerLevel serverLevel && serverLevel.dimension() == Level.OVERWORLD)
        {   SavedDataHelper.getLostDeathBags(serverLevel).tick();
        }
    }
}
