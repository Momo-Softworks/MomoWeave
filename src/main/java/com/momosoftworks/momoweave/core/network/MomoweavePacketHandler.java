package com.momosoftworks.momoweave.core.network;

import com.momosoftworks.momoweave.Momoweave;
import com.momosoftworks.momoweave.core.network.message.RequestLootPageMessage;
import com.momosoftworks.momoweave.core.network.message.SyncLootContainerMessage;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class MomoweavePacketHandler
{
    private static final String PROTOCOL_VERSION = "0.1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Momoweave.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void init()
    {
        INSTANCE.registerMessage(0, SyncLootContainerMessage.class, SyncLootContainerMessage::encode, SyncLootContainerMessage::decode, SyncLootContainerMessage::handle);
        INSTANCE.registerMessage(1, RequestLootPageMessage.class, RequestLootPageMessage::encode, RequestLootPageMessage::decode, RequestLootPageMessage::handle);
    }
}
