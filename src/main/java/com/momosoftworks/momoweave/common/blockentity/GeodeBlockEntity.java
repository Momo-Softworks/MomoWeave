package com.momosoftworks.momoweave.common.blockentity;

import com.momosoftworks.momoweave.core.init.BlockEntityInit;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public class GeodeBlockEntity extends BlockEntity
{
    Block oreBlock = null;

    public GeodeBlockEntity(BlockPos pos, BlockState state)
    {
        super(BlockEntityInit.GEODE.get(), pos, state);
        /*SchedulerHelper.scheduleUntilLoaded(() ->
        {
            if (this.oreBlock == null)
            {
                BlockPos surfacePos = new BlockPos(this.worldPosition.getX(), WorldHelper.getHeight(this.worldPosition, this.level), this.worldPosition.getZ());
                Holder<Biome> biome = this.level.getBiome(surfacePos);
                Collection<Block> oresForBiome = ConfigSettings.FAVORED_ORE_BLOCKS_PER_BIOME.get(biome);
                if (oresForBiome.isEmpty())
                {   this.oreBlock = Blocks.STONE;
                }
                else
                {   List<Block> ores = oresForBiome.stream().toList();
                    this.oreBlock = ores.get(new Random().nextInt(ores.size()));
                }
            }
        }, 5, () -> this.getLevel(), () -> this.getBlockPos());*/
    }

    public Block getOre()
    {   return oreBlock != null ? oreBlock : Blocks.STONE;
    }

    public void setOre(Block oreBlock)
    {   this.oreBlock = oreBlock;
    }

    @Override
    public void saveAdditional(CompoundTag pTag)
    {
        super.saveAdditional(pTag);
        pTag.putString("Ore", ForgeRegistries.BLOCKS.getKey(this.oreBlock).toString());
    }

    @Override
    public void load(CompoundTag pTag)
    {
        super.load(pTag);
        this.oreBlock = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(pTag.getString("Ore")));
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        CompoundTag tag = super.getUpdateTag();
        tag.putString("Ore", ForgeRegistries.BLOCKS.getKey(this.oreBlock).toString());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag)
    {
        super.handleUpdateTag(tag);
        this.oreBlock = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(tag.getString("Ore")));
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {   return ClientboundBlockEntityDataPacket.create(this);
    }
}
