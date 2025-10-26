package com.momosoftworks.momoweave.mixin;

import com.momosoftworks.momoweave.event.common.EntityRemovedEvent;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public class MixinEntityRemoved
{
    Entity self = (Entity) (Object) this;

    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void onDestroyed(Entity.RemovalReason reason, CallbackInfo ci)
    {
        EntityRemovedEvent event = new EntityRemovedEvent(self, reason);
        if (MinecraftForge.EVENT_BUS.post(event))
        {   ci.cancel();
        }
    }
}