package com.momosoftworks.momoweave.mixin;

import com.momosoftworks.momoweave.config.MainSettingsConfig;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.LootItemFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.providers.number.*;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

@Mixin(LootPool.class)
public class MixinLootTables
{
    @Shadow
    NumberProvider rolls;

    private static Field MIN_VAL_UNIFORM = ObfuscationReflectionHelper.findField(UniformGenerator.class, "f_165774_");
    private static Field MAX_VAL_UNIFORM = ObfuscationReflectionHelper.findField(UniformGenerator.class, "f_165775_");

    private static Field BINOMIAL_N_PROVIDER = ObfuscationReflectionHelper.findField(BinomialDistributionGenerator.class, "f_165653_");
    private static Field BINOMIAL_P_PROVIDER = ObfuscationReflectionHelper.findField(BinomialDistributionGenerator.class, "f_165654_");

    private static Field SCOREBOARD_SCALE = ObfuscationReflectionHelper.findField(ScoreboardValue.class, "f_165743_");

    static
    {
        MIN_VAL_UNIFORM.setAccessible(true);
        MAX_VAL_UNIFORM.setAccessible(true);
        BINOMIAL_N_PROVIDER.setAccessible(true);
        BINOMIAL_P_PROVIDER.setAccessible(true);
        SCOREBOARD_SCALE.setAccessible(true);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onLootPoolInit(LootPoolEntryContainer[] p_165128_, LootItemCondition[] p_165129_, LootItemFunction[] p_165130_, NumberProvider rollProvider, NumberProvider p_165132_, String name, CallbackInfo ci)
    {
        this.rolls = multiplyLootRolls(rollProvider);
    }

    private static NumberProvider multiplyLootRolls(NumberProvider rollProvider)
    {
        NumberProvider rolls = rollProvider;
        float rollMultiplier = MainSettingsConfig.lootRollMultiplier.get().floatValue();

        if (rolls instanceof ConstantValue constant)
        {   rolls = ConstantValue.exactly(constant.getFloat(null) * rollMultiplier);
        }
        else if (rolls instanceof UniformGenerator uniform)
        {
            try
            {   MIN_VAL_UNIFORM.set(uniform, multiplyLootRolls((NumberProvider) MIN_VAL_UNIFORM.get(uniform)));
                MAX_VAL_UNIFORM.set(uniform, multiplyLootRolls((NumberProvider) MAX_VAL_UNIFORM.get(uniform)));
            }
            catch (IllegalAccessException e)
            {   e.printStackTrace();
            }
        }
        else if (rolls instanceof BinomialDistributionGenerator binomial)
        {
            try
            {   BINOMIAL_N_PROVIDER.set(binomial, multiplyLootRolls((NumberProvider) BINOMIAL_N_PROVIDER.get(binomial)));
                BINOMIAL_P_PROVIDER.set(binomial, multiplyLootRolls((NumberProvider) BINOMIAL_P_PROVIDER.get(binomial)));
            }
            catch (IllegalAccessException e)
            {   e.printStackTrace();
            }
        }
        else if (rolls instanceof ScoreboardValue scoreboard)
        {
            try
            {   SCOREBOARD_SCALE.set(scoreboard, (float) SCOREBOARD_SCALE.get(scoreboard) * rollMultiplier);
            }
            catch (IllegalAccessException e)
            {   e.printStackTrace();
            }
        }

        return rolls;
    }
}
