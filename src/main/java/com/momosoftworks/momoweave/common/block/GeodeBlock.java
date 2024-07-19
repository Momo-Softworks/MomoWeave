package com.momosoftworks.momoweave.common.block;

import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.momoweave.common.blockentity.GeodeBlockEntity;
import com.momosoftworks.momoweave.core.init.BlockInit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

public class GeodeBlock extends Block implements EntityBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final VoxelShape SHAPE = Block.box(5, 0, 6, 14, 5, 14);
    private static final Map<Direction, VoxelShape> SHAPES_PER_DIRECTION = new EnumMap<>(Direction.class);

    public GeodeBlock(Properties properties)
    {   super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
        calculateFacingShapes(SHAPE);
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter level, BlockPos pos, Player player)
    {
        if (level.getBlockEntity(pos) instanceof GeodeBlockEntity geode)
        {   return geode.getOre().getCloneItemStack(state, target, level, pos, player);
        }
        return super.getCloneItemStack(state, target, level, pos, player);
    }

    static void calculateFacingShapes(VoxelShape shape)
    {
        for (Direction direction : Direction.values())
        {   SHAPES_PER_DIRECTION.put(direction, CSMath.rotateShape(direction, shape));
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext selection)
    {   return SHAPES_PER_DIRECTION.get(state.getValue(FACING));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {   return new GeodeBlockEntity(pos, state);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation direction)
    {   return state.setValue(FACING, direction.rotate(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {   builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context)
    {   return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
}
