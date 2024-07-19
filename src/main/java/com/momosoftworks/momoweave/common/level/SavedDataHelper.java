package com.momosoftworks.momoweave.common.level;

import net.minecraft.server.level.ServerLevel;

public class SavedDataHelper
{
    public static EndEntryPoints getEndEntryPoints(ServerLevel level)
    {   return level.getDataStorage().computeIfAbsent(EndEntryPoints::load, EndEntryPoints::new, "momo:end_entry_points");
    }

    public static EndInitializationStatus getInitializationStatus(ServerLevel level)
    {   return level.getDataStorage().computeIfAbsent(EndInitializationStatus::load, EndInitializationStatus::new, "momo:end_initialization_status");
    }
}
