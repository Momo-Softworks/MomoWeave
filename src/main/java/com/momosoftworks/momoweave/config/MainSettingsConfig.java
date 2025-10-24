package com.momosoftworks.momoweave.config;

import com.momosoftworks.coldsweat.util.world.WorldHelper;
import com.momosoftworks.momoweave.util.SchedulerHelper;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.function.Supplier;

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

    public static final ForgeConfigSpec.IntValue bagExpirationTime;
    public static final ForgeConfigSpec.ConfigValue<? extends List<? extends String>> worthlessTraderItems;
    public static final ForgeConfigSpec.ConfigValue<? extends List<? extends String>> cheapTraderItems;
    public static final ForgeConfigSpec.ConfigValue<? extends List<? extends String>> costlyTraderItems;
    public static final ForgeConfigSpec.ConfigValue<? extends List<? extends String>> extortionateTraderItems;

    public static final ForgeConfigSpec.BooleanValue preventSleep;

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
                .comment("Controls the multiplier for the number of items that will be rolled in dungeon loot tables.",
                         "This value is multiplied by the default number of items rolled in the loot table.")
                .defineInRange("Loot Roll Multiplier", 0.5, 0.0, Double.POSITIVE_INFINITY);

        BUILDER.pop();

        BUILDER.push("Death");
            bagExpirationTime = BUILDER
                .comment("Controls the time in ticks (20 ticks = 1 second) before a Bag of the Perished despawns.",
                         "Set to 0 to disable despawning.")
                .defineInRange("Bag Expiration Time (ticks)", 6000, 0, Integer.MAX_VALUE);
            worthlessTraderItems = BUILDER
                .comment("Items in the lowest tier of wandering trader buy-back items.",
                         "Wandering traders will charge these items if you have near-useless items in your Bag of the Perished")
                .defineList("Worthless Trader Items", List.of("minecraft:copper_ingot", "minecraft:coal"),
                            it -> it instanceof String);
            cheapTraderItems = BUILDER
                .comment("Items in the cheap tier of wandering trader buy-back items.",
                         "Wandering traders will charge these items if you have low-value items in your Bag of the Perished")
                .defineList("Cheap Trader Items", List.of("minecraft:iron_ingot", "minecraft:redstone"),
                            it -> it instanceof String);
            costlyTraderItems = BUILDER
                .comment("Items in the costly tier of wandering trader buy-back items.",
                         "Wandering traders will charge these items if you have mid-value items in your Bag of the Perished")
                .defineList("Costly Trader Items", List.of("minecraft:gold_ingot", "minecraft:lapis_lazuli"),
                            it -> it instanceof String);
            extortionateTraderItems = BUILDER
                .comment("Items in the extortionate tier of wandering trader buy-back items.",
                         "Wandering traders will charge these items if you have high-value items in your Bag of the Perished")
                .defineList("Extortionate Trader Items", List.of("minecraft:diamond", "minecraft:emerald"),
                            it -> it instanceof String);
        BUILDER.pop();

        BUILDER.push("Sleeping");
            preventSleep = BUILDER
                .comment("If true, players will be prevented from sleeping in beds.")
                .define("Prevent Sleep", true);
        BUILDER.pop();

        SPEC = BUILDER.build();


        Supplier<Level> level = () -> WorldHelper.getServer().getLevel(Level.OVERWORLD);
        SchedulerHelper.scheduleUntilLoaded(() ->
        {
            if (favoredOres.get().isEmpty())
            {
                List<? extends List<?>> defaultOres = ForgeRegistries.BLOCKS.getValues().stream()
                                                      .filter(block -> block.builtInRegistryHolder().is(Tags.Blocks.ORES_IN_GROUND_STONE))
                                                      .map(ore ->
                                                      {
                                                          String oreName = ConfigSettings.stripOreName(ForgeRegistries.BLOCKS.getKey(ore).getPath());
                                                          return List.of(oreName, List.of("all"), List.of("all"), List.of("all"));
                                                      })
                                                      .distinct().toList();
                favoredOres.set(defaultOres);
                SPEC.save();
            }
        }, 5, level, () -> level.get().getSharedSpawnPos());
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
