package com.example.clientmod.mixin;

import com.example.clientmod.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.SimpleOption;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Fullbright: when the module is on, the game's brightness (gamma) option reads as 16 instead of 0-1. */
@Mixin(SimpleOption.class)
public class SimpleOptionMixin<T> {
    @SuppressWarnings("unchecked")
    @Inject(method = "getValue", at = @At("RETURN"), cancellable = true)
    private void myclientmod$fullbright(CallbackInfoReturnable<T> cir) {
        if (!ModuleManager.FULLBRIGHT.isEnabled()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return;
        }
        if ((Object) this == client.options.getGamma()) {
            cir.setReturnValue((T) Double.valueOf(16.0));
        }
    }
}
