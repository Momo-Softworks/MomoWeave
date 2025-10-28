package com.momosoftworks.momoweave.common.level.feature;

import com.mojang.serialization.Codec;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.momoweave.common.block.GeodeBlock;
import com.momosoftworks.momoweave.common.blockentity.GeodeBlockEntity;
import com.momosoftworks.momoweave.config.ConfigSettings;
import com.momosoftworks.momoweave.core.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class GeodesFeature extends Feature<NoneFeatureConfiguration>
{
    public GeodesFeature(Codec<NoneFeatureConfiguration> codec)
    {   super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context)
    {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();

        BlockPos.MutableBlockPos movingPos = origin.mutable();
        for (int x = -2; x < 2; x++)
        {
            for (int z = -2; z < 2; z++)
            {
                for (int y = -2; y < 2; y++)
                {
                    movingPos.set(origin.getX() + x, origin.getY() + y, origin.getZ() + z);
                    BlockState blockStateAt = level.getBlockState(movingPos);
                    if (blockStateAt.canBeReplaced() && blockStateAt.getFluidState().isEmpty())
                    {
                        BlockState belowState = level.getBlockState(movingPos.below());
                        if (belowState.is(BlockTags.OVERWORLD_CARVER_REPLACEABLES)
                        && belowState.isFaceSturdy(level, movingPos.below(), Direction.UP))
                        {
                            double noisyDistance = CSMath.getDistance(origin, movingPos) + RandomSource.create().nextGaussian();
                            if (noisyDistance < 1.5)
                            {   level.setBlock(movingPos.below(), Blocks.STONE.defaultBlockState(), 2);
                            }
                            else if (noisyDistance < 2.5)
                            {   level.setBlock(movingPos.below(), Blocks.COBBLESTONE.defaultBlockState(), 2);
                            }
                            if (noisyDistance < 3.5 && blockStateAt.is(Blocks.SNOW))
                            {   level.setBlock(movingPos, Blocks.AIR.defaultBlockState(), 2);
                            }
                        }
                    }
                }
            }
        }
        int geodeTries = 0;
        int geodesPlaced = 0;
        while(geodesPlaced < 6)
        {
            if (geodeTries > (geodesPlaced < 3 ? 80 : 20))
            {   return geodesPlaced > 0;
            }
            movingPos.set(origin.getX() + RandomSource.create().nextIntBetweenInclusive(-2, 1),
                          origin.getY() + RandomSource.create().nextIntBetweenInclusive(-1, 1),
                          origin.getZ() + RandomSource.create().nextIntBetweenInclusive(-2, 1));
            BlockState blockStateAt = level.getBlockState(movingPos);
            if (blockStateAt.canBeReplaced() && blockStateAt.getFluidState().isEmpty())
            {
                BlockState belowState = level.getBlockState(movingPos.below());
                if (belowState.is(Blocks.COBBLESTONE) || belowState.is(Blocks.STONE))
                {
                    level.setBlock(movingPos, BlockInit.GEODE.get().defaultBlockState().setValue(GeodeBlock.FACING, Direction.from2DDataValue(level.getRandom().nextIntBetweenInclusive(0, 4))), 2);
                    if (level.getBlockEntity(movingPos) instanceof GeodeBlockEntity geode)
                    {   geode.setOre(ConfigSettings.FAVORED_ORE_BLOCKS_PER_BIOME.get(level.getBiome(movingPos)).stream().toList().get(0));
                    }
                    geodesPlaced++;
                }
            }
            geodeTries++;
        }
        return true;
    }

    private static BlockState getBlockForDistance(double distance)
    {
        double noisyDistance = distance + RandomSource.create().nextGaussian();
        return noisyDistance < 1.5
                        ? Blocks.STONE.defaultBlockState()
             : noisyDistance < 2.5
                        ? Blocks.COBBLESTONE.defaultBlockState()
             : Blocks.AIR.defaultBlockState();
    }
}
