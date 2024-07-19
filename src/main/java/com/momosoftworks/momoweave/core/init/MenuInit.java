package com.momosoftworks.momoweave.core.init;

import com.momosoftworks.coldsweat.common.container.BoilerContainer;
import com.momosoftworks.momoweave.Momoweave;
import com.momosoftworks.momoweave.common.container.LootTableMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class MenuInit
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Momoweave.MOD_ID);

    public static final RegistryObject<MenuType<LootTableMenu>> LOOT_MENU_TYPE =
            MENU_TYPES.register("loot_tables", () -> IForgeMenuType.create(LootTableMenu::new));
}
