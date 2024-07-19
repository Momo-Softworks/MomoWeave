package com.momosoftworks.momoweave.mixin;

import com.momosoftworks.momoweave.common.level.EndEntryPoints;
import com.momosoftworks.momoweave.common.level.SavedDataHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public class MixinTravelToEnd
{
    @Unique
    Entity self = (Entity)(Object)this;

    @Inject(method = "findDimensionEntryPoint(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/level/portal/PortalInfo;", at = @At("RETURN"), cancellable = true)
    private void changeEndSpawnLocation(ServerLevel newDimension, CallbackInfoReturnable<PortalInfo> cir)
    {
        if (newDimension.dimension() == Level.END && !self.level().isClientSide)
        {
            WorldBorder worldBorder = newDimension.getWorldBorder();
            double coordinateScale = DimensionType.getTeleportationScale(self.level().dimensionType(), newDimension.dimensionType());

            int x = (int) self.getX();
            int z = (int) self.getZ();
            // Use nearest entry point within 32 blocks
            EndEntryPoints endData = SavedDataHelper.getEndEntryPoints(((ServerLevel) self.level()));
            for (BlockPos platform : endData.getEntryPoints())
            {
                if (new Vec2(x, z).distanceToSqr(new Vec2(platform.getX(), platform.getZ())) < 32*32)
                {   x = platform.getX();
                    z = platform.getZ();
                }
            }
            // Ensure spawn position is at least 1000 blocks away from end island
            if (Math.abs(x) < 1000)
            {   x = 1000 * Mth.sign(x);
            }
            if (Math.abs(z) < 1000)
            {   z = 1000 * Mth.sign(z);
            }
            // Create spawn position
            BlockPos spawnPos = worldBorder.clampToBounds(x * coordinateScale, self.getY(), z * coordinateScale);
            int y = getGroundLevel(newDimension, spawnPos);

            // Create platform if no solid ground
            if (y == 0)
            {
                y = 65;
                int platformY = 64;
                // Spawn end platform here
                BlockPos.betweenClosed(x - 2, platformY, z - 2, x + 2, platformY, z + 2).forEach((position) ->
                {   newDimension.setBlockAndUpdate(position, Blocks.OBSIDIAN.defaultBlockState());
                });
                BlockPos.betweenClosed(x - 2, platformY + 1, z - 2, x + 2, platformY + 3, z + 2).forEach((position) ->
                {   newDimension.setBlockAndUpdate(position, Blocks.AIR.defaultBlockState());
                });
                endData.addEndEntryPoint(new BlockPos(x, platformY, z));
            }
            cir.setReturnValue(new PortalInfo(new Vec3(x + 0.5D, y + 2, z + 0.5D),
                                              self.getDeltaMovement(), self.getYRot(), self.getXRot()));
        }
    }

    @Mixin(ServerPlayer.class)
    public static class DisablePlatformPlayer
    {
        @Inject(method = "createEndPlatform", at = @At("HEAD"), cancellable = true)
        private void disableObsidianPlatform(ServerLevel level, BlockPos pos, CallbackInfo ci)
        {   ci.cancel();
        }
    }

    @Unique
    private static int getGroundLevel(Level level, BlockPos pos)
    {
        // check if there's a solid block at any y-level at this x and z
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();
        for (int y = level.getMaxBuildHeight(); y > level.getMinBuildHeight(); y--)
        {
            checkPos.set(pos.getX(), y, pos.getZ());
            BlockState state = level.getBlockState(checkPos);
            if (!state.getCollisionShape(level, checkPos).isEmpty())
            {   return y;
            }
        }
        return 0;
    }
}
