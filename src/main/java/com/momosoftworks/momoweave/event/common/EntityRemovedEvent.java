package com.momosoftworks.momoweave.event.common;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.item.ItemEvent;
import net.minecraftforge.eventbus.api.Cancelable;

@Cancelable
public class EntityRemovedEvent extends EntityEvent
{
    protected final Entity.RemovalReason reason;

    public EntityRemovedEvent(Entity entity, Entity.RemovalReason reason)
    {   super(entity);
        this.reason = reason;
    }

    public Entity.RemovalReason getReason()
    {   return reason;
    }
}
