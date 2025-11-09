package com.momosoftworks.momoweave.mixin;

import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.momoweave.common.event.DeathBagHandler;
import com.momosoftworks.momoweave.common.item.DeathBagItem;
import com.momosoftworks.momoweave.common.level.save_data.LostDeathBagsData;
import com.momosoftworks.momoweave.config.ItemPrice;
import com.momosoftworks.momoweave.config.ConfigSettings;
import com.momosoftworks.momoweave.core.init.ItemInit;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.WanderingTrader;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collection;
import java.util.List;

@Mixin(WanderingTrader.class)
public class MixinAddWanderingTrades
{
    WanderingTrader self = (WanderingTrader)(Object)this;
    @Inject(method = "mobInteract", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/WanderingTrader;setTradingPlayer(Lnet/minecraft/world/entity/player/Player;)V", shift = At.Shift.AFTER))
    private void addWanderingTrades(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir)
    {
        self.getOffers().removeIf(trade -> trade.getResult().is(ItemInit.DEATH_BAG.get()) || trade.getResult().isEmpty());
        Collection<LostDeathBagsData.BagData> bags = LostDeathBagsData.INSTANCE.getLostBags(player);
        for (LostDeathBagsData.BagData bagData : bags)
        {
            ItemStack bag = bagData.bag();
            ItemStack[] charges = getChargesForPlayer(player, bagData);
            self.getOffers().add(new MerchantOffer(charges[0], charges[1], bag, 1, 0, 0));
        }
    }

    private static ItemStack getCostForValue(int value)
    {
        List<Item> pool = switch (value)
        {
            case 0 -> ConfigSettings.TRADER_BUYBACK_ITEMS.get().get(ItemPrice.WORTHLESS);
            case 1 -> ConfigSettings.TRADER_BUYBACK_ITEMS.get().get(ItemPrice.CHEAP);
            case 2 -> ConfigSettings.TRADER_BUYBACK_ITEMS.get().get(ItemPrice.COSTLY);
            default -> ConfigSettings.TRADER_BUYBACK_ITEMS.get().get(ItemPrice.EXTORTIONATE);
        };
        RandomSource rand = RandomSource.create();
        Item item = pool.get(rand.nextInt(0, pool.size()));
        return new ItemStack(item, RandomSource.create().nextIntBetweenInclusive(1, Math.min(8, item.getDefaultInstance().getMaxStackSize())));
    }

    private static int getToolHarvestLevel(ItemStack itemStack)
    {
        if (itemStack.getItem() instanceof TieredItem)
        {
            TieredItem tieredItem = (TieredItem) itemStack.getItem();
            Tier tier = tieredItem.getTier();
            return tier.getLevel();
        }
        return 0;
    }

    private ItemStack[] getChargesForPlayer(Player player, LostDeathBagsData.BagData bagData)
    {
        ItemStack[] charges = DeathBagHandler.getBagCharges(self, player.getUUID(), bagData);
        ItemStack bag = bagData.bag();

        if (charges[0].isEmpty() && charges[1].isEmpty())
        {
            List<ItemStack> bagItems = DeathBagItem.deserializeContents(bag);
            int highestEnchantmentValue = CSMath.clamp(bagItems
                                                       .stream()
                                                       .mapToInt(stack -> EnchantmentHelper.getEnchantments(stack).values()
                                                                          .stream().mapToInt(val -> val).sum())
                                                       .max().orElse(0),
                                                       0, 3);
            int highestRarity = CSMath.clamp(bagItems
                                             .stream()
                                             .mapToInt(stack -> stack.getRarity().ordinal())
                                             .max().orElse(0),
                                             0, 3);
            int highestHarvestLevel = CSMath.clamp(bagItems
                                                   .stream()
                                                   .mapToInt(stack -> getToolHarvestLevel(stack))
                                                   .max().orElse(0),
                                                   0, 3);
            int highestProtectionLevel = CSMath.clamp(bagItems
                                                      .stream()
                                                      .mapToInt(stack ->
                                                      {
                                                          if (stack.getItem() instanceof Equipable)
                                                          {
                                                              return
                                                              CSMath.getIfNotNull(stack.getAttributeModifiers(Mob.getEquipmentSlotForItem(stack)).get(Attributes.ARMOR),
                                                                                  attributes -> attributes.stream()
                                                                                                .mapToInt(attr -> (int) attr.getAmount())
                                                                                                .sum(), 0);
                                                          }
                                                          return 0;
                                                      })
                                                      .max().orElse(0),
                                                      0, 3);
            int experienceLevel = (int) CSMath.blend(0, 3, player.experienceLevel, 0, 30);
            Integer[] chargeLevels = new Integer[2];

            for (int level : List.of(highestEnchantmentValue, highestRarity, highestHarvestLevel, highestProtectionLevel, experienceLevel))
            {
                for (int i = 0; i < chargeLevels.length; i++)
                {
                    if (chargeLevels[i] == null || level >= chargeLevels[i])
                    {
                        chargeLevels[i] = level;
                        break;
                    }
                }
            }
            charges[0] = getCostForValue(chargeLevels[0]);
            if (chargeLevels[1] != null)
            {   charges[1] = getCostForValue(chargeLevels[1]);
            }
            DeathBagHandler.addBagCharge(self, player.getUUID(), bagData, charges[0], charges[1]);
        }
        return charges;
    }
}
