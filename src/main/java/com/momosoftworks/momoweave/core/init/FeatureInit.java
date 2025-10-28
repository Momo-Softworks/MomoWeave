package com.momosoftworks.momoweave.core.init;

import com.momosoftworks.momoweave.Momoweave;
import com.momosoftworks.momoweave.common.level.feature.GeodesFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class FeatureInit
{
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, Momoweave.MOD_ID);

    public static final RegistryObject<Feature<NoneFeatureConfiguration>> SURFACE_ORES = FEATURES.register("surface_ores", () -> new GeodesFeature(NoneFeatureConfiguration.CODEC));
}
