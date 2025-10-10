package com.momosoftworks.momoweave.client.gui;

import com.mojang.datafixers.util.Either;
import com.momosoftworks.coldsweat.compat.CompatManager;
import com.momosoftworks.coldsweat.util.math.CSMath;
import com.momosoftworks.coldsweat.util.serialization.NBTHelper;
import com.momosoftworks.momoweave.common.container.LootTableMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.reflect.Field;
import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class LootTableScreen extends AbstractContainerScreen<LootTableMenu>
{
    public static LootTablesSelectionScreen PARENT_SCREEN = null;
    private static final int SCROLL_SPEED = 32;

    public LootTableScreen(LootTableMenu screenContainer, Inventory inv, Component titleIn)
    {
        super(screenContainer, inv, titleIn);
        this.leftPos = 0;
        this.topPos = 0;
        this.inventoryLabelX = 0;
        this.titleLabelX = 0;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        this.inventoryLabelX = 0;
        this.titleLabelX = 0;
        this.titleLabelY = 0;
        this.renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderTooltip(graphics, mouseX, mouseY);
        int x = (this.width - this.getXSize()) / 2;
        int y = (this.height - this.getYSize()) / 2;
        for (Slot slot : this.getMenu().slots)
        {
            if (isSlotOutOfRange(slot))
            {   continue;
            }
            ItemStack stack = slot.getItem();
            LootTableMenu menu = this.getMenu();
            Integer lootCount = menu.getLootRolls().entrySet().stream().filter(entry -> ItemStack.isSameItemSameTags(entry.getKey(), stack)).map(entry -> entry.getValue()).findFirst().orElse(null);
            if (lootCount != null)
            {
                graphics.drawCenteredString(this.font, CSMath.formatDoubleOrInt(CSMath.truncate(lootCount / 100d, 2)),
                                    slot.x + 9 + x,
                                    slot.y + 17 + y,
                                    0xFFFFFF);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int pMouseX, int pMouseY)
    {
        graphics.drawString(this.font, this.getMenu().getLootTableName().toString(), this.titleLabelX, this.titleLabelY, 0xFFFFFF, true);
    }

    @Override
    protected void init()
    {
        super.init();
        this.titleLabelX = this.getXSize() / 2 - this.font.width(this.title) / 2;
        if (PARENT_SCREEN == null)
        {   PARENT_SCREEN = new LootTablesSelectionScreen(Component.literal("Loot Tables"));
        }
        this.addRenderableWidget(new Button.Builder(Component.literal("<"), (button) -> this.getMinecraft().setScreen(PARENT_SCREEN))
                               .pos(this.width / 2 - 10, this.height / 2 - 116)
                               .size(20, 20)
                               .build());
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY)
    {
    }

    static final Field SLOT_Y = ObfuscationReflectionHelper.findField(Slot.class, "f_40221_");
    static
    {
        SLOT_Y.setAccessible(true);
    }

    @SubscribeEvent
    public static void onMouseScroll(ScreenEvent.MouseScrolled.Pre event)
    {
        if (event.getScreen() instanceof LootTableScreen screen)
        {
            List<Slot> slots = screen.getMenu().slots;
            if (event.getScrollDelta() > 0)
            {
                Slot firstFilledSlot = slots.stream().filter(slot -> !slot.getItem().isEmpty()).findFirst().orElse(null);
                if (firstFilledSlot != null && firstFilledSlot.y < 0)
                {
                    for (Slot slot : slots)
                    {
                        try
                        {   SLOT_Y.set(slot, slot.y + SCROLL_SPEED);
                        }
                        catch (IllegalAccessException e)
                        {   e.printStackTrace();
                        }
                    }
                }
            }
            else
            {
                Slot lastFilledSlot = slots.stream().filter(slot -> !slot.getItem().isEmpty()).reduce((first, second) -> second).orElse(null);
                if (lastFilledSlot != null && lastFilledSlot.y > screen.height - screen.topPos - 32)
                {
                    for (Slot slot : slots)
                    {
                        try
                        {   SLOT_Y.set(slot, slot.y - SCROLL_SPEED);
                        }
                        catch (IllegalAccessException e)
                        {   e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    @Override
    public void renderSlot(GuiGraphics graphics, Slot slot)
    {
        if (isSlotOutOfRange(slot))
        {   return;
        }
        super.renderSlot(graphics, slot);
    }

    private boolean isSlotOutOfRange(Slot slot)
    {
        return slot.y > this.height - this.topPos - 32 || slot.y < 12;
    }

    @SubscribeEvent
    public static void renderEnchantmentTooltip(RenderTooltipEvent.GatherComponents event)
    {
        int index = CompatManager.LegendaryTooltips.getTooltipStartIndex(event.getTooltipElements());
        if (index == -1) index = 1;

        ItemStack stack = event.getItemStack();
        if (NBTHelper.getTagOrEmpty(stack).contains("momo:Enchanted"))
        {   event.getTooltipElements().add(index, Either.left(Component.literal("Random Enchantments").withStyle(ChatFormatting.LIGHT_PURPLE)));
        }
        if (NBTHelper.getTagOrEmpty(stack).contains("momo:Affixed"))
        {   event.getTooltipElements().add(index, Either.left(Component.literal("Random Affixes").withStyle(ChatFormatting.GOLD)));
        }
    }
}
