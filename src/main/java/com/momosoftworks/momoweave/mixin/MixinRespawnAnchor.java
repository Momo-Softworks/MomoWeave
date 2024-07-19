package com.momosoftworks.momoweave.mixin;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RespawnAnchorBlock.class)
public class MixinRespawnAnchor
{
    @Inject(method = "canSetSpawn", at = @At("HEAD"), cancellable = true)
    private static void canSetSpawn(Level level, CallbackInfoReturnable<Boolean> cir)
    {   cir.setReturnValue(true);
    }
}
