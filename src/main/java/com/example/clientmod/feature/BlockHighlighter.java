package com.example.clientmod.feature;

import com.example.clientmod.module.ModuleManager;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexRendering;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Outlines the blocks listed in the module's settings that are near the player.
 * Uses Minecraft's own line renderer, so outlines are hidden behind solid blocks like normal.
 */
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
            if (id != null && Registries.BLOCK.containsId(id)) {
                blocks.add(Registries.BLOCK.get(id));
            }
        }
        return blocks;
    }

    /** Draws the outlines. Registered on the world render event (after entities). */
    public static void render(WorldRenderContext context) {
        List<BlockPos> toDraw = found;
        if (!ModuleManager.HIGHLIGHT.isEnabled() || toDraw.isEmpty()) {
            return;
        }
        MatrixStack matrices = context.matrices();
        VertexConsumerProvider consumers = context.consumers();
        if (matrices == null || consumers == null) {
            return;
        }

        // Coordinates sent to the renderer must be relative to the camera.
        Vec3d cam = context.worldState().cameraRenderState.pos;
        int argb = 0xFF000000 | ModuleManager.COLOR_RGB[ModuleManager.HL_COLOR.get()];

        VertexConsumer lines = consumers.getBuffer(RenderLayers.lines());
        VoxelShape cube = VoxelShapes.fullCube();
        for (BlockPos p : toDraw) {
            VertexRendering.drawOutline(matrices, lines, cube,
                    p.getX() - cam.x, p.getY() - cam.y, p.getZ() - cam.z, argb, 2.0f);
        }
    }
}
