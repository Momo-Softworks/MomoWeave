package com.momosoftworks.momoweave.core.network.message;

import com.momosoftworks.momoweave.common.container.LootTableMenu;
import com.momosoftworks.momoweave.core.network.MomoweavePacketHandler;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkHooks;
import net.minecraftforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class RequestLootPageMessage
{
    ResourceLocation lootTableName;

    public RequestLootPageMessage(ResourceLocation lootTableName)
    {   this.lootTableName = lootTableName;
    }

    public static void encode(RequestLootPageMessage message, FriendlyByteBuf buffer)
    {   buffer.writeResourceLocation(message.lootTableName);
    }

    public static RequestLootPageMessage decode(FriendlyByteBuf buffer)
    {
        ResourceLocation lootTableName = buffer.readResourceLocation();
        return new RequestLootPageMessage(lootTableName);
    }

    public static void handle(RequestLootPageMessage message, Supplier<NetworkEvent.Context> contextSupplier)
    {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() ->
        {
            if (context.getSender() != null)
            {
                ResourceLocation lootTableName = message.lootTableName;
                context.getSender().getPersistentData().putString("LootTableName", lootTableName.toString());
                NetworkHooks.openScreen(context.getSender(), new MenuProvider()
                {
                    @Override
                    public Component getDisplayName()
                    {   return Component.literal(lootTableName.toString());
                    }

                    @Nullable
                    @Override
                    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer)
                    {   return new LootTableMenu(pContainerId, lootTableName, context.getSender());
                    }
                });
            }
        });
        context.setPacketHandled(true);
    }
}