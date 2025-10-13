package com.momosoftworks.momoweave.config;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.Tags;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Mod.EventBusSubscriber
public class MainSettingsConfig
{
    private static final ForgeConfigSpec SPEC;
    private static final MainSettingsConfig INSTANCE = new MainSettingsConfig();
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.ConfigValue<List<? extends List<?>>> favoredOres;
    public static final ForgeConfigSpec.DoubleValue favoredOreMultiplier;
    public static final ForgeConfigSpec.ConfigValue<List<? extends Integer>> favoredOresPerBiome;
    public static final ForgeConfigSpec.IntValue maxFavoredOresPerChunk;
    public static final ForgeConfigSpec.DoubleValue unfavoredOreMultiplier;
    public static final ForgeConfigSpec.IntValue averageAnimalBreedingTimeSecs;
    public static final ForgeConfigSpec.IntValue maxAnimalPackSize;

    public static final ForgeConfigSpec.DoubleValue lootRollMultiplier;

    public static final ForgeConfigSpec.IntValue maxTickingMonsterDistance;

    static
    {
        BUILDER.push("Generation");

        favoredOresPerBiome = BUILDER
                .comment("Controls the number of favored ores that will generate in a biome.",
                         "Format: [min, max]",
                         "Example: [1, 3]")
                .defineList("Favored Ores Per Biome", List.of(1, 3),
                            it -> it instanceof Integer);

        favoredOres = BUILDER
                .comment("Controls the conditions under which an ore will be \"favored\" in a biome, depending on the biome's climate.",
                         "Favored ores have increased rates of generation.",
                         "Format: [\"ore name\", [temperature-range-1, temperature-range-1...], [humidity-range-1, humidity-range-2...], [altitude-range-1, altitude-range-2...]]",
                         "Ranges can be set to \"all\" to match any value in that category.",
                         "Example: [\"diamond\", [\"temperate\", \"warm\"], [\"all\"], [\"all\"]]",
                         "temperature-ranges: \"freezing\": -1.0-0.1, \"cool\": 0.1-0.5, \"temperate\": 0.5-1.2, \"warm\": 1.2-1.6, \"hot\": 1.6-2.0",
                         "humidity-ranges: \"dry\": 0-0.4, \"normal\": 0.4-0.8, \"wet\": 0.8-1.0",
                         "altitude-ranges: \"deep\": -64-0, \"low\": 0-32, \"meidum\": 32-84, \"high\": 84-128, \"very_high\": 128-320")
                .defineListAllowEmpty("Favored Ore Conditions", List.of(

                ),
                it -> it instanceof List<?> list
                && list.size() == 4
                && list.get(0) instanceof String
                && list.get(1) instanceof List<?>
                && list.get(2) instanceof List<?>
                && list.get(3) instanceof List<?>);

        favoredOreMultiplier = BUILDER
                .comment("Controls the multiplier for ores that are favored in a biome.",
                         "This value is multiplied by the default generation rate of the ore.")
                .defineInRange("Favored Ore Multiplier", 4.0, 0.0, Double.POSITIVE_INFINITY);

        maxFavoredOresPerChunk = BUILDER
                .comment("Controls the maximum number of favored ores that can generate in a single chunk.")
                .defineInRange("Max Favored Ores Per Chunk", 8, 0, Integer.MAX_VALUE);

        unfavoredOreMultiplier = BUILDER
                .comment("Controls the multiplier for ores that are not favored in a biome.",
                         "This value is multiplied by the default generation rate of the ore.")
                .defineInRange("Unfavored Ore Multiplier", 0.5, 0.0, Double.POSITIVE_INFINITY);

        BUILDER.pop();

        BUILDER.push("Animals");

        averageAnimalBreedingTimeSecs = BUILDER
                .comment("Controls the average time in seconds that it takes for animals to breed.",
                         "This is solely used for determining how often animals will \"breed\" in unloaded chunks.")
                .defineInRange("Average Animal Breeding Time (seconds)", 3600, 0, Integer.MAX_VALUE);

        maxAnimalPackSize = BUILDER
                .comment("Controls the maximum number of any one type of animal allowed within a 32x32 block area.",
                         "This is used to prevent overpopulation of animals in a small area.",
                         "If the number of animals of any one type exceeds this value, that type of animal will not breed.")
                .defineInRange("Max Animal Litter Size", 6, 0, Integer.MAX_VALUE);

        BUILDER.pop();

        BUILDER.push("Optimization");

        maxTickingMonsterDistance = BUILDER
                .comment("Controls the maximum distance that a monster can be from a player before it stops ticking.",
                         "This is used to prevent monsters from ticking when they are far away from the player.")
                .defineInRange("Max Ticking Monster Distance", 48, 0, Integer.MAX_VALUE);

        BUILDER.pop();

        BUILDER.push("Loot");

        lootRollMultiplier = BUILDER
                .comment("Controls the multiplier for the number of items that will be rolled in a loot table.",
                         "This value is multiplied by the default number of items rolled in the loot table.")
                .defineInRange("Loot Roll Multiplier", 0.5, 0.0, Double.POSITIVE_INFINITY);

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    // Generate default configs if none are present
    @SubscribeEvent
    public static void initDefaultConfigs(ServerStartedEvent event)
    {
        if (favoredOres.get().isEmpty())
        {
            Level overworld = event.getServer().getLevel(Level.OVERWORLD);
            if (overworld == null) return;
            Registry<PlacedFeature> configuredFeatureRegistry = event.getServer().registryAccess().registryOrThrow(Registries.PLACED_FEATURE);
            List<? extends List<?>> defaultOres =
                configuredFeatureRegistry.holders()
                .filter(feature ->
                {
                    PlacedFeature placement = feature.value();
                    ConfiguredFeature<?, ?> config = placement.feature().value();
                    RandomSource random = RandomSource.create();
                    return config.config() instanceof OreConfiguration oreConfig
                        && oreConfig.targetStates.stream().anyMatch(target -> target.state.is(Tags.Blocks.ORES))
                        && oreConfig.targetStates.stream().anyMatch(target -> target.target.test(Blocks.STONE.defaultBlockState(), random) || target.target.test(Blocks.DEEPSLATE.defaultBlockState(), random));
                })
                .flatMap(feature -> ((OreConfiguration) feature.value().feature().value().config()).targetStates.stream().map(target -> target.state))
                .map(state -> {
                    String oreName = ConfigSettings.stripOreName(ForgeRegistries.BLOCKS.getKey(state.getBlock()).getPath());
                    return List.of(oreName, List.of("all"), List.of("all"), List.of("all"));
                })
                .distinct()
                .toList();
            favoredOres.set(defaultOres);
            SPEC.save();
        }
    }

    public static void setup()
    {
        Path configPath = FMLPaths.CONFIGDIR.get();
        Path csConfigPath = Paths.get(configPath.toAbsolutePath().toString(), "momoweave");

        // Create the config folder
        try
        {   Files.createDirectory(csConfigPath);
        }
        catch (Exception ignored) {}

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "momoweave/main.toml");
    }
}
