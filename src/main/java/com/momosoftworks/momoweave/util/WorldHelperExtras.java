package com.momosoftworks.momoweave.util;

import com.mojang.datafixers.util.Pair;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import net.minecraft.world.level.biome.Biome;
import oshi.util.tuples.Triplet;

public class WorldHelperExtras
{
    public static Pair<Double, Double> getBiomeTemperature(Biome biome)
    {
        double variance = 1 / Math.max(1, 2 + biome.getModifiedClimateSettings().downfall() * 2);
        double baseTemp = biome.getBaseTemperature();

        // Get the biome's temperature, either overridden by config or calculated
        // Start with biome override
        Triplet<Double, Double, Temperature.Units> configTemp   = ConfigSettings.BIOME_TEMPS.get(RegistryHelper.getRegistryAccess())
                                                                  .getOrDefault(biome, new Triplet<>(baseTemp - variance, baseTemp + variance, Temperature.Units.MC));
        Triplet<Double, Double, Temperature.Units> configOffset = ConfigSettings.BIOME_OFFSETS.get(RegistryHelper.getRegistryAccess())
                                                                  .getOrDefault(biome, new Triplet<>(0d, 0d, Temperature.Units.MC));
        return CSMath.addPairs(Pair.of(configTemp.getA(), configTemp.getB()), Pair.of(configOffset.getA(), configOffset.getB()));
    }
}
