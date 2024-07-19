package com.momosoftworks.momoweave.core.init;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.momoweave.Momoweave;
import com.momosoftworks.momoweave.data.biome_modifier.ExtraOresBiomeModifier;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BiomeCodecInit
{
    public static DeferredRegister<Codec<? extends BiomeModifier>> BIOME_MODIFIER_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, Momoweave.MOD_ID);

    public static RegistryObject<Codec<ExtraOresBiomeModifier>> EXTRA_ORES_CODEC = BIOME_MODIFIER_SERIALIZERS.register("extra_ores", () ->
            RecordCodecBuilder.create(builder -> builder.group(
                    Codec.BOOL.fieldOf("use_configs").forGetter(ExtraOresBiomeModifier::useConfigs)
            ).apply(builder, ExtraOresBiomeModifier::new)));
}
