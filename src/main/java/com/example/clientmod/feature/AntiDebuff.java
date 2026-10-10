package com.example.clientmod.feature;

import com.example.clientmod.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffects;

/**
 * Removes the nausea and darkness effects from your own client so their screen distortion goes away.
 * This only changes what your game shows: the server still thinks you have the effect.
 */
public final class AntiDebuff {
    private AntiDebuff() {}

    public static void tick(MinecraftClient client) {
        if (!ModuleManager.ANTI_DEBUFF.isEnabled() || client.player == null) {
            return;
        }
        if (client.player.hasStatusEffect(StatusEffects.NAUSEA)) {
            client.player.removeStatusEffect(StatusEffects.NAUSEA);
        }
        if (client.player.hasStatusEffect(StatusEffects.DARKNESS)) {
            client.player.removeStatusEffect(StatusEffects.DARKNESS);
        }
    }
}
