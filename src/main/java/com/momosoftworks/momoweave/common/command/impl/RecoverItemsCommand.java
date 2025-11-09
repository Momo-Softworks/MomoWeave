package com.momosoftworks.momoweave.common.command.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.momosoftworks.coldsweat.common.command.BaseCommand;
import com.momosoftworks.momoweave.common.item.DeathBagItem;
import com.momosoftworks.momoweave.common.level.save_data.LostDeathBagsData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class RecoverItemsCommand extends BaseCommand
{
    public RecoverItemsCommand(String name, int permissionLevel, boolean enabled)
    {   super(name, permissionLevel, enabled);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> setExecution()
    {   return builder
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("timestamp", StringArgumentType.string()).suggests(getTimestampSuggestions())
                        .executes(this::executeRecoverItems)
                    )
                );
    }

    private static SuggestionProvider<CommandSourceStack> getTimestampSuggestions()
    {
        return (context, builder) -> {
            Player player = EntityArgument.getPlayer(context, "player");
            for (LostDeathBagsData.BagData bagData : LostDeathBagsData.INSTANCE.getLostBags(player))
            {
                long timestamp = bagData.lostTime();
                Instant instant = Instant.ofEpochMilli(timestamp);
                LocalDateTime dateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
                String dataTimeString = LostDeathBagsData.TIMESTAMP_FORMATTER.format(dateTime);
                builder.suggest(dataTimeString);
            }
            return builder.buildFuture();
        };
    }

    private int executeRecoverItems(CommandContext<CommandSourceStack> context) throws CommandSyntaxException
    {
        String dateTime = StringArgumentType.getString(context, "timestamp");
        Player player = EntityArgument.getPlayer(context, "player");

        long timestamp = LocalDateTime.parse(dateTime, LostDeathBagsData.TIMESTAMP_FORMATTER)
                         .atZone(ZoneId.systemDefault())
                         .toInstant()
                         .toEpochMilli();
        LostDeathBagsData.INSTANCE.getLostBags(player).stream()
            .filter(bagData ->
                    {
                        // check if timestamp matches within 1 second
                        long bagTimestamp = bagData.lostTime();
                        return Math.abs(bagTimestamp - timestamp) < 1000;
                    })
            .forEach(bagData -> {
                ItemStack deathBag = bagData.bag();
                DeathBagItem.deserializeContents(deathBag).forEach(stack -> {
                    if (!player.getInventory().add(stack))
                    {   player.drop(stack, false);
                    }
                });
                LostDeathBagsData.INSTANCE.removeLostBag(player.getUUID(), bagData.bag());
            });
        return 0;
    }
}
