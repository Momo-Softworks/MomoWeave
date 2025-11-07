package com.momosoftworks.momoweave.common.blockentity;

import com.momosoftworks.coldsweat.util.world.WorldHelper;
import com.momosoftworks.momoweave.config.MainSettingsConfig;
import com.momosoftworks.momoweave.core.init.BlockEntityInit;
import com.momosoftworks.momoweave.core.init.BlockInit;
import com.momosoftworks.momoweave.event.common.EntityRemovedEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import javax.annotation.Nullable;
import java.util.UUID;

@Mod.EventBusSubscriber
public class TradingPostBlockEntity extends BlockEntity
{
    private int ticksExisted = 0;
    private boolean hasBeenNight = false;
    private UUID traderUUID = null; // Store UUID instead of direct reference

    public TradingPostBlockEntity(BlockPos pos, BlockState state)
    {   super(BlockEntityInit.TRADING_POST.get(), pos, state);
    }

    public static <T extends BlockEntity> void tick(Level level, BlockPos pos, BlockState state, T te)
    {
        if (te instanceof TradingPostBlockEntity tradingPostTE)
        {   tradingPostTE.tick(level, state, pos);
        }
    }

    public void tick(Level level, BlockState state, BlockPos pos)
    {
        if (!level.isClientSide && this.ticksExisted % 20 == 0)
        {
            // Check if our tracked trader still exists
            WanderingTrader trader = this.getTrader(level);
            if (trader != null && trader.isRemoved())
            {
                this.traderUUID = null;
                trader = null;
            }

            if (!level.isDay())
            {   this.hasBeenNight = true;
            }

            // Spawn trader at dawn if we don't have one
            if (this.hasBeenNight && level.isDay() && trader == null
            && MainSettingsConfig.enableTradingPost.get())
            {
                // Double-check: is there already a trader linked to this post that we just don't know about?
                // (This handles the case where the trader was in an unloaded chunk)
                trader = findExistingTraderForPost(level, pos);

                if (trader != null)
                {
                    // Found an existing trader, just update our reference
                    this.traderUUID = trader.getUUID();
                }
                else
                {
                    // No existing trader found, spawn a new one
                    BlockPos spawnPos = findSafeSpawnPos(level, pos);
                    if (spawnPos == null)
                    {   spawnPos = pos.above();
                    }

                    trader = (WanderingTrader) EntityType.WANDERING_TRADER.create(level);
                    if (trader == null) return;

                    trader.setPos(spawnPos.getCenter().add(0, -0.5, 0));
                    trader.getPersistentData().putLong("TradingPostPos", pos.asLong());

                    this.traderUUID = trader.getUUID();
                    level.addFreshEntity(trader);
                    spawnPoofParticles(level, trader);
                }
            }
            // Remove trader at night
            else if (!level.isDay() && trader != null)
            {
                spawnPoofParticles(level, trader);
                trader.remove(Entity.RemovalReason.DISCARDED);
                this.traderUUID = null;
            }
        }
        this.ticksExisted++;
    }

    /**
     * Get the trader associated with this post, if it exists and is loaded
     */
    @Nullable
    private WanderingTrader getTrader(Level level)
    {
        if (this.traderUUID == null || !(level instanceof ServerLevel serverLevel))
        {   return null;
        }

        Entity entity = serverLevel.getEntity(this.traderUUID);
        if (entity instanceof WanderingTrader trader && !trader.isRemoved())
        {   return trader;
        }

        return null;
    }

    /**
     * Search for a trader that's already linked to this post position
     * This is critical for handling chunk loading/unloading scenarios
     */
    @Nullable
    private static WanderingTrader findExistingTraderForPost(Level level, BlockPos postPos)
    {
        if (!(level instanceof ServerLevel serverLevel))
        {   return null;
        }

        // Scan all loaded entities for a wandering trader linked to this post
        for (Entity entity : serverLevel.getAllEntities())
        {
            if (entity instanceof WanderingTrader trader
                && trader.getPersistentData().contains("TradingPostPos"))
            {
                BlockPos traderPostPos = BlockPos.of(trader.getPersistentData().getLong("TradingPostPos"));
                if (traderPostPos.equals(postPos) && !trader.isRemoved())
                {
                    return trader;
                }
            }
        }
        return null;
    }

