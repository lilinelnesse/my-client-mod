package com.example.clientmod.feature;

import com.example.clientmod.module.ModuleManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.block.AbstractFurnaceBlock;
import net.minecraft.block.BarrelBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BrewingStandBlock;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.ChiseledBookshelfBlock;
import net.minecraft.block.DecoratedPotBlock;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.EnderChestBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
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
import net.minecraft.world.chunk.WorldChunk;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Outlines the chosen storage blocks (chests, barrels, shulker boxes, ...) near the player. */
public final class StorageEsp {
    private static final int SCAN_INTERVAL_TICKS = 20;

    private static List<BlockPos> found = new ArrayList<>();
    private static Set<Block> targets = new HashSet<>();
    private static String parsedSpec = null;
    private static int ticks = SCAN_INTERVAL_TICKS;

    private StorageEsp() {}

    /** Only these kinds of blocks can be chosen in the Storage ESP search. */
    public static boolean isStorage(Block block) {
        return block instanceof ChestBlock
                || block instanceof EnderChestBlock
                || block instanceof BarrelBlock
                || block instanceof ShulkerBoxBlock
                || block instanceof HopperBlock
                || block instanceof DispenserBlock
                || block instanceof AbstractFurnaceBlock
                || block instanceof BrewingStandBlock
                || block instanceof DecoratedPotBlock
                || block instanceof ChiseledBookshelfBlock;
    }

    public static void tick(MinecraftClient client) {
        ClientWorld world = client.world;
        if (!ModuleManager.STORAGE_ESP.isEnabled() || world == null || client.player == null) {
            found = new ArrayList<>();
            ticks = SCAN_INTERVAL_TICKS;
            return;
        }
        if (++ticks < SCAN_INTERVAL_TICKS) {
            return;
        }
        ticks = 0;

        String spec = ModuleManager.SE_BLOCKS.get();
        if (!spec.equals(parsedSpec)) {
            targets = parse(spec);
            parsedSpec = spec;
        }
        if (targets.isEmpty()) {
            found = new ArrayList<>();
            return;
        }

        int range = ModuleManager.SE_RANGE.get();
        long rangeSq = (long) range * range;
        BlockPos center = client.player.getBlockPos();
        int minCx = (center.getX() - range) >> 4, maxCx = (center.getX() + range) >> 4;
        int minCz = (center.getZ() - range) >> 4, maxCz = (center.getZ() + range) >> 4;

        List<BlockPos> result = new ArrayList<>();
        for (int cx = minCx; cx <= maxCx; cx++) {
            for (int cz = minCz; cz <= maxCz; cz++) {
                WorldChunk chunk = world.getChunkManager().getWorldChunk(cx, cz);
                if (chunk == null) {
                    continue;
                }
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (!targets.contains(be.getCachedState().getBlock())) {
                        continue;
                    }
                    BlockPos p = be.getPos();
                    long dx = p.getX() - center.getX(), dy = p.getY() - center.getY(), dz = p.getZ() - center.getZ();
                    if (dx * dx + dy * dy + dz * dz <= rangeSq) {
                        result.add(p.toImmutable());
                    }
                }
            }
        }
        found = result;
    }

    private static Set<Block> parse(String spec) {
        Set<Block> blocks = new HashSet<>();
        for (String part : spec.split(",")) {
            String name = part.trim().toLowerCase();
            if (name.isEmpty()) {
                continue;
            }
            Identifier id = Identifier.tryParse(name);
            if (id != null && Registries.BLOCK.containsId(id)) {
                Block b = Registries.BLOCK.get(id);
                if (isStorage(b)) {
                    blocks.add(b);
                }
            }
        }
        return blocks;
    }

    public static void render(WorldRenderContext context) {
        List<BlockPos> toDraw = found;
        if (!ModuleManager.STORAGE_ESP.isEnabled() || toDraw.isEmpty()) {
            return;
        }
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) {
            return;
        }
        int rgb = ModuleManager.COLOR_RGB[ModuleManager.SE_COLOR.get()];
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
        if (ModuleManager.SE_THROUGH_WALLS.get()) {
            RenderSystem.disableDepthTest();
        } else {
            RenderSystem.enableDepthTest();
        }
        RenderSystem.lineWidth(2.0f);

        BufferBuilder buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (BlockPos p : toDraw) {
            float x = p.getX(), y = p.getY(), z = p.getZ();
            float x2 = x + 1, y2 = y + 1, z2 = z + 1;
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
