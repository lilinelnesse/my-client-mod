package com.example.clientmod.module;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.block.Block;
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
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Finds chosen blocks near you and draws translucent boxes on them, visible through walls. */
public final class BlockHighlighter {
    private record Hit(BlockPos pos, int rgb) {}

    private static final int SCAN_INTERVAL_TICKS = 20;
    private static final int MAX_HITS = 1500;
    private static final Map<Block, Integer> COLORS = new HashMap<>();

    private static List<Hit> hits = List.of();
    private static int tickCounter = 0;

    static {
        COLORS.put(Blocks.DIAMOND_ORE, 0x33E6FF);
        COLORS.put(Blocks.DEEPSLATE_DIAMOND_ORE, 0x33E6FF);
        COLORS.put(Blocks.EMERALD_ORE, 0x33FF66);
        COLORS.put(Blocks.DEEPSLATE_EMERALD_ORE, 0x33FF66);
        COLORS.put(Blocks.GOLD_ORE, 0xFFD633);
        COLORS.put(Blocks.DEEPSLATE_GOLD_ORE, 0xFFD633);
        COLORS.put(Blocks.NETHER_GOLD_ORE, 0xFFD633);
        COLORS.put(Blocks.ANCIENT_DEBRIS, 0xC8743A);
        COLORS.put(Blocks.IRON_ORE, 0xD8AF93);
        COLORS.put(Blocks.DEEPSLATE_IRON_ORE, 0xD8AF93);
        COLORS.put(Blocks.COAL_ORE, 0x9A9A9A);
        COLORS.put(Blocks.DEEPSLATE_COAL_ORE, 0x9A9A9A);
        COLORS.put(Blocks.COPPER_ORE, 0xE77C56);
        COLORS.put(Blocks.DEEPSLATE_COPPER_ORE, 0xE77C56);
        COLORS.put(Blocks.REDSTONE_ORE, 0xFF2222);
        COLORS.put(Blocks.DEEPSLATE_REDSTONE_ORE, 0xFF2222);
        COLORS.put(Blocks.LAPIS_ORE, 0x2F5BFF);
        COLORS.put(Blocks.DEEPSLATE_LAPIS_ORE, 0x2F5BFF);
        COLORS.put(Blocks.NETHER_QUARTZ_ORE, 0xFFFFFF);
        COLORS.put(Blocks.CHEST, 0xFFAA00);
        COLORS.put(Blocks.TRAPPED_CHEST, 0xFF5500);
        COLORS.put(Blocks.BARREL, 0xAA7744);
        COLORS.put(Blocks.ENDER_CHEST, 0xAA33FF);
        COLORS.put(Blocks.SPAWNER, 0xFF33AA);
    }

    private BlockHighlighter() {}

    public static void init() {
        WorldRenderEvents.AFTER_TRANSLUCENT.register(BlockHighlighter::render);
    }

    /** Called every client tick. Rescans about once per second while the module is on. */
    public static void tick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (!ModuleManager.BLOCK_HIGHLIGHT.isEnabled() || world == null || client.player == null) {
            hits = List.of();
            tickCounter = SCAN_INTERVAL_TICKS; // scan right away next time it is turned on
            return;
        }
        tickCounter++;
        if (tickCounter < SCAN_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        Set<Block> targets = Settings.targets();
        if (targets.isEmpty()) {
            hits = List.of();
            return;
        }

        int r = Settings.highlightRange;
        BlockPos center = client.player.getBlockPos();
        int minY = Math.max(world.getBottomY(), center.getY() - r);
        int maxY = Math.min(world.getTopY() - 1, center.getY() + r);

        List<Hit> found = new ArrayList<>();
        BlockPos.Mutable pos = new BlockPos.Mutable();
        scan:
        for (int x = center.getX() - r; x <= center.getX() + r; x++) {
            for (int z = center.getZ() - r; z <= center.getZ() + r; z++) {
                for (int y = minY; y <= maxY; y++) {
                    pos.set(x, y, z);
                    Block block = world.getBlockState(pos).getBlock();
                    if (targets.contains(block)) {
                        found.add(new Hit(pos.toImmutable(), COLORS.getOrDefault(block, 0xFFFFFF)));
                        if (found.size() >= MAX_HITS) {
                            break scan;
                        }
                    }
                }
            }
        }
        hits = found;
    }

    private static void render(WorldRenderContext context) {
        if (!ModuleManager.BLOCK_HIGHLIGHT.isEnabled() || hits.isEmpty()) {
            return;
        }
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) {
            return;
        }

        Vec3d cam = context.camera().getPos();
        matrices.push();
        matrices.translate(-cam.x, -cam.y, -cam.z);
        Matrix4f matrix = matrices.peek().getPositionMatrix();

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        int alpha = Math.round(Settings.highlightOpacity * 2.55f);
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        for (Hit hit : hits) {
            addBox(buffer, matrix, hit.pos(), hit.rgb(), alpha);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private static void addBox(BufferBuilder b, Matrix4f m, BlockPos p, int rgb, int a) {
        float e = 0.002f;
        float x0 = p.getX() - e, y0 = p.getY() - e, z0 = p.getZ() - e;
        float x1 = p.getX() + 1 + e, y1 = p.getY() + 1 + e, z1 = p.getZ() + 1 + e;
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, bl = rgb & 0xFF;

        // down
        v(b, m, x0, y0, z0, r, g, bl, a); v(b, m, x1, y0, z0, r, g, bl, a);
        v(b, m, x1, y0, z1, r, g, bl, a); v(b, m, x0, y0, z1, r, g, bl, a);
        // up
        v(b, m, x0, y1, z0, r, g, bl, a); v(b, m, x0, y1, z1, r, g, bl, a);
        v(b, m, x1, y1, z1, r, g, bl, a); v(b, m, x1, y1, z0, r, g, bl, a);
        // north
        v(b, m, x0, y0, z0, r, g, bl, a); v(b, m, x0, y1, z0, r, g, bl, a);
        v(b, m, x1, y1, z0, r, g, bl, a); v(b, m, x1, y0, z0, r, g, bl, a);
        // south
        v(b, m, x0, y0, z1, r, g, bl, a); v(b, m, x1, y0, z1, r, g, bl, a);
        v(b, m, x1, y1, z1, r, g, bl, a); v(b, m, x0, y1, z1, r, g, bl, a);
        // west
        v(b, m, x0, y0, z0, r, g, bl, a); v(b, m, x0, y0, z1, r, g, bl, a);
        v(b, m, x0, y1, z1, r, g, bl, a); v(b, m, x0, y1, z0, r, g, bl, a);
        // east
        v(b, m, x1, y0, z0, r, g, bl, a); v(b, m, x1, y1, z0, r, g, bl, a);
        v(b, m, x1, y1, z1, r, g, bl, a); v(b, m, x1, y0, z1, r, g, bl, a);
    }

    private static void v(BufferBuilder b, Matrix4f m, float x, float y, float z, int r, int g, int bl, int a) {
        b.vertex(m, x, y, z).color(r, g, bl, a);
    }
}