    private static void spawnPoofParticles(Level level, LivingEntity entity)
    {
        WorldHelper.spawnParticleBatch(level, ParticleTypes.POOF, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(),
                                       entity.getBbWidth() / 2, entity.getBbHeight() / 2, entity.getBbWidth() / 2, 10, 0.01);
    }

    @Nullable
    private static BlockPos findSafeSpawnPos(Level level, BlockPos origin)
    {
        for (int dx = -1; dx <= 1; dx++)
        {
            for (int dz = -1; dz <= 1; dz++)
            {
                // Skip corner blocks where all offsets are non-zero
                if (dz != 0 && dx != 0) continue;
                BlockPos checkPos = origin.offset(dx, 0, dz);
                BlockState stateAtPos = level.getBlockState(checkPos.below());
                BlockState stateAbove = level.getBlockState(checkPos);
                BlockState stateAbove2 = level.getBlockState(checkPos.above());
                if (stateAtPos.isSolidRender(level, checkPos)
                && stateAbove.isAir()
                && stateAbove2.isAir())
                {
                    return checkPos;
                }
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void ensureTraderDistance(LivingEvent.LivingTickEvent event)
    {
        if (event.getEntity() instanceof WanderingTrader trader && trader.level() instanceof ServerLevel level)
        {
            if (!trader.getPersistentData().contains("TradingPostPos")) return;
            BlockPos postPos = BlockPos.of(trader.getPersistentData().getLong("TradingPostPos"));

            // Verify the post still exists
            BlockState stateAtPost = level.getBlockState(postPos);
            if (!stateAtPost.is(BlockInit.TRADING_POST.get()))
            {
                // Post was removed, despawn trader
                trader.remove(Entity.RemovalReason.DISCARDED);
                return;
            }

            // Teleport trader to post if very far away or in wrong dimension
            if (postPos.distSqr(trader.blockPosition()) > 1000)
            {
                BlockPos teleportPos = findSafeSpawnPos(level, postPos);
                if (teleportPos == null)
                {   teleportPos = postPos.above();
                }
                trader.setPos(teleportPos.getCenter().add(0, -0.5, 0));
            }
            else if (postPos.distSqr(trader.blockPosition()) > 64)
            {
                // Pathfind back to post
                Path returnPath = trader.getNavigation().createPath(postPos, 4);
                trader.getNavigation().moveTo(returnPath, 0.3);
            }
        }
    }

    @SubscribeEvent
    public static void removeTraderOnDespawn(EntityRemovedEvent event)
    {
        if (event.getEntity() instanceof WanderingTrader trader)
        {
            if (!trader.getPersistentData().contains("TradingPostPos")) return;
            BlockPos postPos = BlockPos.of(trader.getPersistentData().getLong("TradingPostPos"));
            Level level = trader.level();
            BlockState stateAtPost = level.getBlockState(postPos);

            if (stateAtPost.is(BlockInit.TRADING_POST.get()))
            {
                BlockEntity blockEntity = level.getBlockEntity(postPos);
                if (blockEntity instanceof TradingPostBlockEntity tradingPostTE)
                {
                    if (tradingPostTE.traderUUID != null && tradingPostTE.traderUUID.equals(trader.getUUID()))
                    {   tradingPostTE.traderUUID = null;
                    }
                }
            }
        }
    }

    @Override
    public void setRemoved()
    {
        super.setRemoved();
        WanderingTrader trader = this.getTrader(this.level);
        if (trader != null && !trader.isRemoved())
        {
            spawnPoofParticles(this.level, trader);
            trader.remove(Entity.RemovalReason.DISCARDED);
            this.traderUUID = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.putBoolean("HasBeenNight", this.hasBeenNight);
        if (this.traderUUID != null)
        {   tag.putUUID("TraderUUID", this.traderUUID);
        }
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        this.hasBeenNight = tag.getBoolean("HasBeenNight");
        if (tag.hasUUID("TraderUUID"))
        {
            this.traderUUID = tag.getUUID("TraderUUID");
            // Don't try to resolve the entity here - it will be resolved on next tick if loaded
        }
    }
}