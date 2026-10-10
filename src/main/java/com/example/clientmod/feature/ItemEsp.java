package com.example.clientmod.feature;

import com.example.clientmod.module.ModuleManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.List;

/** Outlines dropped items near the player. */
public final class ItemEsp {
    private ItemEsp() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!ModuleManager.ITEM_ESP.isEnabled() || client.world == null || client.player == null) {
            return;
        }
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) {
            return;
        }
        double range = ModuleManager.IE_RANGE.get();
        List<ItemEntity> items = client.world.getEntitiesByClass(ItemEntity.class,
                client.player.getBoundingBox().expand(range), e -> true);
        if (items.isEmpty()) {
            return;
        }

        int rgb = ModuleManager.COLOR_RGB[ModuleManager.IE_COLOR.get()];
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;

        Vec3d cam = context.camera().getPos();
        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f m = matrices.peek().getPositionMatrix();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (ModuleManager.IE_THROUGH_WALLS.get()) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.lineWidth(2.0f);

        BufferBuilder buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (ItemEntity item : items) {
            Box bb = item.getBoundingBox();
            float x = (float) bb.minX, y = (float) bb.minY, z = (float) bb.minZ;
            float x2 = (float) bb.maxX, y2 = (float) bb.maxY, z2 = (float) bb.maxZ;
            line(buf, m, x, y, z, x2, y, z, r, g, b);
            line(buf, m, x2, y, z, x2, y, z2, r, g, b);
            line(buf, m, x2, y, z2, x, y, z2, r, g, b);
            line(buf, m, x, y, z2, x, y, z, r, g, b);
            line(buf, m, x, y2, z, x2, y2, z, r, g, b);
            line(buf, m, x2, y2, z, x2, y2, z2, r, g, b);
            line(buf, m, x2, y2, z2, x, y2, z2, r, g, b);
            line(buf, m, x, y2, z2, x, y2, z, r, g, b);
            line(buf, m, x, y, z, x, y2, z, r, g, b);
            line(buf, m, x2, y, z, x2, y2, z, r, g, b);
            line(buf, m, x2, y, z2, x2, y2, z2, r, g, b);
            line(buf, m, x, y, z2, x, y2, z2, r, g, b);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0f);
        matrices.pop();
    }

    private static void line(BufferBuilder buf, Matrix4f m, float x1, float y1, float z1,
                             float x2, float y2, float z2, float r, float g, float b) {
        buf.vertex(m, x1, y1, z1).color(r, g, b, 1.0f);
        buf.vertex(m, x2, y2, z2).color(r, g, b, 1.0f);
    }
}
