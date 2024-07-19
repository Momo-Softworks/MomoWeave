package com.momosoftworks.momoweave.common.level;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;

public class EndInitializationStatus extends SavedData
{
    private boolean initialized = false;

    public EndInitializationStatus()
    {}

    public boolean isInitialized()
    {   return initialized;
    }

    public void setInitialized(boolean initialized)
    {   this.initialized = initialized;
        this.setDirty();
    }

    public static EndInitializationStatus load(CompoundTag nbt)
    {
        EndInitializationStatus data = new EndInitializationStatus();
        data.initialized = nbt.getBoolean("Initialized");
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag nbt)
    {
        nbt.putBoolean("Initialized", initialized);
        return nbt;
    }
}
