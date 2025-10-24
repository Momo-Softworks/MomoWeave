package com.momosoftworks.momoweave.config;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.mojang.datafixers.util.Either;
import com.momosoftworks.coldsweat.data.codec.util.NegatableList;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.ConfigHelper;
import com.momosoftworks.coldsweat.util.serialization.DynamicHolder;
import com.momosoftworks.momoweave.Momoweave;
import com.momosoftworks.momoweave.data.biome_modifier.ExtraOresBiomeModifier;
import com.momosoftworks.momoweave.data.biome_modifier.OreClimateSettings;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Mod.EventBusSubscriber
public class ConfigSettings
{
    public static final Map<ResourceLocation, OreClimateSettings> CONFIGURED_FAVORED_ORES = new HashMap<>();
    public static final Multimap<Holder<Biome>, Block> FAVORED_ORE_BLOCKS_PER_BIOME = HashMultimap.create();

    public static final DynamicHolder<Double> FAVORED_ORE_MULTIPLIER = DynamicHolder.create(null, MainSettingsConfig.favoredOreMultiplier);
    public static final DynamicHolder<Double> UNFAVORED_ORE_MULTIPLIER = DynamicHolder.create(null, MainSettingsConfig.unfavoredOreMultiplier);
    public static final DynamicHolder<List<? extends Integer>> FAVORED_ORE_COUNT_PER_BIOME = DynamicHolder.create(null, MainSettingsConfig.favoredOresPerBiome);
    public static final DynamicHolder<Map<ItemPrice, List<Item>>> TRADER_BUYBACK_ITEMS = DynamicHolder.create(null, () ->
    {
        Map<ItemPrice, List<Item>> map = new HashMap<>();
        Function<NegatableList<Either<TagKey<Item>, Item>>, List<Item>> itemGetter = list ->
            list.flatMap(either -> either.map(tag -> ForgeRegistries.ITEMS.tags().getTag(tag).stream().toList(), List::of),
                         CSMath::append,
                         List::removeAll)
                .orElse(List.of());
        map.put(ItemPrice.WORTHLESS, itemGetter.apply(ConfigHelper.getItems(MainSettingsConfig.worthlessTraderItems.get().toArray(String[]::new))));
        map.put(ItemPrice.CHEAP, itemGetter.apply(ConfigHelper.getItems(MainSettingsConfig.cheapTraderItems.get().toArray(String[]::new))));
        map.put(ItemPrice.COSTLY, itemGetter.apply(ConfigHelper.getItems(MainSettingsConfig.costlyTraderItems.get().toArray(String[]::new))));
        map.put(ItemPrice.EXTORTIONATE, itemGetter.apply(ConfigHelper.getItems(MainSettingsConfig.extortionateTraderItems.get().toArray(String[]::new))));
        return map;
    });

    public static Map<ResourceLocation, OreClimateSettings> getQualifyingOresForBiome(Holder<Biome> biome)
    {
        return ConfigSettings.CONFIGURED_FAVORED_ORES.entrySet().stream()
                .filter(entry -> entry.getValue().matches(biome))
                .collect(HashMap::new, (m, v) -> m.put(v.getKey(), v.getValue()), HashMap::putAll);
    }

    public static String stripOreName(String ore)
    {   return ore.replace("_ore", "").replace("ore_", "").replace("_deepslate", "").replace("deepslate_", "");
    }

    public static void compileOres(MinecraftServer server)
    {
        RegistryAccess registries = server.registryAccess();
        Registry<PlacedFeature> featureRegistry = registries.registryOrThrow(Registries.PLACED_FEATURE);

        // Clear existing ores to ensure we're starting fresh
        CONFIGURED_FAVORED_ORES.clear();

        // Compile all ore features, including modded ones
        for (PlacedFeature feature : featureRegistry)
        {
            if (ExtraOresBiomeModifier.featureContainsOverworldOres(feature))
            {
                ResourceLocation oreId = featureRegistry.getKey(feature);
                CONFIGURED_FAVORED_ORES.put(oreId, OreClimateSettings.createAny(oreId));
                Momoweave.LOGGER.info("Added ore to favored list: {}", oreId);
            }
        }

        for (List<?> list : MainSettingsConfig.favoredOres.get())
        {
            String ore = (String) list.get(0);
            List<String> temperatures = (List<String>) list.get(1);
            List<String> humidities = (List<String>) list.get(2);
            List<String> altitudes = (List<String>) list.get(3);

            if (ore.contains(":"))
            {
                ResourceLocation oreId = new ResourceLocation(ore);
                if (!featureRegistry.containsKey(oreId))
                {   Momoweave.LOGGER.error("Invalid ore ID: {}", ore);
                    continue;
                }

                CONFIGURED_FAVORED_ORES.put(oreId, new OreClimateSettings(OreClimateSettings.Temperature.byNames(temperatures),
                                                                          OreClimateSettings.Humidity.byNames(humidities),
                                                                          OreClimateSettings.Altitude.byNames(altitudes),
                                                                          stripOreName(oreId.getPath())));
            }
            else
            {
                OreClimateSettings settings = new OreClimateSettings(OreClimateSettings.Temperature.byNames(temperatures),
                                                                     OreClimateSettings.Humidity.byNames(humidities),
                                                                     OreClimateSettings.Altitude.byNames(altitudes),
                                                                     ore);
                for (PlacedFeature feature : featureRegistry)
                {
                    if (!ExtraOresBiomeModifier.getOresForFeature(feature).isEmpty())
                    {
                        ResourceLocation id = featureRegistry.getKey(feature);
                        if (id.getPath().contains(ore) && ExtraOresBiomeModifier.featureContainsOverworldOres(feature))
                        {
                            CONFIGURED_FAVORED_ORES.put(id, settings);
                            Momoweave.LOGGER.info("Added favored ore from configs: {}", id);
                        }
                    }
                }
            }
        }

        // Ensure we have at least one ore
        if (CONFIGURED_FAVORED_ORES.isEmpty())
        {   CONFIGURED_FAVORED_ORES.put(new ResourceLocation("minecraft:coal_ore_lower"), OreClimateSettings.createAny("coal"));
        }

        // Initialize spawn probabilities for all ores
        for (ResourceLocation oreId : CONFIGURED_FAVORED_ORES.keySet())
        {   ExtraOresBiomeModifier.ORE_SPAWN_PROBABILITIES.putIfAbsent(oreId, 1.0);
        }

        Momoweave.LOGGER.info("Compiled {} favored ores", CONFIGURED_FAVORED_ORES.size());
    }
}
