package com.momosoftworks.momoweave.core.init;

import com.momosoftworks.momoweave.Momoweave;
import com.momosoftworks.momoweave.common.blockentity.GeodeBlockEntity;
import com.momosoftworks.momoweave.common.blockentity.TradingPostBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class BlockEntityInit
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Momoweave.MOD_ID);

    public static final RegistryObject<BlockEntityType<GeodeBlockEntity>> GEODE = BLOCK_ENTITIES.register("geode", () -> BlockEntityType.Builder.of(GeodeBlockEntity::new, BlockInit.GEODE.get()).build(null));
    public static final RegistryObject<BlockEntityType<TradingPostBlockEntity>> TRADING_POST = BLOCK_ENTITIES.register("trading_post", () -> BlockEntityType.Builder.of(TradingPostBlockEntity::new, BlockInit.TRADING_POST.get()).build(null));
}
