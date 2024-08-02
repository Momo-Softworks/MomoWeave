package com.momosoftworks.momoweave.mixin;

import com.momosoftworks.coldsweat.util.compat.CompatManager;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.world.WorldHelper;
import com.momosoftworks.momoweave.common.capability.ModCapabilities;
import com.momosoftworks.momoweave.common.level.LostDeathBagsData;
import com.momosoftworks.momoweave.common.level.SavedDataHelper;
import com.momosoftworks.momoweave.core.init.ItemInit;
import com.momosoftworks.momoweave.event.common.ItemEntityDestroyedEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(Inventory.class)
public class MixinDropItems
{
    Inventory self = (Inventory)(Object)this;
    private static ItemStack BAG = null;

    @Inject(method = "dropAll", at = @At(value = "HEAD"))
    private void initBag(CallbackInfo ci)
    {
        BAG = new ItemStack(ItemInit.BAG_OF_THE_PERISHED.get());
        BAG.getCapability(ModCapabilities.DEATH_POUCH_ITEMS).ifPresent(cap ->
        {
            // Add curios to the bag
            if (CompatManager.isCuriosLoaded())
            {
                for (ItemStack curio : CompatManager.getCurios(self.player))
                {
                    if (!curio.isEmpty())
                    {   cap.addItem(curio.copy());
                    }
                }
            }
        });
    }

    @Redirect(method = "dropAll", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;drop(Lnet/minecraft/world/item/ItemStack;ZZ)Lnet/minecraft/world/entity/item/ItemEntity;"))
    private ItemEntity fillBagItems(Player player, ItemStack stack, boolean f7, boolean f8)
    {
        BAG.getCapability(ModCapabilities.DEATH_POUCH_ITEMS).ifPresent(cap ->
        {
            if (stack.is(ItemInit.BAG_OF_THE_PERISHED.get()))
            {   WorldHelper.entityDropItem(player, stack, LostDeathBagsData.BAG_EXPIRATION_TIME);
            }
            else if (!stack.isEmpty())
            {   cap.addItem(stack.copy());
            }
        });
        return null;
    }

    @Inject(method = "dropAll", at = @At(value = "TAIL"))
    private void dropBag(CallbackInfo ci)
    {
        Player player = self.player;
        BAG.getCapability(ModCapabilities.DEATH_POUCH_ITEMS).ifPresent(cap ->
        {
            if (!cap.getItems().isEmpty())
            {
                BAG.getOrCreateTag().putUUID("Owner", player.getUUID());
                BAG.getOrCreateTag().putUUID("ID", UUID.randomUUID());
                BAG.setHoverName(Component.literal(player.getDisplayName().getString() + "'s " + BAG.getHoverName().getString()));
                WorldHelper.entityDropItem(player, BAG, LostDeathBagsData.BAG_EXPIRATION_TIME);
            }
        });
    }

    @Mixin(ItemEntity.class)
    public static class InjectDestroyed
    {
        ItemEntity self = (ItemEntity)(Object)this;

        @Inject(method = "hurt",
                at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;onDestroyed(Lnet/minecraft/world/entity/item/ItemEntity;Lnet/minecraft/world/damagesource/DamageSource;)V"),
                cancellable = true)
        private void onDestroyed(DamageSource source, float damage, CallbackInfoReturnable<Boolean> cir)
        {
            ItemEntityDestroyedEvent event = new ItemEntityDestroyedEvent(self);
            if (MinecraftForge.EVENT_BUS.post(event))
            {   cir.setReturnValue(false);
            }
        }
    }
}