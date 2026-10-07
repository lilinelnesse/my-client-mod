package com.example.clientmod.feature;

import com.example.clientmod.module.ModuleManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Outlines the blocks listed in the module's settings that are near the player. */
public final class BlockHighlighter {
    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int HARD_CAP = 20000;

    private static List<BlockPos> found = new ArrayList<>();
    private static Set<Block> targets = new HashSet<>();
    private static String parsedSpec = null;
    private static int ticks = 0;

    private BlockHighlighter() {}

    /** Called every client tick: rescans the area around the player about once a second. */
    public static void tick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (!ModuleManager.HIGHLIGHT.isEnabled() || world == null || client.player == null) {
            found = new ArrayList<>();
            ticks = SCAN_INTERVAL_TICKS; // scan right away when turned on
            return;
        }
        if (++ticks < SCAN_INTERVAL_TICKS) {
            return;
        }
        ticks = 0;

        String spec = ModuleManager.HL_BLOCKS.get();
        if (!spec.equals(parsedSpec)) {
            targets = parseBlocks(spec);
            parsedSpec = spec;
        }
        if (targets.isEmpty()) {
            found = new ArrayList<>();
            return;
        }

        int range = ModuleManager.HL_RANGE.get();
        BlockPos center = client.player.getBlockPos();
        int minY = Math.max(center.getY() - range, world.getBottomY());
        int maxY = Math.min(center.getY() + range, world.getBottomY() + world.getHeight() - 1);

        List<BlockPos> result = new ArrayList<>();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        scan:
        for (int x = center.getX() - range; x <= center.getX() + range; x++) {
            for (int z = center.getZ() - range; z <= center.getZ() + range; z++) {
                for (int y = minY; y <= maxY; y++) {
                    pos.set(x, y, z);
                    if (targets.contains(world.getBlockState(pos).getBlock())) {
                        result.add(pos.toImmutable());
                        if (result.size() >= HARD_CAP) {
                            break scan;
                        }
                    }
                }
            }
        }

        // Keep the closest ones if there are more than the "Max blocks" setting.
        int max = ModuleManager.HL_MAX.get();
        if (result.size() > max) {
            result.sort(Comparator.comparingDouble(p -> p.getSquaredDistance(center)));
            result = new ArrayList<>(result.subList(0, max));
        }
        found = result;
    }

    private static Set<Block> parseBlocks(String spec) {
        Set<Block> blocks = new HashSet<>();
        for (String part : spec.split(",")) {
            String name = part.trim().toLowerCase();
            if (name.isEmpty()) {
                continue;
            }
            Identifier id = Identifier.tryParse(name);
            if (id != null) {
                Registries.BLOCK.getOrEmpty(id).ifPresent(blocks::add);
            }
        }
        return blocks;
    }

    /** Draws the outlines. Registered on the world render event. */
    public static void render(WorldRenderContext context) {
        List<BlockPos> toDraw = found;
        if (!ModuleManager.HIGHLIGHT.isEnabled() || toDraw.isEmpty()) {
            return;
        }
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) {
            return;
        }

        int rgb = ModuleManager.COLOR_RGB[ModuleManager.HL_COLOR.get()];
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;

        Vec3d cam = context.camera().getPos();
        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        if (ModuleManager.HL_THROUGH_WALLS.get()) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.lineWidth(2.0f);

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (BlockPos p : toDraw) {
            addBox(buffer, matrix, p.getX(), p.getY(), p.getZ(), r, g, b);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0f);
        matrices.pop();
    }

    private static void addBox(BufferBuilder buf, Matrix4f m, float x, float y, float z,
                               float r, float g, float b) {
        float x2 = x + 1;
        float y2 = y + 1;
        float z2 = z + 1;
        // bottom square
        line(buf, m, x, y, z, x2, y, z, r, g, b);
        line(buf, m, x2, y, z, x2, y, z2, r, g, b);
        line(buf, m, x2, y, z2, x, y, z2, r, g, b);
        line(buf, m, x, y, z2, x, y, z, r, g, b);
        // top square
        line(buf, m, x, y2, z, x2, y2, z, r, g, b);
        line(buf, m, x2, y2, z, x2, y2, z2, r, g, b);
        line(buf, m, x2, y2, z2, x, y2, z2, r, g, b);
        line(buf, m, x, y2, z2, x, y2, z, r, g, b);
        // vertical edges
        line(buf, m, x, y, z, x, y2, z, r, g, b);
        line(buf, m, x2, y, z, x2, y2, z, r, g, b);
        line(buf, m, x2, y, z2, x2, y2, z2, r, g, b);
        line(buf, m, x, y, z2, x, y2, z2, r, g, b);
    }

    private static void line(BufferBuilder buf, Matrix4f m, float x1, float y1, float z1,
                             float x2, float y2, float z2, float r, float g, float b) {
        buf.vertex(m, x1, y1, z1).color(r, g, b, 1.0f);
        buf.vertex(m, x2, y2, z2).color(r, g, b, 1.0f);
    }
}
