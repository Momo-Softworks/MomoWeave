package com.momosoftworks.momoweave.client.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.momosoftworks.momoweave.common.blockentity.GeodeBlockEntity;
import com.momosoftworks.momoweave.core.init.BlockInit;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.Tags;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.lang.reflect.Method;
import java.util.List;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class RenderGeodeTooltip
{
    static final Method GET_FOV = ObfuscationReflectionHelper.findMethod(GameRenderer.class, "m_109141_",
                                                                         Camera.class, float.class, boolean.class);
    static final Method BOB_VIEW = ObfuscationReflectionHelper.findMethod(GameRenderer.class, "m_109138_",
                                                                          PoseStack.class, float.class);
    static
    {   GET_FOV.setAccessible(true);
        BOB_VIEW.setAccessible(true);
    }

    @SubscribeEvent
    public static void onHoverGeode(RenderGuiOverlayEvent.Post event)
    {
        if (event.getOverlay() == VanillaGuiOverlay.CROSSHAIR.type()
        && Minecraft.getInstance().hitResult instanceof BlockHitResult blockHitResult
        && Minecraft.getInstance().level.getBlockState(blockHitResult.getBlockPos()).getBlock() == BlockInit.GEODE.get()
        && !Minecraft.getInstance().isPaused())
        {
            GeodeBlockEntity geode = (GeodeBlockEntity) Minecraft.getInstance().level.getBlockEntity(blockHitResult.getBlockPos());
            if (geode != null)
            {
                Block ore = geode.getOre();
                String blockName = "?";

                StatsCounter stats = Minecraft.getInstance().player.getStats();
                if (stats.getValue(Stats.BLOCK_MINED, ore) > 0 || stats.getValue(Stats.BLOCK_MINED, getVariant(ore)) > 0)
                {   blockName = I18n.get(ore.getDescriptionId());
                }
                List<FormattedCharSequence> tooltip = List.of(Component.literal("Geode").withStyle(ChatFormatting.YELLOW).getVisualOrderText(),
                                                              Component.literal(blockName).withStyle(ChatFormatting.GRAY).getVisualOrderText());
                Vec3 screenPos = getScreenPosition(blockHitResult.getBlockPos().getCenter(), Minecraft.getInstance().gameRenderer.getMainCamera());
                event.getGuiGraphics().renderTooltip(Minecraft.getInstance().font, tooltip, (int) screenPos.x, (int) screenPos.y);
            }
        }
    }

    private static Block getVariant(Block block)
    {
        if (block.defaultBlockState().is(Tags.Blocks.ORES_IN_GROUND_DEEPSLATE))
        {
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
            ResourceLocation altId = new ResourceLocation(id.getNamespace(), id.getPath().replace("deepslate_", "").replace("_deepslate", ""));
            return ForgeRegistries.BLOCKS.getValue(altId);
        }
        else if (block.defaultBlockState().is(Tags.Blocks.ORES_IN_GROUND_STONE))
        {
            ResourceLocation id = ForgeRegistries.BLOCKS.getKey(block);
            ResourceLocation altId = new ResourceLocation(id.getNamespace(), "deepslate_" + id.getPath());
            Block altBlock = ForgeRegistries.BLOCKS.getValue(altId);
            if (altBlock == null)
            {
                altId = new ResourceLocation(id.getNamespace(), id.getPath() + "_deepslate");
                altBlock = ForgeRegistries.BLOCKS.getValue(altId);
            }
            return altBlock;
        }
        return block;
    }

    public static Vec3 getScreenPosition(Vec3 objectPosition, Camera camera)
    {
        Minecraft mc = Minecraft.getInstance();

        // Get the view matrix
        PoseStack poseStack = new PoseStack();
        //poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(camera.getXRot()));
        poseStack.mulPose(Axis.YP.rotationDegrees(-camera.getYRot()));
        // Apply view bobbing transformations
        if (Minecraft.getInstance().options.bobView().get())
        {
            try
            {   PoseStack bobStack = new PoseStack();
                BOB_VIEW.invoke(mc.gameRenderer, bobStack, mc.getFrameTime());
                //mul the poseStack by the bobStack's y position, and negative of the x position
                poseStack.translate(0, bobStack.last().pose().get(3, 1), 0);
            }
            catch (Exception ignored) {}
        }
        Matrix4f viewMatrix = poseStack.last().pose();

        // Get the projection matrix
        float fov;
        try
        {   fov = (float)(double)GET_FOV.invoke(mc.gameRenderer, camera, mc.getFrameTime(), true);
        }
        catch (Exception e)
        {   fov = 70.0F;
        }

        float aspectRatio = (float)mc.getWindow().getGuiScaledWidth() / (float)mc.getWindow().getGuiScaledHeight();
        float nearPlane = 0.05F;
        float farPlane = mc.options.renderDistance().get() * 16.0F;
        Matrix4f projectionMatrix = new Matrix4f().perspective(fov * ((float)Math.PI / 180F), aspectRatio, nearPlane, farPlane);

        // Combine view and projection matrices
        Matrix4f viewProjectionMatrix = new Matrix4f(projectionMatrix);
        viewProjectionMatrix.mul(viewMatrix);

        // Transform the world position
        Vec3 cameraPos = camera.getPosition();
        Vector4f pos = new Vector4f(
                (float)(objectPosition.x - cameraPos.x),
                (float)(objectPosition.y - cameraPos.y),
                (float)-(objectPosition.z - cameraPos.z),
                1.0F
        );
        viewProjectionMatrix.transform(pos);

        // Perform perspective division
        if (pos.w > 0.0)
        {
            pos.x /= pos.w;
            pos.y /= pos.w;
            pos.z /= pos.w;
        }

        // Convert to screen coordinates
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();
        double screenX = (1.0 - pos.x) / 2.0 * screenWidth;
        double screenY = (1.0 - pos.y) / 2.0 * screenHeight;

        return new Vec3(screenX, screenY, pos.z);
    }
}
