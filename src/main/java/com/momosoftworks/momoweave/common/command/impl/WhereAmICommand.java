package com.momosoftworks.momoweave.common.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.momosoftworks.coldsweat.api.util.Temperature;
import com.momosoftworks.coldsweat.common.command.BaseCommand;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import com.momosoftworks.momoweave.config.ConfigSettings;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.registries.ForgeRegistries;

public class WhereAmICommand extends BaseCommand
{
    public WhereAmICommand(String name, int permissionLevel, boolean enabled) {
        super(name, permissionLevel, enabled);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> setExecution()
    {
        return builder.executes(source -> executeWhereAmI(source.getSource()));
    }

    private int executeWhereAmI(CommandSourceStack source)
    {
        if (source.getEntity() instanceof Player player)
        {
            Holder<Biome> biome = source.getLevel().getBiome(BlockPos.containing(source.getPosition()));
            source.sendSuccess(() -> Component.literal("Dimension: " + source.getLevel().dimension().location()).withStyle(ChatFormatting.GRAY), false);

            source.sendSuccess(() -> Component.literal("Biome: " + source.registryAccess().registryOrThrow(Registries.BIOME)
                                                                   .getKey(source.getLevel().getBiome(BlockPos.containing(source.getPosition())).value()).toString())
                                                                   .withStyle(ChatFormatting.GRAY), false);

            source.sendSuccess(() -> Component.literal("Temperature: " + WorldHelper.getTemperatureAt(source.getLevel(), BlockPos.containing(source.getPosition()))).withStyle(ChatFormatting.GRAY), false);

            source.sendSuccess(() -> Component.literal("Structure: " + source.registryAccess().registryOrThrow(Registries.STRUCTURE)
                                                                       .getKey(WorldHelper.getStructureAt(source.getLevel(), BlockPos.containing(source.getPosition())).map(Holder::value).orElse(null)))
                                                                       .withStyle(ChatFormatting.GRAY), false);

            source.sendSuccess(() -> Component.literal("Preferred Ores: " + ConfigSettings.FAVORED_ORE_BLOCKS_PER_BIOME.get(biome).stream().map(ForgeRegistries.BLOCKS::getKey).toList()), false);
        }
        return 1;
    }
}
