package com.momosoftworks.momoweave.common.blockentity;

import com.momosoftworks.coldsweat.util.world.WorldHelper;
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
    private LivingEntity trader = null;

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
            if (this.trader != null
            && (this.trader.getRemovalReason() == Entity.RemovalReason.KILLED || this.trader.getRemovalReason() == Entity.RemovalReason.DISCARDED))
            {   this.trader = null;
            }
            if (!level.isDay())
            {   this.hasBeenNight = true;
            }
            if (this.hasBeenNight && level.isDay() && this.trader == null)
            {
                BlockPos spawnPos = findSafeSpawnPos(level, pos);
                if (spawnPos == null)
                {   spawnPos = pos.above();
                }

                LivingEntity trader = EntityType.WANDERING_TRADER.create(level);
                if (trader == null) return;

                trader.setPos(spawnPos.getCenter().add(0, -0.5, 0));
                trader.getPersistentData().putLong("TradingPostPos", pos.asLong());

                this.trader = trader;
                level.addFreshEntity(trader);
                spawnPoofParticles(level, trader);
            }
            else if (!level.isDay() && this.trader != null)
            {
                spawnPoofParticles(level, this.trader);
                this.trader.remove(Entity.RemovalReason.DISCARDED);
                this.trader = null;
            }
        }
        this.ticksExisted++;
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

            // Teleport trader to post if very far away
            if (postPos.distSqr(trader.blockPosition()) > 1000 || trader.level().dimension() != level.dimension())
            {
                if (!level.getBlockState(postPos.above()).isAir()
                && !level.getBlockState(postPos.above(2)).isAir())
                {
                    trader.changeDimension(level);
                    trader.setPos(postPos.above().getCenter());
                }
            }
            else if (postPos.distSqr(trader.blockPosition()) > 64)
            {
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
                {   tradingPostTE.trader = null;
                }
            }
        }
    }

    @Override
    public void setRemoved()
    {
        super.setRemoved();
        if (this.trader != null && !this.trader.isRemoved())
        {
            spawnPoofParticles(this.level, this.trader);
            this.trader.remove(Entity.RemovalReason.DISCARDED);
            this.trader = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag)
    {
        super.saveAdditional(tag);
        tag.putBoolean("HasBeenNight", this.hasBeenNight);
        if (this.trader != null)
        {   tag.putUUID("TraderUUID", this.trader.getUUID());
        }
    }

    @Override
    public void load(CompoundTag tag)
    {
        super.load(tag);
        this.hasBeenNight = tag.getBoolean("HasBeenNight");
        if (tag.hasUUID("TraderUUID"))
        {
            UUID traderUUID = tag.getUUID("TraderUUID");
            if (this.getLevel() instanceof ServerLevel serverLevel)
            {
                serverLevel.getServer().execute(() ->
                {
                    Entity entity = serverLevel.getEntities().get(traderUUID);
                    if (entity instanceof LivingEntity livingEntity)
                    {   this.trader = livingEntity;
                    }
                });
            }
        }
    }
}
