package com.momosoftworks.momoweave.core.init;

import com.mojang.brigadier.CommandDispatcher;
import com.momosoftworks.coldsweat.ColdSweat;
import com.momosoftworks.coldsweat.common.command.BaseCommand;
import com.momosoftworks.coldsweat.common.command.argument.AbilityOrTempTypeArgument;
import com.momosoftworks.coldsweat.common.command.argument.TempModifierTypeArgument;
import com.momosoftworks.coldsweat.common.command.argument.TemperatureTypeArgument;
import com.momosoftworks.coldsweat.common.command.impl.TempCommand;
import com.momosoftworks.momoweave.common.command.impl.LootTablesCommand;
import com.momosoftworks.momoweave.common.command.impl.WhereAmICommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;

@Mod.EventBusSubscriber
public class CommandInit
{
    private static final ArrayList<BaseCommand> COMMANDS = new ArrayList<>();

    @SubscribeEvent
    public static void registerCommands(final RegisterCommandsEvent event)
    {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        COMMANDS.add(new WhereAmICommand("whereami", 2, true));
        COMMANDS.add(new LootTablesCommand("showloottables", 2, true));

        COMMANDS.forEach(command ->
        {
            if (command.isEnabled() && command.setExecution() != null)
            {   dispatcher.register(command.getBuilder());
            }
        });
    }
}
