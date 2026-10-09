package com.example.clientmod.feature;

import com.example.clientmod.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Mines the blocks chosen in Block Highlight by itself: turns to face the nearest one, walks to it,
 * jumps over small steps, and mines whatever is in the way. Turn on Block Highlight too, because
 * that is what finds the blocks. It is a simple bot (no real path finding), so it can get stuck.
 */
public final class AutoMine {
    private static final double REACH = 4.2;
    private static final float MAX_TURN = 30.0f; // degrees per tick

    private static boolean holdingAttack = false;
    private static boolean holdingForward = false;
    private static boolean holdingJump = false;
    private static boolean holdingSprint = false;
    private static BlockPos target = null;

    private AutoMine() {}

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        boolean allowed = ModuleManager.AUTO_MINE.isEnabled()
                && player != null
                && client.world != null
                && client.currentScreen == null
                && player.getHealth() > ModuleManager.AM_MIN_HEALTH.get();
        if (!allowed) {
            releaseAll(client);
            target = null;
            return;
        }

        // keep the current target while it is still one of the chosen blocks, otherwise pick the nearest
        if (target == null || !isStillTarget(client, target)) {
            target = findNearest(client, player);
        }

        boolean attack = false;
        boolean forward = false;
        boolean jump = false;

        if (target != null) {
            Vec3d eye = player.getEyePos();
            Vec3d center = Vec3d.ofCenter(target);
            double dist = eye.distanceTo(center);

            if (ModuleManager.AM_LOOK.get()) {
                faceTowards(player, center.subtract(eye));
            }

            if (ModuleManager.AM_WALK.get() && dist > REACH - 0.6) {
                forward = true;
                if (player.horizontalCollision && player.isOnGround()) {
                    jump = true;
                }
            }

            // mine whatever the crosshair is on (the target itself, or a block that is in the way)
            if (client.crosshairTarget != null
                    && client.crosshairTarget.getType() == HitResult.Type.BLOCK
                    && client.crosshairTarget instanceof BlockHitResult hit
                    && eye.distanceTo(Vec3d.ofCenter(hit.getBlockPos())) <= REACH + 1.0) {
                attack = true;
            }
        } else if (ModuleManager.AM_LOOK.get() == false
                && client.crosshairTarget != null
                && client.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            attack = true; // nothing chosen: just keep mining what you look at
        }

        set(client, client.options.attackKey, attack, 0);
        set(client, client.options.forwardKey, forward, 1);
        set(client, client.options.jumpKey, jump, 2);
        set(client, client.options.sprintKey, forward, 3);
    }

    private static boolean isStillTarget(MinecraftClient client, BlockPos pos) {
        return BlockHighlighter.isTarget(client.world.getBlockState(pos).getBlock());
    }

    private static BlockPos findNearest(MinecraftClient client, ClientPlayerEntity player) {
        List<BlockPos> found = BlockHighlighter.getFound();
        BlockPos best = null;
        double bestDist = Double.MAX_VALUE;
        Vec3d eye = player.getEyePos();
        for (BlockPos p : found) {
            if (!isStillTarget(client, p)) {
                continue;
            }
            double d = eye.squaredDistanceTo(Vec3d.ofCenter(p));
            if (d < bestDist) {
                bestDist = d;
                best = p;
            }
        }
        return best;
    }

    /** Turns the view smoothly towards a direction. */
    private static void faceTowards(ClientPlayerEntity player, Vec3d dir) {
        double flat = Math.hypot(dir.x, dir.z);
        float wantYaw = (float) (MathHelper.atan2(dir.z, dir.x) * 180.0 / Math.PI) - 90.0f;
        float wantPitch = (float) -(MathHelper.atan2(dir.y, flat) * 180.0 / Math.PI);
        float dyaw = MathHelper.clamp(MathHelper.wrapDegrees(wantYaw - player.getYaw()), -MAX_TURN, MAX_TURN);
        float dpitch = MathHelper.clamp(wantPitch - player.getPitch(), -MAX_TURN, MAX_TURN);
        player.setYaw(player.getYaw() + dyaw);
        player.setPitch(MathHelper.clamp(player.getPitch() + dpitch, -90.0f, 90.0f));
    }

    private static void set(MinecraftClient client, net.minecraft.client.option.KeyBinding key, boolean on, int slot) {
        boolean was = switch (slot) {
            case 0 -> holdingAttack;
            case 1 -> holdingForward;
            case 2 -> holdingJump;
            default -> holdingSprint;
        };
        if (on) {
            key.setPressed(true);
        } else if (was) {
            key.setPressed(false);
        }
        switch (slot) {
            case 0 -> holdingAttack = on;
            case 1 -> holdingForward = on;
            case 2 -> holdingJump = on;
            default -> holdingSprint = on;
        }
    }

    private static void releaseAll(MinecraftClient client) {
        if (client.options == null) {
            return;
        }
        if (holdingAttack) client.options.attackKey.setPressed(false);
        if (holdingForward) client.options.forwardKey.setPressed(false);
        if (holdingJump) client.options.jumpKey.setPressed(false);
        if (holdingSprint) client.options.sprintKey.setPressed(false);
        holdingAttack = holdingForward = holdingJump = holdingSprint = false;
    }
}
