package com.momosoftworks.momoweave.event.common;

import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.entity.item.ItemEvent;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
public class ItemEntityDestroyedEvent extends ItemEvent
{
    public ItemEntityDestroyedEvent(ItemEntity entity)
    {   super(entity);
    }
}
