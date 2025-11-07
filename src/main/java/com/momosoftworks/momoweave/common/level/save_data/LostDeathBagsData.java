package com.momosoftworks.momoweave.common.level.save_data;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.momoweave.Momoweave;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Mod.EventBusSubscriber
public class LostDeathBagsData
{
    public static final LostDeathBagsData INSTANCE = new LostDeathBagsData();
    private static DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event)
    {   INSTANCE.load(event.getServer().registryAccess());
    }

    @SubscribeEvent
    public static void onServerClosing(ServerStoppingEvent event)
    {   INSTANCE.save(event.getServer().registryAccess());
    }

    private final Multimap<UUID, BagData> lostBags = HashMultimap.create();

    public LostDeathBagsData()
    {}

    public Multimap<UUID, BagData> getLostBags()
    {   return lostBags;
    }

    public Collection<BagData> getLostBags(Player player)
    {   return lostBags.get(player.getUUID());
    }

    public void addLostBag(UUID uuid, ItemStack bag, long timestamp)
    {   lostBags.put(uuid, new BagData(bag.copy(), timestamp));
    }

    public void removeLostBag(UUID uuid, ItemStack bag)
    {   lostBags.entries().removeIf(entry -> entry.getKey().equals(uuid) && ItemStack.isSameItemSameTags(entry.getValue().bag(), bag));
    }

    public void save(RegistryAccess registryAccess)
    {
        Path dataPath = FMLPaths.GAMEDIR.get().resolve("momoweave").resolve("lost_death_bags");
        try
        {   FileUtils.deleteDirectory(dataPath.toFile());
        }
        catch (IOException e)
        {   throw new RuntimeException(e);
        }
        RegistryOps<JsonElement> encoderOps = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
        lostBags.asMap().forEach((uuid, bags) ->
        {
            File userDirectory = dataPath.resolve(uuid.toString()).toFile();
            if (!userDirectory.mkdirs())
            {   Momoweave.LOGGER.error("Failed to create directory for lost death bags of user: {}", uuid);
            }
            for (BagData bag : bags)
            {
                JsonElement bagJson = BagData.CODEC.encodeStart(encoderOps, bag).result().orElse(null);
                if (bagJson != null && bagJson.isJsonObject())
                {
                    Instant instant = Instant.ofEpochMilli(bag.lostTime());
                    LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                    String formattedTime = dateTime.format(TIMESTAMP_FORMATTER);
                    String fileName = "bag_" + formattedTime + ".json";
                    File bagFile = userDirectory.toPath().resolve(fileName).toFile();
                    Gson gson = new Gson();
                    try (FileWriter writer = new FileWriter(bagFile))
                    {   gson.toJson(bagJson, writer);
                    }
                    catch (IOException e)
                    {   Momoweave.LOGGER.error("Failed to save lost death bag for user: {}", uuid, e);
                    }
                }
            }
        });
    }

    public void load(RegistryAccess registryAccess)
    {
        this.lostBags.clear();
        Path dataPath = FMLPaths.GAMEDIR.get().resolve("momoweave").resolve("lost_death_bags");
        File dataDirectory = dataPath.toFile();
        if (dataDirectory.exists() && dataDirectory.isDirectory())
        {
            File[] userDirectories = dataDirectory.listFiles(File::isDirectory);
            if (userDirectories != null)
            {
                RegistryOps<JsonElement> decoderOps = RegistryOps.create(JsonOps.INSTANCE, registryAccess);
                for (File userDir : userDirectories)
                {
                    try
                    {
                        UUID uuid = UUID.fromString(userDir.getName());
                        File[] bagFiles = userDir.listFiles((dir, name) -> name.endsWith(".json"));
                        if (bagFiles != null)
                        {
                            for (File bagFile : bagFiles)
                            {
                                try (FileReader reader = new FileReader(bagFile))
                                {
                                    Gson gson = new Gson();
                                    JsonElement bagJson = gson.fromJson(reader, JsonElement.class);
                                    BagData bagData = BagData.CODEC.parse(decoderOps, bagJson).result().orElse(null);
                                    if (bagData != null)
                                    {   lostBags.put(uuid, bagData);
                                    }
                                }
                                catch (IOException e)
                                {   Momoweave.LOGGER.error("Failed to load lost death bag from file: {}", bagFile.getName(), e);
                                }
                            }
                        }
                    }
                    catch (IllegalArgumentException e)
                    {   Momoweave.LOGGER.error("Failed to load lost death bags from directory: {}", userDir.getName(), e);
                    }
                }
            }
        }
    }

    public record BagData(ItemStack bag, long lostTime)
    {
        public static final Codec<BagData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ItemStack.CODEC.fieldOf("bag").forGetter(BagData::bag),
                Codec.LONG.fieldOf("lostTime").forGetter(BagData::lostTime)
        ).apply(instance, BagData::new));
    }
}
