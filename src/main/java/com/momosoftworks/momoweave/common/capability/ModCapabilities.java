package com.momosoftworks.momoweave.common.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public class ModCapabilities
{
    public static final Capability<IBagCap> DEATH_POUCH_ITEMS = CapabilityManager.get(new CapabilityToken<>() {});
}
