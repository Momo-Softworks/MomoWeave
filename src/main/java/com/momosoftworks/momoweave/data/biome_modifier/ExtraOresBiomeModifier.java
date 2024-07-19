package com.momosoftworks.momoweave.data.biome_modifier;

import com.mojang.serialization.Codec;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import com.momosoftworks.momoweave.Momoweave;
import com.momosoftworks.momoweave.config.ConfigSettings;
import com.momosoftworks.momoweave.core.init.BiomeCodecInit;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.valueproviders.ConstantInt;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.OreFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.UniformHeight;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

public record ExtraOresBiomeModifier(boolean useConfigs) implements BiomeModifier
{
    public static final Map<ResourceLocation, Double> ORE_SPAWN_PROBABILITIES = new HashMap<>();
    //5813168463475788799 -246978.00 105.46 989342.10

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder)
    {
        if (ConfigSettings.CONFIGURED_FAVORED_ORES.isEmpty())
        {   ConfigSettings.compileOres(WorldHelper.getServer());
        }

        ResourceLocation biomeId = RegistryHelper.getBiomeId(biome.value(), RegistryHelper.getRegistryAccess());
        long worldSeed = WorldHelper.getServer().getWorldData().worldGenOptions().seed();
        long randomSeed = worldSeed * new BigInteger(biomeId.toString().getBytes()).intValue();
        Random random = new Random(randomSeed);

        List<Holder<PlacedFeature>> biomeFeaturesReadonly = builder.getGenerationSettings().getFeatures(GenerationStep.Decoration.UNDERGROUND_ORES);

        if (phase == Phase.ADD && biome.is(BiomeTags.IS_OVERWORLD) && !biome.is(Tags.Biomes.IS_UNDERGROUND))
        {
            Registry<PlacedFeature> featureRegistry = RegistryHelper.getRegistry(Registries.PLACED_FEATURE);

            PlacedFeature geodeFeature = featureRegistry.get(new ResourceLocation(Momoweave.MOD_ID, "geode"));
            builder.getGenerationSettings().addFeature(GenerationStep.Decoration.VEGETAL_DECORATION, Holder.direct(geodeFeature));

            // If the biome doesn't match any ore configs, give it random ones
            if (ConfigSettings.getQualifyingOresForBiome(biome).isEmpty())
            {
                int oresToAdd = (int) CSMath.blend(ConfigSettings.FAVORED_ORE_COUNT_PER_BIOME.get().get(0),
                                                   ConfigSettings.FAVORED_ORE_COUNT_PER_BIOME.get().get(1),
                                                   random.nextDouble(), 0, 1);
                List<ResourceLocation> oreCandidates = ConfigSettings.CONFIGURED_FAVORED_ORES.keySet().stream().filter(entry -> biomeHasFeature(biomeFeaturesReadonly, entry, RegistryHelper.getRegistryAccess())).toList();
                for (int i = 0; i < oresToAdd; i++)
                {
                    ResourceLocation randomOre = oreCandidates.get(random.nextInt(oreCandidates.size()));
                    PlacedFeature oreFeature = featureRegistry.get(randomOre);

                    builder.getGenerationSettings().addFeature(GenerationStep.Decoration.UNDERGROUND_ORES, Holder.direct(oreFeature));

                    ConfigSettings.CONFIGURED_FAVORED_ORES.put(randomOre, OreClimateSettings.createAnyRandomHeight(randomOre.getPath()));
                    ConfigSettings.FAVORED_ORE_BLOCKS_PER_BIOME.putAll(biome, getOresForFeature(oreFeature).stream().map(BlockState::getBlock).toList());

                    Momoweave.LOGGER.warn("Forced to add random ore {} to biome {}: No matches found", randomOre, biome.unwrapKey().get().location());
                }
            }
        }

        else if (phase == Phase.REMOVE && biome.is(Tags.Biomes.IS_UNDERGROUND))
        {
            builder.getGenerationSettings().getFeatures(GenerationStep.Decoration.UNDERGROUND_ORES)
            .removeIf(feature -> !getOresForFeature(feature.get()).isEmpty());
        }

        else if (phase == Phase.AFTER_EVERYTHING && biome.is(BiomeTags.IS_OVERWORLD) && !biome.is(Tags.Biomes.IS_UNDERGROUND))
        {
            Registry<PlacedFeature> featureRegistry = RegistryHelper.getRegistry(Registries.PLACED_FEATURE);

            // Get a random feature to increase based on the random seed
            Map<ResourceLocation, OreClimateSettings> availableOres = getAvailableOres(biome, biomeFeaturesReadonly, RegistryHelper.getRegistryAccess());
            if (availableOres.isEmpty())
            {
                Momoweave.LOGGER.error("No favored ore candidates found for {}", biome.unwrapKey().get().location());
                availableOres = selectRandomOres(random, ConfigSettings.FAVORED_ORE_COUNT_PER_BIOME.get().get(0));
                Momoweave.LOGGER.error("Selected ores {}", availableOres.keySet());
            }

            // Adjust spawn probabilities based on previous spawns
            adjustSpawnProbabilities(availableOres);

            // Find all ores that can be favored in this biome and
            // Determine how many favored ores are in this biome
            int minOres = ConfigSettings.FAVORED_ORE_COUNT_PER_BIOME.get().get(0);
            int maxOres = ConfigSettings.FAVORED_ORE_COUNT_PER_BIOME.get().get(1);
            int favoredOresInBiome = random.nextInt(minOres, maxOres + 1);
            Map<ResourceLocation, OreClimateSettings> favoredOreTypes = selectFavoredOres(availableOres, random, favoredOresInBiome);

            if (favoredOreTypes.isEmpty())
            {   Momoweave.LOGGER.error("Failed to assign favored ores for biome {}", biome.unwrapKey().get().location());
            }

            for (int i = 0; i < biomeFeaturesReadonly.size(); i++)
            {
                Holder<PlacedFeature> placedFeature = biomeFeaturesReadonly.get(i);
                // Decrease the spawn rates of all ores except the one we want to increase
                if (placedFeature != null && !getOresForFeature(placedFeature.value()).isEmpty())
                {
                    ResourceLocation featureId = featureRegistry.getKey(placedFeature.value());
                    OreClimateSettings favoredOreSettings = favoredOreTypes.get(featureId);
                    // Get the placement modifiers for this ore
                    ConfiguredFeature<?, ?> configuredFeature = placedFeature.value().feature().value();
                    List<PlacementModifier> newPlacement = new ArrayList<>(placedFeature.value().placement());

                    // Change the generation settings of the favored ore
                    if (favoredOreSettings != null)
                    {
                        // Make a copy of the favored ore with custom height range
                        HeightRangePlacement heightRange = HeightRangePlacement.of(UniformHeight.of(VerticalAnchor.absolute(favoredOreSettings.minY()),
                                                                                                    VerticalAnchor.absolute(favoredOreSettings.maxY())));
                        newPlacement.removeIf(mod -> mod instanceof HeightRangePlacement);
                        newPlacement.add(heightRange);
                        CountPlacement count = newPlacement.stream().filter(mod -> mod instanceof CountPlacement).map(mod -> (CountPlacement) mod).findFirst().orElse(null);
                        if (count != null)
                        {
                            IntProvider oldCount = getCount(count);
                            int oldCountMin = oldCount.getMinValue();
                            int oldCountMax = oldCount.getMaxValue();
                            double oreMultiplier = ConfigSettings.FAVORED_ORE_MULTIPLIER.get() / 2;
                            // Add the new count modifier
                            IntProvider newCount = UniformInt.of((int) Math.min(oldCountMin * oreMultiplier, 20), (int) Math.min(oldCountMax * oreMultiplier, 20));
                            newPlacement.set(newPlacement.indexOf(count), CountPlacement.of(newCount));
                            // Remove biome filters. We want to ignore cave biome generation
                            newPlacement.removeIf(mod -> mod instanceof BiomeFilter);
                        }
                        List<Block> favoredOreBlocs = getOresForFeature(placedFeature.value()).stream().map(BlockState::getBlock).toList();
                        ConfigSettings.FAVORED_ORE_BLOCKS_PER_BIOME.putAll(biome, favoredOreBlocs);
                    }
                    else
                    {
                        // Make non-favored ores rarer, and reduce the count of favored ores' normal generation to account for the new spread
                        CountPlacement countPlacement = newPlacement.stream().filter(mod -> mod instanceof CountPlacement).map(mod -> (CountPlacement) mod).findFirst().orElse(null);
                        if (countPlacement != null)
                        {
                            IntProvider countRange = getCount(countPlacement);
                            int oldCountMin = countRange.getMinValue();
                            int oldCountMax = countRange.getMaxValue();
                            double oreMultiplier = ConfigSettings.UNFAVORED_ORE_MULTIPLIER.get();
                            // Add the new count modifier
                            newPlacement.set(newPlacement.indexOf(countPlacement), CountPlacement.of(UniformInt.of((int) Math.min(oldCountMin * oreMultiplier, 20), (int) Math.min(oldCountMax * oreMultiplier, 20))));
                        }
                    }

                    // Add the new ore feature
                    PlacedFeature newPlacedFeature = new PlacedFeature(Holder.direct(configuredFeature), newPlacement);
                    builder.getGenerationSettings().getFeatures(GenerationStep.Decoration.UNDERGROUND_ORES).set(i, Holder.direct(newPlacedFeature));
                }
            }
            if (favoredOreTypes.keySet().stream().anyMatch(ore -> featureRegistry.get(ore).feature().value().config() instanceof OreConfiguration config
            && config.targetStates.stream().anyMatch(target -> !ForgeRegistries.BLOCKS.getKey(target.state.getBlock()).getNamespace().equals("minecraft"))))
            {
                Momoweave.LOGGER.warn("Biome {} has modded ores: {}", biome.unwrapKey().get().location(), favoredOreTypes.keySet());
            }
        }
    }

    @Override
    public Codec<? extends BiomeModifier> codec()
    {   return BiomeCodecInit.EXTRA_ORES_CODEC.get();
    }

    private Map<ResourceLocation, OreClimateSettings> getAvailableOres(Holder<Biome> biome, List<Holder<PlacedFeature>> features, RegistryAccess registryAccess)
    {
        Map<ResourceLocation, OreClimateSettings> qualifyingOres = ConfigSettings.getQualifyingOresForBiome(biome);

        Map<ResourceLocation, OreClimateSettings> filtered = qualifyingOres.entrySet()
                .stream()
                .filter(entry -> biomeHasFeature(features, entry.getKey(), registryAccess))
                .collect(HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()), HashMap::putAll);
        return filtered;
    }

    private Map<ResourceLocation, OreClimateSettings> selectRandomOres(Random random, int count)
    {
        List<Map.Entry<ResourceLocation, OreClimateSettings>> allOres = new ArrayList<>(ConfigSettings.CONFIGURED_FAVORED_ORES.entrySet());
        Collections.shuffle(allOres, random);
        return allOres.stream().limit(count).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private void adjustSpawnProbabilities(Map<ResourceLocation, OreClimateSettings> availableOres)
    {
        for (ResourceLocation oreId : availableOres.keySet())
        {
            double currentProbability = ORE_SPAWN_PROBABILITIES.getOrDefault(oreId, 1.0);
            double adjustedProbability = Math.max(currentProbability * 0.9, 0.1); // Reduce probability by 10%, but not below 10%
            ORE_SPAWN_PROBABILITIES.put(oreId, adjustedProbability);
        }
    }

    private Map<ResourceLocation, OreClimateSettings> selectFavoredOres(Map<ResourceLocation, OreClimateSettings> availableOres, Random random, int count)
    {
        Map<ResourceLocation, OreClimateSettings> favoredOres = new HashMap<>();
        List<Map.Entry<ResourceLocation, OreClimateSettings>> oreList = new ArrayList<>(availableOres.entrySet());

        for (int i = 0; i < count && !oreList.isEmpty(); i++)
        {
            double totalProbability = oreList.stream()
                    .mapToDouble(entry -> ORE_SPAWN_PROBABILITIES.getOrDefault(entry.getKey(), 1.0))
                    .sum();
            double randomValue = random.nextDouble() * totalProbability;
            double cumulativeProbability = 0.0;

            for (Map.Entry<ResourceLocation, OreClimateSettings> entry : oreList)
            {
                cumulativeProbability += ORE_SPAWN_PROBABILITIES.getOrDefault(entry.getKey(), 1.0);
                if (randomValue <= cumulativeProbability)
                {
                    favoredOres.put(entry.getKey(), entry.getValue());
                    oreList.remove(entry);
                    break;
                }
            }
        }

        return favoredOres;
    }

    public static boolean biomeHasFeature(List<Holder<PlacedFeature>> features, ResourceLocation featureId, RegistryAccess registryAccess)
    {
        Registry<PlacedFeature> featureRegistry = registryAccess.registryOrThrow(Registries.PLACED_FEATURE);
        return features.stream().map(holder -> featureRegistry.getKey(holder.value())).toList().contains(featureId);
    }

    private static final Field COUNT_FIELD = ObfuscationReflectionHelper.findField(CountPlacement.class, "f_191624_");
    static
    {   COUNT_FIELD.setAccessible(true);
    }

    private static IntProvider getCount(CountPlacement filter)
    {
        try
        {   return (IntProvider) COUNT_FIELD.get(filter);
        }
        catch (IllegalAccessException e)
        {   e.printStackTrace();
            return ConstantInt.of(0);
        }
    }

    public static List<BlockState> getOresForFeature(PlacedFeature feature)
    {
        ConfiguredFeature<?, ?> configuredFeature = feature.feature().value();
        if (configuredFeature.feature() instanceof OreFeature)
        {
            OreConfiguration config = (OreConfiguration) configuredFeature.config();
            return config.targetStates.stream().map(state -> state.state)
                    .filter(state -> state.is(Tags.Blocks.ORES) || state.is(Tags.Blocks.ORES_IN_GROUND_STONE) || state.is(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE)).toList();
        }
        return List.of();
    }

    public static boolean featureContainsOverworldOres(PlacedFeature feature)
    {
        return feature.feature().value().config() instanceof OreConfiguration config
            && config.targetStates.stream().anyMatch(target -> target.state.is(Tags.Blocks.ORES_IN_GROUND_STONE) || target.state.is(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE));
    }
}
