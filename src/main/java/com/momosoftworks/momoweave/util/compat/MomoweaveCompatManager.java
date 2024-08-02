package com.momosoftworks.momoweave.util.compat;

import com.momosoftworks.coldsweat.util.compat.CompatManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import top.theillusivec4.curios.api.event.CurioDropsEvent;

@Mod.EventBusSubscriber
public class MomoweaveCompatManager
{
    static
    {
        if (CompatManager.isCuriosLoaded())
        {
            MinecraftForge.EVENT_BUS.register(new Object()
            {
                @SubscribeEvent
                public void cancelCurioDrops(CurioDropsEvent event)
                {   event.setCanceled(true);
                }
            });
        }
    }
}
