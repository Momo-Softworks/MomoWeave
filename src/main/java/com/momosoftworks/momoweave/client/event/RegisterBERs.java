package com.momosoftworks.momoweave.client.event;

import com.momosoftworks.momoweave.client.blockentityrenderer.GeodeBlockEntityRenderer;
import com.momosoftworks.momoweave.core.init.BlockEntityInit;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class RegisterBERs
{
    @SubscribeEvent
    public static void registerBERs(EntityRenderersEvent.RegisterRenderers event)
    {
        event.registerBlockEntityRenderer(BlockEntityInit.GEODE.get(), GeodeBlockEntityRenderer::new);
    }
}
