package com.example.cheatmod;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldVertexBufferUploader;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Matrix4f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.client.Minecraft;

public class RenderUtils {
    public static void drawEntityBox(Entity entity, float red, float green, float blue, float alpha, MatrixStack matrixStack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getRenderViewEntity() == null) return;

        Vector3d cameraPos = mc.getRenderViewEntity().getEyePosition(1.0F);
        AxisAlignedBB bb = entity.getBoundingBox().offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(2.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(3, DefaultVertexFormats.POSITION_COLOR);

        Matrix4f matrix = matrixStack.getLast().getMatrix();
        drawBoxOutline(buffer, matrix, bb, red, green, blue, alpha);

        Tessellator.getInstance().draw();
        WorldVertexBufferUploader.draw(buffer);

        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public static void drawBlockBox(BlockPos pos, float red, float green, float blue, float alpha, MatrixStack matrixStack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getRenderViewEntity() == null) return;

        Vector3d cameraPos = mc.getRenderViewEntity().getEyePosition(1.0F);
        AxisAlignedBB bb = new AxisAlignedBB(pos).offset(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        RenderSystem.disableTexture();
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(2.0F);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(3, DefaultVertexFormats.POSITION_COLOR);

        Matrix4f matrix = matrixStack.getLast().getMatrix();
        drawBoxOutline(buffer, matrix, bb, red, green, blue, alpha);

        Tessellator.getInstance().draw();
        WorldVertexBufferUploader.draw(buffer);

        RenderSystem.enableDepthTest();
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    private static void drawBoxOutline(BufferBuilder buffer, Matrix4f matrix, AxisAlignedBB bb, float red, float green, float blue, float alpha) {
        buffer.pos(matrix, (float) bb.minX, (float) bb.minY, (float) bb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, (float) bb.minX, (float) bb.minY, (float) bb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, (float) bb.minX, (float) bb.minY, (float) bb.minZ).color(red, green, blue, alpha).endVertex();

        buffer.pos(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.minZ).color(red, green, blue, alpha).endVertex();

        buffer.pos(matrix, (float) bb.minX, (float) bb.minY, (float) bb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.minZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, (float) bb.maxX, (float) bb.minY, (float) bb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, (float) bb.maxX, (float) bb.maxY, (float) bb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, (float) bb.minX, (float) bb.minY, (float) bb.maxZ).color(red, green, blue, alpha).endVertex();
        buffer.pos(matrix, (float) bb.minX, (float) bb.maxY, (float) bb.maxZ).color(red, green, blue, alpha).endVertex();
    }
}
