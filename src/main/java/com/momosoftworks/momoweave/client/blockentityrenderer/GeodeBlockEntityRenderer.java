package com.momosoftworks.momoweave.client.blockentityrenderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.momosoftworks.momoweave.common.block.GeodeBlock;
import com.momosoftworks.momoweave.common.blockentity.GeodeBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class GeodeBlockEntityRenderer implements BlockEntityRenderer<GeodeBlockEntity>
{
    private static final Map<Direction, Float> SHADES_BY_DIRECTION = new EnumMap<>(Direction.class);
    static
    {
        SHADES_BY_DIRECTION.put(Direction.UP, 1f);
        SHADES_BY_DIRECTION.put(Direction.DOWN, 0.5f);
        SHADES_BY_DIRECTION.put(Direction.NORTH, 0.75f);
        SHADES_BY_DIRECTION.put(Direction.SOUTH, 0.75f);
        SHADES_BY_DIRECTION.put(Direction.EAST, 0.55f);
        SHADES_BY_DIRECTION.put(Direction.WEST, 0.55f);
    }

    public GeodeBlockEntityRenderer(BlockEntityRendererProvider.Context context)
    {}

    @Override
    public void render(GeodeBlockEntity blockEntity, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int combinedLight, int overlay)
    {
        SHADES_BY_DIRECTION.put(Direction.EAST, 0.55f);
        SHADES_BY_DIRECTION.put(Direction.NORTH, 0.75f);
        BlockPos pos = blockEntity.getBlockPos();
        Direction direction = blockEntity.getBlockState().getValue(GeodeBlock.FACING);
        Block ore = blockEntity.getOre();

        BakedModel referenceModel = Minecraft.getInstance().getBlockRenderer().getBlockModel(ore.defaultBlockState());
        List<BakedQuad> modelQuads = referenceModel.getQuads(ore.defaultBlockState(),Direction.UP, blockEntity.getLevel().random);
        if (modelQuads.isEmpty()) return;
        TextureAtlasSprite texture = modelQuads.get(modelQuads.size() - 1).getSprite();

        RenderType renderType = RenderType.cutout(); // or another appropriate RenderType
        VertexConsumer buffer = bufferSource.getBuffer(renderType);

        float width = 9/16f;
        float height = 5/16f;
        float length = 8/16f;
        poseStack.pushPose();
        poseStack.translate(4.97/16f, -0.001, 5.97/16f);
        poseStack.scale(1.01f, 1.01f, 1.01f);
        switch (direction)
        {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(0));
            case EAST  ->
            {
                poseStack.mulPose(Axis.YP.rotationDegrees(90));
                poseStack.translate(-8/16f, 0, -3/16f);
            }
            case SOUTH ->
            {
                poseStack.mulPose(Axis.YP.rotationDegrees(180));
                poseStack.translate(-6/16f, 0, -4/16f);
            }
            case WEST  ->
            {
                poseStack.mulPose(Axis.YP.rotationDegrees(270));
                poseStack.translate(-4/16f, 0, -9/16f);
            }
        }
        renderCube(poseStack, buffer, width, height, length, texture, pos, direction,
                   new int[]{0, 1,
                             5, 8,
                             7, 4,
                             8, 1,
                             7, 5,
                             0, 0});
        poseStack.popPose();
    }

    private void renderCube(PoseStack matrixStack, VertexConsumer builder, float width, float height, float depth, TextureAtlasSprite sprite, BlockPos pos, Direction rotation) {
        renderCube(matrixStack, builder, width, height, depth, sprite, pos, rotation,
                   new int[]{0,0, 0,0, 0,0, 0,0, 0,0, 0,0});
    }

    private void renderCube(PoseStack matrixStack, VertexConsumer builder, float width, float height, float depth,
                            TextureAtlasSprite sprite, BlockPos pos, Direction rotation, int[] uvOffsets)
    {
        Level level = Minecraft.getInstance().level;

        // Front face (SOUTH)
        renderFace(builder, matrixStack,
                   0, 0, depth,
                   width, 0, depth,
                   width, height, depth,
                   0, height, depth,
                   getPixelU(sprite, uvOffsets[0]), getPixelV(sprite, uvOffsets[1]),
                   getPixelU(sprite, uvOffsets[0] + width * 16), getPixelV(sprite, uvOffsets[1] + height * 16),
                   0, 0, 1, pos, level, SHADES_BY_DIRECTION.get(rotateBy(Direction.SOUTH, rotation)));

        // Back face (NORTH)
        renderFace(builder, matrixStack,
                   width, 0, 0,
                   0, 0, 0,
                   0, height, 0,
                   width, height, 0,
                   getPixelU(sprite, uvOffsets[2]), getPixelV(sprite, uvOffsets[3]),
                   getPixelU(sprite, uvOffsets[2] + width * 16), getPixelV(sprite, uvOffsets[3] + height * 16),
                   0, 0, -1, pos, level, SHADES_BY_DIRECTION.get(rotateBy(Direction.NORTH, rotation)));

        // Left face (WEST)
        renderFace(builder, matrixStack,
                   0, 0, 0,
                   0, 0, depth,
                   0, height, depth,
                   0, height, 0,
                   getPixelU(sprite, uvOffsets[4]), getPixelV(sprite, uvOffsets[5]),
                   getPixelU(sprite, uvOffsets[4] + depth * 16), getPixelV(sprite, uvOffsets[5] + height * 16),
                   -1, 0, 0, pos, level, SHADES_BY_DIRECTION.get(rotateBy(Direction.WEST, rotation)));

        // Right face (EAST)
        renderFace(builder, matrixStack,
                   width, 0, depth,
                   width, 0, 0,
                   width, height, 0,
                   width, height, depth,
                   getPixelU(sprite, uvOffsets[6]), getPixelV(sprite, uvOffsets[7]),
                   getPixelU(sprite, uvOffsets[6] + depth * 16), getPixelV(sprite, uvOffsets[7] + height * 16),
                   1, 0, 0, pos, level, SHADES_BY_DIRECTION.get(rotateBy(Direction.EAST, rotation)));

        // Top face
        renderFace(builder, matrixStack,
                   0, height, depth,
                   width, height, depth,
                   width, height, 0,
                   0, height, 0,
                   getPixelU(sprite, uvOffsets[8]), getPixelV(sprite, uvOffsets[9]),
                   getPixelU(sprite, uvOffsets[8] + width * 16), getPixelV(sprite, uvOffsets[9] + depth * 16),
                   0, 1, 0, pos, level, SHADES_BY_DIRECTION.get(Direction.UP));

        // Bottom face
        renderFace(builder, matrixStack,
                   0, 0, 0,
                   width, 0, 0,
                   width, 0, depth,
                   0, 0, depth,
                   getPixelU(sprite, uvOffsets[10]), getPixelV(sprite, uvOffsets[11]),
                   getPixelU(sprite, uvOffsets[10] + width * 16), getPixelV(sprite, uvOffsets[11] + depth * 16),
                   0, -1, 0, pos, level, SHADES_BY_DIRECTION.get(Direction.DOWN));
    }

    private float getPixelU(TextureAtlasSprite sprite, float pixelX) {
        return sprite.getU0() + (sprite.getU1() - sprite.getU0()) * (pixelX / 16f);
    }

    private float getPixelV(TextureAtlasSprite sprite, float pixelY) {
        return sprite.getV0() + (sprite.getV1() - sprite.getV0()) * (pixelY / 16f);
    }

    private void renderFace(VertexConsumer builder, PoseStack matrixStack,
                            float x1, float y1, float z1,
                            float x2, float y2, float z2,
                            float x3, float y3, float z3,
                            float x4, float y4, float z4,
                            float minU, float minV, float maxU, float maxV,
                            float nx, float ny, float nz, BlockPos lightPos, Level level, float shade) {
        int light = getLightLevel(level, lightPos);
        int r = (int)(255 * shade);
        int g = (int)(255 * shade);
        int b = (int)(255 * shade);
        vertex(builder, matrixStack, x1, y1, z1, minU, maxV, nx, ny, nz, light, r, g, b);
        vertex(builder, matrixStack, x2, y2, z2, maxU, maxV, nx, ny, nz, light, r, g, b);
        vertex(builder, matrixStack, x3, y3, z3, maxU, minV, nx, ny, nz, light, r, g, b);
        vertex(builder, matrixStack, x4, y4, z4, minU, minV, nx, ny, nz, light, r, g, b);
    }

    private void vertex(VertexConsumer builder, PoseStack matrixStack, float x, float y, float z, float u, float v, float nx, float ny, float nz, int light, int r, int g, int b) {
        PoseStack.Pose pose = matrixStack.last();
        builder.vertex(pose.pose(), x, y, z)
                .color(r, g, b, 255)
                .uv(u, v)
                .overlayCoords(0, 0)
                .uv2(light)
                .normal(pose.normal(), nx, ny, nz)
                .endVertex();
    }

    private int getLightLevel(Level level, BlockPos pos) {
        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        int skyLight = level.getBrightness(LightLayer.SKY, pos);
        return LightTexture.pack(blockLight, skyLight);
    }

    private Direction rotateBy(Direction direction, Direction relativeRotation)
    {
        Rotation rotationFromDirection = switch (relativeRotation)
        {
            default    -> Rotation.NONE;
            case EAST  -> Rotation.COUNTERCLOCKWISE_90;
            case SOUTH -> Rotation.CLOCKWISE_180;
            case WEST  -> Rotation.CLOCKWISE_90;
        };
        return rotationFromDirection.rotate(direction);
    }
}
