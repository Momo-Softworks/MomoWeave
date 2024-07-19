package com.momosoftworks.momoweave.common.level;

import com.google.common.collect.ImmutableList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.List;

public class EndEntryPoints extends SavedData
{
    private final List<BlockPos> entryPoints = new ArrayList<>();

    public EndEntryPoints()
    {}

    public List<BlockPos> getEntryPoints()
    {   return ImmutableList.copyOf(entryPoints);
    }

    public void addEndEntryPoint(BlockPos pos)
    {   entryPoints.add(pos);
        this.setDirty();
    }

    public void removeEndEntryPoint(BlockPos pos)
    {   entryPoints.remove(pos);
        this.setDirty();
    }

    public static EndEntryPoints load(CompoundTag nbt)
    {
        EndEntryPoints data = new EndEntryPoints();
        long[] platforms = nbt.getLongArray("EntryPoints");
        for (long l : platforms)
        {   data.entryPoints.add(BlockPos.of(l));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt)
    {
        nbt.putLongArray("EntryPoints", entryPoints.stream().map(BlockPos::asLong).toList());
        return nbt;
    }
}
