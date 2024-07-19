package com.momosoftworks.momoweave.event.common;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.eventbus.api.Event;

public class BeforeServerStartEvent extends Event
{
    MinecraftServer server;

    public BeforeServerStartEvent(MinecraftServer server)
    {   this.server = server;
    }

    public MinecraftServer getServer()
    {   return server;
    }
}
