package com.momosoftworks.momoweave.data.biome_modifier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.RegistryHelper;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import com.momosoftworks.momoweave.config.ConfigSettings;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.biome.Biome;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record OreClimateSettings(List<Temperature> temperatures, List<Humidity> humidities, List<Altitude> altitudes, String oreName)
{
    public static final Codec<OreClimateSettings> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        Temperature.CODEC.listOf().fieldOf("temperatures").forGetter(OreClimateSettings::temperatures),
        Humidity.CODEC.listOf().fieldOf("humidities").forGetter(OreClimateSettings::humidities),
        Altitude.CODEC.listOf().optionalFieldOf("altitudes", List.of()).forGetter(OreClimateSettings::altitudes),
        Codec.STRING.fieldOf("ore_name").forGetter(OreClimateSettings::oreName)
    ).apply(inst, OreClimateSettings::new));

    public static OreClimateSettings createAnyRandomHeight(String name)
    {
        List<Temperature> temperatures = List.of(Temperature.values());
        List<Humidity> humidities = List.of(Humidity.values());

        RandomSource rand = RandomSource.create();
        List<Altitude> availableAltitudes = new ArrayList<>(List.of(Altitude.values()));
        List<Altitude> altitudes = new ArrayList<>();
        int amount = rand.nextIntBetweenInclusive(1, 5);
        for (int i = 0; i < amount; i++)
        {
            Altitude randomAltitude = availableAltitudes.get(rand.nextInt(availableAltitudes.size()));
            altitudes.add(randomAltitude);
            availableAltitudes.remove(randomAltitude);
        }

        return new OreClimateSettings(temperatures, humidities, altitudes, name);
    }

    public static OreClimateSettings createAny(String name)
    {
        List<Temperature> temperatures = List.of(Temperature.values());
        List<Humidity> humidities = List.of(Humidity.values());
        List<Altitude> altitudes = List.of(Altitude.values());
        return new OreClimateSettings(temperatures, humidities, altitudes, name);
    }

    public static OreClimateSettings createAny(ResourceLocation oreId)
    {   return createAny(ConfigSettings.stripOreName(oreId.getPath()));
    }

    public boolean matches(Holder<Biome> biome)
    {
        double temperature = CSMath.averagePair(WorldHelper.getBiomeTemperatureRange(RegistryHelper.getRegistryAccess(), biome));
        double humidity = biome.value().getModifiedClimateSettings().downfall();
        return temperatures.stream().anyMatch(temp -> temp == Temperature.get(temperature))
            && humidities.stream().anyMatch(hum -> hum == Humidity.get(humidity));
    }

    public enum Temperature implements StringRepresentable
    {
        FREEZING ("freezing", -1.0, 0.1),
        COOL     ("cool", 0.1, 0.5),
        TEMPERATE("temperate", 0.5, 1.2),
        WARM     ("warm", 1.2, 1.6),
        HOT      ("hot", 1.6, 2.0);

        public static final Codec<Temperature> CODEC = StringRepresentable.fromEnum(Temperature::values);

        private final double min;
        private final double max;
        private final String name;

        Temperature(String name, double min, double max)
        {   this.min = min;
            this.max = max;
            this.name = name;
        }

        public double getMin()
        {   return min;
        }

        public double getMax()
        {   return max;
        }

        public static Temperature get(double temperature)
        {
            for (Temperature temp : values())
            {
                if (temperature >= temp.getMin() && temperature <= temp.getMax())
                {   return temp;
                }
            }
            return TEMPERATE;
        }

        @Override
        public String getSerializedName()
        {   return name;
        }

        public static Temperature byName(String name)
        {
            for (Temperature temp : values())
            {
                if (temp.getSerializedName().equals(name))
                {   return temp;
                }
            }
            return TEMPERATE;
        }

        public static List<Temperature> byNames(List<String> names)
        {
            List<Temperature> temps = new ArrayList<>();
            for (String name : names)
            {
                if (name.equals("all"))
                {   return List.of(values());
                }
                else
                {   temps.add(byName(name));
                }
            }
            return temps;
        }
    }

    public enum Humidity implements StringRepresentable
    {
        DRY   ("dry", 0.0D, 0.4D),
        NORMAL("normal", 0.4D, 0.8D),
        WET   ("wet", 0.8D, 1.0D);

        public static final Codec<Humidity> CODEC = StringRepresentable.fromEnum(Humidity::values);

        private final double min;
        private final double max;
        private final String name;

        Humidity(String name, double min, double max)
        {   this.min = min;
            this.max = max;
            this.name = name;
        }

        public double getMin()
        {   return min;
        }

        public double getMax()
        {   return max;
        }

        public static Humidity get(double humidity)
        {
            for (Humidity hum : values())
            {
                if (humidity >= hum.getMin() && humidity <= hum.getMax())
                {   return hum;
                }
            }
            return NORMAL;
        }
        
        @Override
        public String getSerializedName()
        {   return name;
        }
        
        public static Humidity byName(String name)
        {
            for (Humidity hum : values())
            {
                if (hum.getSerializedName().equals(name))
                {   return hum;
                }
            }
            return NORMAL;
        }

        public static List<Humidity> byNames(List<String> names)
        {
            return names.stream().map(name ->
            {
                if (name.equals("all"))
                {   return List.of(values());
                }
                else
                {   return List.of(byName(name));
                }
            }).flatMap(Collection::stream).toList();
        }
    }

    public enum Altitude implements StringRepresentable
    {
        DEEP   ("deep", -64, 0),
        LOW    ("low", 0, 32),
        MEDIUM ("medium", 32, 84),
        HIGH   ("high", 84, 128),
        EXTREME("extreme", 128, 320);

        public static final Codec<Altitude> CODEC = StringRepresentable.fromEnum(Altitude::values);

        private final int min;
        private final int max;
        private final String name;

        Altitude(String name, int min, int max)
        {   this.min = min;
            this.max = max;
            this.name = name;
        }

        public int getMin()
        {   return min;
        }

        public int getMax()
        {   return max;
        }

        public static Altitude get(int altitude)
        {
            for (Altitude alt : values())
            {
                if (altitude >= alt.getMin() && altitude <= alt.getMax())
                {   return alt;
                }
            }
            return MEDIUM;
        }

        @Override
        public String getSerializedName()
        {   return name;
        }

        public static Altitude byName(String name)
        {
            for (Altitude alt : values())
            {
                if (alt.getSerializedName().equals(name))
                {   return alt;
                }
            }
            return MEDIUM;
        }

        public static List<Altitude> byNames(List<String> names)
        {
            return names.stream().map(name ->
            {
                if (name.equals("all"))
                {   return List.of(values());
                }
                else
                {   return List.of(byName(name));
                }
            }).flatMap(Collection::stream).toList();
        }
    }
}
