package com.momosoftworks.momoweave.common.command.impl;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.momosoftworks.coldsweat.common.command.BaseCommand;
import com.momosoftworks.momoweave.common.container.LootTableMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.network.NetworkHooks;

import java.lang.reflect.Field;

public class LootTablesCommand extends BaseCommand
{
    public LootTablesCommand(String name, int permissionLevel, boolean enabled)
    {   super(name, permissionLevel, enabled);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> setExecution()
    {   return builder.executes(source -> executeShowLootTables(source.getSource()));
    }

    static final Field ENTRIES;
    static final Field POOLS;
    static final Field CONTAINER_ITEMS;
    static
    {
        POOLS = ObfuscationReflectionHelper.findField(LootTable.class, "f_79109_");
        ENTRIES = ObfuscationReflectionHelper.findField(LootPool.class, "f_79023_");
        CONTAINER_ITEMS = ObfuscationReflectionHelper.findField(SimpleContainer.class, "f_19147_");
        POOLS.setAccessible(true);
        ENTRIES.setAccessible(true);
        CONTAINER_ITEMS.setAccessible(true);
    }

    private int executeShowLootTables(CommandSourceStack source)
    {
        if (source.getEntity() instanceof ServerPlayer player)
        {
            try
            {
                ResourceLocation lootTableName = !player.getPersistentData().getString("LootTableName").isEmpty()
                                                 ? new ResourceLocation(player.getPersistentData().getString("LootTableName"))
                                                 : new ResourceLocation("minecraft:chests/simple_dungeon");
                NetworkHooks.openScreen(player, new MenuProvider()
                {
                    @Override
                    public Component getDisplayName()
                    {   return Component.literal("Loot");
                    }

                    @Override
                    public LootTableMenu createMenu(int id, Inventory playerInventory, Player player)
                    {   return new LootTableMenu(id, lootTableName, (ServerPlayer) player);
                    }
                });
            }
            catch (Exception e)
            {   e.printStackTrace();
            }
        }

        return 1;
    }
}
