package com.example.clientmod.mixin;

import com.example.clientmod.MyClientMod;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

// Example mixin: hooks into vanilla code. Use this pattern to change Minecraft behavior.
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "<init>", at = @At("TAIL"))
    private void myclientmod$onInit(CallbackInfo ci) {
        MyClientMod.LOGGER.info("MinecraftClient constructed (mixin hook works)");
    }
}
