package com.momosoftworks.momoweave.common.event;

import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import com.momosoftworks.momoweave.Momoweave;
import com.momosoftworks.momoweave.common.capability.BagInventoryCap;
import com.momosoftworks.momoweave.common.capability.IBagCap;
import com.momosoftworks.momoweave.common.capability.ModCapabilities;
import com.momosoftworks.momoweave.common.level.LostDeathBagsData;
import com.momosoftworks.momoweave.common.level.SavedDataHelper;
import com.momosoftworks.momoweave.core.init.ItemInit;
import com.momosoftworks.momoweave.event.common.ItemEntityDestroyedEvent;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.entity.item.ItemEvent;
import net.minecraftforge.event.entity.item.ItemExpireEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
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

    @SubscribeEvent
    public static void onBagDespawn(ItemExpireEvent event)
    {   handleDespawnedDeathBag(event);
    }

    @SubscribeEvent
    public static void onBagDestroyed(ItemEntityDestroyedEvent event)
    {   handleDespawnedDeathBag(event);
    }

    @SubscribeEvent
    public static void removeExpiredBags(EntityJoinLevelEvent event)
    {
        if (event.getEntity() instanceof ItemEntity itemEntity)
        {
            ItemStack stack = itemEntity.getItem();
            if (stack.is(ItemInit.BAG_OF_THE_PERISHED.get()))
            {
                ServerLevel overworld = CSMath.getIfNotNull(WorldHelper.getServer(), MinecraftServer::overworld, null);
                LostDeathBagsData bagsData = SavedDataHelper.getLostDeathBags(overworld);
                for (ItemStack bag : bagsData.getExpiredBags())
                {
                    if (ItemStack.isSameItemSameTags(bag, stack))
                    {
                        itemEntity.remove(Entity.RemovalReason.DISCARDED);
                        bagsData.removeExpiredBag(bag);
                        break;
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void registerDroppedBag(EntityJoinLevelEvent event)
    {
        if (event.getEntity() instanceof ItemEntity itemEntity)
        {
            ItemStack stack = itemEntity.getItem();
            if (stack.is(ItemInit.BAG_OF_THE_PERISHED.get()))
            {
                System.out.println("loaded bag");
                ServerLevel overworld = CSMath.getIfNotNull(WorldHelper.getServer(), MinecraftServer::overworld, null);
                LostDeathBagsData bagsData = SavedDataHelper.getLostDeathBags(overworld);
                bagsData.registerBag(stack.copy());
            }
        }
    }

    @SubscribeEvent
    public static void unregisterRemovedBag(EntityLeaveLevelEvent event)
    {
        if (event.getEntity() instanceof ItemEntity itemEntity)
        {
            ItemStack stack = itemEntity.getItem();
            if (stack.is(ItemInit.BAG_OF_THE_PERISHED.get()))
            {
                System.out.println("unloaded bag");
                ServerLevel overworld = CSMath.getIfNotNull(WorldHelper.getServer(), MinecraftServer::overworld, null);
                LostDeathBagsData bagsData = SavedDataHelper.getLostDeathBags(overworld);
                bagsData.unregisterBag(stack);
            }
        }
    }

    @SubscribeEvent
    public static void test(EntityLeaveLevelEvent event)
    {
    }

    @SubscribeEvent
    public static void onTradeWithWanderingTrader(TradeWithVillagerEvent event)
    {
        if (event.getAbstractVillager() instanceof WanderingTrader trader && event.getMerchantOffer().getResult().is(ItemInit.BAG_OF_THE_PERISHED.get()))
        {
            UUID playerId = event.getEntity().getUUID();
            ItemStack bag = event.getMerchantOffer().getResult();
            SavedDataHelper.getLostDeathBags((ServerLevel) trader.level()).removeLostBag(playerId, bag);
        }
    }

    @SubscribeEvent
    public static void tickWanderingTraderSpawns(TickEvent.LevelTickEvent event)
    {
        if (event.level instanceof ServerLevel serverLevel && serverLevel.dimension() == Level.OVERWORLD)
        {   SavedDataHelper.getLostDeathBags(serverLevel).tick();
        }
    }

    private static void handleDespawnedDeathBag(ItemEvent event)
    {
        ItemStack stack = event.getEntity().getItem();
        ServerLevel overworld = event.getEntity().getServer().overworld();
        if (stack.is(ItemInit.BAG_OF_THE_PERISHED.get()))
        {
            UUID uuid = stack.getOrCreateTag().getUUID("Owner");
            LostDeathBagsData lostBagData = SavedDataHelper.getLostDeathBags(overworld);
            lostBagData.addLostBag(uuid, stack.copy());
            lostBagData.scheduleWanderingTrader(uuid);
        }
    }
}
