package com.momosoftworks.momoweave.mixin;

import com.momosoftworks.momoweave.event.common.BeforeServerStartEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLifecycleHooks.class)
public class MixinBeforeServerStart
{
    @Inject(method = "handleServerAboutToStart", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/server/ServerLifecycleHooks;runModifiers(Lnet/minecraft/server/MinecraftServer;)V", shift = At.Shift.BEFORE), remap = false)
    private static void onServerAboutToStart(MinecraftServer server, CallbackInfoReturnable<Boolean> cir)
    {   MinecraftForge.EVENT_BUS.post(new BeforeServerStartEvent(server));
    }
}
