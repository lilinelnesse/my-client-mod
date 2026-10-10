package com.example.clientmod.feature;

import com.example.clientmod.module.ModuleManager;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.math.Vec3d;

/**
 * Free camera: the view detaches from your player and flies around with WASD / Space / Shift,
 * while your player stands still (the movement, mine and place buttons are blocked while it is on).
 * Turn it off to snap back to your player.
 */
public final class Freecam {
    private static OtherClientPlayerEntity camera = null;
    private static float savedYaw, savedPitch;

    private Freecam() {}

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        boolean want = ModuleManager.FREECAM.isEnabled() && player != null && client.world != null;
        if (!want) {
            if (camera != null) {
                stop(client);
            }
            return;
        }
        if (camera == null) {
            start(client, player);
        }

        // read the movement keys straight from the keyboard, then stop the player from using them
        boolean free = client.currentScreen == null;
        boolean fwd = free && down(client, client.options.forwardKey);
        boolean back = free && down(client, client.options.backKey);
        boolean left = free && down(client, client.options.leftKey);
        boolean right = free && down(client, client.options.rightKey);
        boolean up = free && down(client, client.options.jumpKey);
        boolean downKey = free && down(client, client.options.sneakKey);
        boolean fast = free && down(client, client.options.sprintKey);

        client.options.forwardKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
        client.options.sneakKey.setPressed(false);
        client.options.sprintKey.setPressed(false);
        client.options.attackKey.setPressed(false);
        client.options.useKey.setPressed(false);

        double speed = ModuleManager.FC_SPEED.get() * 0.05; // blocks per tick
        if (fast) {
            speed *= ModuleManager.FC_SPRINT.get();
        }

        // the camera looks where the mouse looks
        float yaw = player.getYaw();
        float pitch = player.getPitch();
        Vec3d look = Vec3d.fromPolar(pitch, yaw);
        Vec3d side = Vec3d.fromPolar(0, yaw + 90);

        Vec3d move = Vec3d.ZERO;
        if (fwd) move = move.add(look);
        if (back) move = move.subtract(look);
        if (right) move = move.add(side);
        if (left) move = move.subtract(side);
        if (up) move = move.add(0, 1, 0);
        if (downKey) move = move.subtract(0, 1, 0);
        if (move.lengthSquared() > 0) {
            move = move.normalize().multiply(speed);
        }

        camera.prevX = camera.lastRenderX = camera.getX();
        camera.prevY = camera.lastRenderY = camera.getY();
        camera.prevZ = camera.lastRenderZ = camera.getZ();
        camera.prevYaw = camera.getYaw();
        camera.prevPitch = camera.getPitch();
        camera.setPosition(camera.getX() + move.x, camera.getY() + move.y, camera.getZ() + move.z);
        camera.setYaw(yaw);
        camera.setPitch(pitch);
        camera.setHeadYaw(yaw);
    }

    private static void start(MinecraftClient client, ClientPlayerEntity player) {
        camera = new OtherClientPlayerEntity(client.world, player.getGameProfile());
        camera.refreshPositionAndAngles(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch());
        camera.noClip = true;
        savedYaw = player.getYaw();
        savedPitch = player.getPitch();
        client.setCameraEntity(camera);
    }

    private static void stop(MinecraftClient client) {
        camera = null;
        if (client.player != null) {
            client.setCameraEntity(client.player);
            client.player.setYaw(savedYaw);
            client.player.setPitch(savedPitch);
        }
    }

    private static boolean down(MinecraftClient client, KeyBinding key) {
        InputUtil.Key bound = KeyBindingHelper.getBoundKeyOf(key);
        if (bound.getCategory() != InputUtil.Type.KEYSYM) {
            return false;
        }
        return InputUtil.isKeyPressed(client.getWindow().getHandle(), bound.getCode());
    }
}
