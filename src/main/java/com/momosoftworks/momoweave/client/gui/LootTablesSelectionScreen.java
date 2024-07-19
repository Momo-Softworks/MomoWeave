package com.momosoftworks.momoweave.client.gui;

import com.momosoftworks.momoweave.core.network.MomoweavePacketHandler;
import com.momosoftworks.momoweave.core.network.message.RequestLootPageMessage;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(value = Dist.CLIENT)
public class LootTablesSelectionScreen extends Screen
{
    public static final List<ResourceLocation> ALL_LOOT_TABLES = new ArrayList<>();
    private static final int LOOT_TABLES_PER_PAGE = 16;
    private static final int LINES_PER_SCROLL = 4;
    private static int CURRENT_PAGE = 0;

    private int screenAge = 0;
    private final List<ResourceLocation> filteredLootTables = new ArrayList<>(ALL_LOOT_TABLES);

    public LootTablesSelectionScreen(Component titleIn)
    {   super(titleIn);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks)
    {
        super.render(graphics, mouseX, mouseY, partialTicks);
        this.renderBackground(graphics);
        for (int i = 0; i < LOOT_TABLES_PER_PAGE; i++)
        {
            int index = CURRENT_PAGE * LINES_PER_SCROLL + i;
            if (index >= filteredLootTables.size())
            {   break;
            }
            boolean selected = mouseX > 3
                            && mouseX < Minecraft.getInstance().font.width(filteredLootTables.get(index).toString()) + 3
                            && mouseY > i * (font.lineHeight + 8)
                            && mouseY < (i + 1) * (font.lineHeight + 8);
            MutableComponent text = Component.literal(filteredLootTables.get(index).toString());
            if (selected)
            {   text = text.withStyle(text.getStyle().withUnderlined(true).withColor(ChatFormatting.YELLOW));
            }
            graphics.drawString(this.font, text, 3, i * (font.lineHeight + 8) + 4, 0xFFFFFF, true);
        }
        screenAge++;
    }

    @Override
    protected void init()
    {
        super.init();
        this.addRenderableWidget(new EditBox(this.font, this.width - 180, 5, 160, 20, Component.literal(""))
        {
            private void fetchSearchResults()
            {
                CURRENT_PAGE = 0;
                filteredLootTables.clear();
                for (ResourceLocation location : ALL_LOOT_TABLES)
                {
                    if (location.toString().contains(this.getValue()))
                    {   filteredLootTables.add(location);
                    }
                }
            }

            @Override
            public boolean charTyped(char pCodePoint, int pModifiers)
            {
                boolean result = super.charTyped(pCodePoint, pModifiers);
                fetchSearchResults();
                return result;
            }

            @Override
            public void deleteChars(int pNum)
            {
                super.deleteChars(pNum);
                fetchSearchResults();
            }
        });
    }

    @SubscribeEvent
    public static void onMouseScroll(ScreenEvent.MouseScrolled.Pre event)
    {
        if (event.getScreen() instanceof LootTablesSelectionScreen screen)
        {
            if (event.getScrollDelta() > 0)
            {
                if (CURRENT_PAGE > 0)
                    CURRENT_PAGE--;
            }
            else
            {
                if (CURRENT_PAGE * LINES_PER_SCROLL < screen.filteredLootTables.size() - LOOT_TABLES_PER_PAGE)
                    CURRENT_PAGE++;
            }
        }
    }

    @SubscribeEvent
    public static void onMouseClicked(ScreenEvent.MouseButtonPressed event)
    {
        if (event.getScreen() instanceof LootTablesSelectionScreen screen)
        {
            if (screen.screenAge < 10)
            {   return;
            }
            int x = (int) event.getMouseX();
            int y = (int) event.getMouseY();
            int line = y / (Minecraft.getInstance().font.lineHeight + 8);
            int index = CURRENT_PAGE * LINES_PER_SCROLL + line;
            if (index < screen.filteredLootTables.size())
            {
                ResourceLocation lootTableName = screen.filteredLootTables.get(index);
                if (x > Minecraft.getInstance().font.width(lootTableName.toString()) + 3)
                {   return;
                }
                // Send a message to the server to open the loot table screen
                MomoweavePacketHandler.INSTANCE.sendToServer(new RequestLootPageMessage(lootTableName));
                LootTableScreen.PARENT_SCREEN = screen;
            }
        }
    }
}
