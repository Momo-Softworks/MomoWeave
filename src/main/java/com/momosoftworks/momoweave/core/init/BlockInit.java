package com.momosoftworks.momoweave.core.init;

import com.momosoftworks.momoweave.Momoweave;
import com.momosoftworks.momoweave.common.block.GeodeBlock;
import com.momosoftworks.momoweave.common.block.TradingPostBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockInit
{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Momoweave.MOD_ID);

    public static final RegistryObject<Block> GEODE = BLOCKS.register("geode", () -> new GeodeBlock(Block.Properties.of().strength(3.0F, 3.0F).sound(SoundType.STONE)));
    public static final RegistryObject<Block> TRADING_POST = BLOCKS.register("trading_post", () -> new TradingPostBlock(Block.Properties.of().strength(2.0F, 3.0F).sound(SoundType.WOOD)));
}
