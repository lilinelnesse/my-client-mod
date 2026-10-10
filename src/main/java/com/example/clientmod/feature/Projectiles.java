package com.example.clientmod.feature;

import com.example.clientmod.module.ModuleManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.BowItem;
import net.minecraft.item.EggItem;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.LingeringPotionItem;
import net.minecraft.item.SnowballItem;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the path a bow arrow or a throwable (ender pearl, snowball, egg, potion, XP bottle) will take
 * from your hands, and a small square where it lands. It only looks at blocks, not at mobs or players.
 */
public final class Projectiles {
    private Projectiles() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientPlayerEntity player = client.player;
        if (!ModuleManager.PROJECTILES.isEnabled() || player == null || client.world == null) {
            return;
        }
        MatrixStack matrices = context.matrixStack();
        if (matrices == null) {
            return;
        }

        // speed and gravity per kind of item
        double speed;
        double gravity;
        ItemStack stack = player.getMainHandStack();
        Item item = stack.getItem();
        if (item instanceof BowItem && player.isUsingItem()) {
            float pull = BowItem.getPullProgress(player.getItemUseTime());
            if (pull < 0.1f) {
                return;
            }
            speed = pull * 3.0;
            gravity = 0.05;
        } else if (item instanceof EnderPearlItem || item instanceof SnowballItem || item instanceof EggItem) {
            speed = 1.5;
            gravity = 0.03;
        } else if (item instanceof SplashPotionItem || item instanceof LingeringPotionItem) {
            speed = 0.5;
            gravity = 0.05;
        } else if (item instanceof ExperienceBottleItem) {
            speed = 0.7;
            gravity = 0.07;
        } else {
            return;
        }

        // starting point and direction: from the eyes along where you look
        Vec3d look = Vec3d.fromPolar(player.getPitch(), player.getYaw());
        Vec3d pos = player.getEyePos().add(look.multiply(0.3)).add(0, -0.1, 0);
        Vec3d vel = look.multiply(speed);

        List<Vec3d> points = new ArrayList<>();
        points.add(pos);
        Vec3d landing = null;
        for (int i = 0; i < 300; i++) {
            Vec3d next = pos.add(vel);
            var hit = client.world.raycast(new RaycastContext(pos, next, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, player));
            if (hit.getType() != HitResult.Type.MISS) {
                landing = hit.getPos();
                points.add(landing);
                break;
            }
            pos = next;
            points.add(pos);
            vel = vel.multiply(0.99).add(0, -gravity, 0);
            if (pos.y < client.world.getBottomY() - 10) {
                break;
            }
        }

        int rgb = ModuleManager.COLOR_RGB[ModuleManager.PROJ_COLOR.get()];
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
        RenderSystem.disableDepthTest();
        RenderSystem.lineWidth(2.0f);

        BufferBuilder buf = Tessellator.getInstance()
                .begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
        for (int i = 1; i < points.size(); i++) {
            Vec3d a = points.get(i - 1);
            Vec3d c = points.get(i);
            line(buf, m, a, c, r, g, b);
        }
        if (landing != null) {
            double s = 0.25;
            Vec3d p = landing;
            line(buf, m, new Vec3d(p.x - s, p.y, p.z - s), new Vec3d(p.x + s, p.y, p.z - s), r, g, b);
            line(buf, m, new Vec3d(p.x + s, p.y, p.z - s), new Vec3d(p.x + s, p.y, p.z + s), r, g, b);
            line(buf, m, new Vec3d(p.x + s, p.y, p.z + s), new Vec3d(p.x - s, p.y, p.z + s), r, g, b);
            line(buf, m, new Vec3d(p.x - s, p.y, p.z + s), new Vec3d(p.x - s, p.y, p.z - s), r, g, b);
        }
        BufferRenderer.drawWithGlobalProgram(buf.end());

        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.lineWidth(1.0f);
        matrices.pop();
    }

    private static void line(BufferBuilder buf, Matrix4f m, Vec3d a, Vec3d c, float r, float g, float b) {
        buf.vertex(m, (float) a.x, (float) a.y, (float) a.z).color(r, g, b, 1.0f);
        buf.vertex(m, (float) c.x, (float) c.y, (float) c.z).color(r, g, b, 1.0f);
    }
}
