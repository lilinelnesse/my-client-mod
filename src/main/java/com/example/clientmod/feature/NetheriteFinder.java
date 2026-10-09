package com.example.clientmod.feature;

import com.example.clientmod.module.ModuleManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Marks every loaded chunk that really contains ancient debris (netherite ore).
 * It only reports what is actually in the chunk data your game has received, so a marked chunk
 * always has debris in it. It cannot see chunks that are not loaded yet, and a server that hides
 * ores from the client (anti-xray) will hide them from this too.
 */
public final class NetheriteFinder {
    /** One marked chunk: its position, how much debris it has, and the height range of the debris. */
    public record Mark(int chunkX, int chunkZ, int count, int minY, int maxY) {}

    private static final int SCAN_INTERVAL_TICKS = 40;

    private static List<Mark> marks = new ArrayList<>();
    private static Mark nearest = null;
    private static int ticks = SCAN_INTERVAL_TICKS;

    private NetheriteFinder() {}

    public static Mark getNearest() {
        return ModuleManager.NETHERITE.isEnabled() ? nearest : null;
    }

    public static int getMarkCount() {
        return marks.size();
    }

    public static void tick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (!ModuleManager.NETHERITE.isEnabled() || world == null || client.player == null) {
            marks = new ArrayList<>();
            nearest = null;
            ticks = SCAN_INTERVAL_TICKS; // scan right away when turned on
            return;
        }
        if (++ticks < SCAN_INTERVAL_TICKS) {
            return;
        }
        ticks = 0;

        int range = ModuleManager.NF_RANGE.get();
        int minCount = ModuleManager.NF_MIN.get();
        BlockPos center = client.player.getBlockPos();
        int pcx = center.getX() >> 4;
        int pcz = center.getZ() >> 4;
        int bottomSection = world.getBottomSectionCoord();

        List<Mark> result = new ArrayList<>();
        for (int cx = pcx - range; cx <= pcx + range; cx++) {
            for (int cz = pcz - range; cz <= pcz + range; cz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) {
                    continue;
                }
                int count = 0;
                int minY = Integer.MAX_VALUE;
                int maxY = Integer.MIN_VALUE;
                ChunkSection[] sections = chunk.getSectionArray();
                for (int si = 0; si < sections.length; si++) {
                    ChunkSection section = sections[si];
                    if (section == null || section.isEmpty()
                            || !section.hasAny(state -> state.isOf(Blocks.ANCIENT_DEBRIS))) {
                        continue;
                    }
                    int baseY = (bottomSection + si) << 4;
                    for (int lx = 0; lx < 16; lx++) {
                        for (int lz = 0; lz < 16; lz++) {
                            for (int ly = 0; ly < 16; ly++) {
                                if (section.getBlockState(lx, ly, lz).isOf(Blocks.ANCIENT_DEBRIS)) {
                                    count++;
                                    minY = Math.min(minY, baseY + ly);
                                    maxY = Math.max(maxY, baseY + ly);
                                }
                            }
                        }
                    }
                }
                if (count >= minCount) {
                    result.add(new Mark(cx, cz, count, minY, maxY));
                }
            }
        }

        Mark best = null;
        double bestDist = Double.MAX_VALUE;
        for (Mark m : result) {
            double dx = (m.chunkX() * 16 + 8) - client.player.getX();
            double dz = (m.chunkZ() * 16 + 8) - client.player.getZ();
            double d = dx * dx + dz * dz;
            if (d < bestDist) {
                bestDist = d;
                best = m;
            }
        }
        marks = result;
        nearest = best;
    }

    /** Distance in blocks from the player to the middle of a marked chunk (flat, ignoring height). */
    public static int distanceTo(MinecraftClient client, Mark m) {
        double dx = (m.chunkX() * 16 + 8) - client.player.getX();
        double dz = (m.chunkZ() * 16 + 8) - client.player.getZ();
        return (int) Math.round(Math.sqrt(dx * dx + dz * dz));
    }

    /** Draws a box around the debris of every marked chunk. Registered on the world render event. */
    public static void render(WorldRenderContext context) {
        List<Mark> toDraw = marks;
        if (!ModuleManager.NETHERITE.isEnabled() || toDraw.isEmpty()) {
            return;
        }
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) {
            return;
        }

        int rgb = ModuleManager.COLOR_RGB[ModuleManager.NF_COLOR.get()];
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
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(2.0f);

        BufferBuilder buffer = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (Mark m : toDraw) {
            float x1 = m.chunkX() * 16f;
            float z1 = m.chunkZ() * 16f;
            float x2 = x1 + 16f;
            float z2 = z1 + 16f;
            float y1 = m.minY();
            float y2 = m.maxY() + 1f;
            line(buffer, matrix, x1, y1, z1, x2, y1, z1, r, g, b);
            line(buffer, matrix, x2, y1, z1, x2, y1, z2, r, g, b);
            line(buffer, matrix, x2, y1, z2, x1, y1, z2, r, g, b);
            line(buffer, matrix, x1, y1, z2, x1, y1, z1, r, g, b);
            line(buffer, matrix, x1, y2, z1, x2, y2, z1, r, g, b);
            line(buffer, matrix, x2, y2, z1, x2, y2, z2, r, g, b);
            line(buffer, matrix, x2, y2, z2, x1, y2, z2, r, g, b);
            line(buffer, matrix, x1, y2, z2, x1, y2, z1, r, g, b);
            line(buffer, matrix, x1, y1, z1, x1, y2, z1, r, g, b);
            line(buffer, matrix, x2, y1, z1, x2, y2, z1, r, g, b);
            line(buffer, matrix, x2, y1, z2, x2, y2, z2, r, g, b);
            line(buffer, matrix, x1, y1, z2, x1, y2, z2, r, g, b);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

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
