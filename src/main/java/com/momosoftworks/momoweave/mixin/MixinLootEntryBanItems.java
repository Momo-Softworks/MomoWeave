package com.momosoftworks.momoweave.mixin;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraftforge.common.Tags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

@Mixin(LootTable.class)
public abstract class MixinLootEntryBanItems
{
    @Shadow
    protected abstract ObjectArrayList<ItemStack> getRandomItems(LootContext pParams);

    @Inject(method = "fill", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/loot/LootContext;getRandom()Lnet/minecraft/util/RandomSource;"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void banItems(Container pContainer, LootParams pParams, long pSeed, CallbackInfo ci, LootContext lootcontext, ObjectArrayList<ItemStack> objectarraylist)
    {
        ObjectArrayList<ItemStack> newList = new ObjectArrayList<>(objectarraylist);
        Predicate<ItemStack> isBanned = itemStack -> itemStack.is(Tags.Items.INGOTS) || itemStack.is(Items.DIAMOND);

        newList.removeIf(isBanned);

        if (newList.size() < objectarraylist.size())
        {
            List<ItemStack> newStacks = new ArrayList<>(Stream.of(getRandomItems(lootcontext),
                                                                getRandomItems(lootcontext),
                                                                getRandomItems(lootcontext),
                                                                getRandomItems(lootcontext),
                                                                getRandomItems(lootcontext)).flatMap(List::stream).filter(stack -> !isBanned.test(stack)).toList());
            int i = 0;
            while (newList.size() < objectarraylist.size())
            {
                if (i >= Math.min(20, newStacks.size()))
                {   break;
                }
                newList.add(newStacks.get(i));
                newStacks.remove(i);
                i++;
            }
        }

        objectarraylist.clear();
        objectarraylist.addAll(newList);
    }
}
