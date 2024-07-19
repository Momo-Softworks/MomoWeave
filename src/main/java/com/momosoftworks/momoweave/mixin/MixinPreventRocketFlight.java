package com.momosoftworks.momoweave.mixin;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FireworkRocketItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FireworkRocketItem.class)
public class MixinPreventRocketFlight
{
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void preventRocketFlight(Level level, Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir)
    {
        if (player.isFallFlying())
        {   cir.setReturnValue(InteractionResultHolder.pass(player.getItemInHand(hand)));
        }
    }
}
