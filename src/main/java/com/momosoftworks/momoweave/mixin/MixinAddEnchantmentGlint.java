package com.momosoftworks.momoweave.mixin;

import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.common.Mod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Item.class)
public class MixinAddEnchantmentGlint
{
    @Inject(method = "isFoil", at = @At("HEAD"), cancellable = true)
    private void isFoil(ItemStack stack, CallbackInfoReturnable<Boolean> cir)
    {
        if (NBTHelper.getTagOrEmpty(stack).getBoolean("momo:Enchanted"))
        {   cir.setReturnValue(true);
        }
    }
}
