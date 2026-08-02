package io.github.cruciblemc.necrotempus.modules.mixin.mixins.angelica;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.gtnewhorizons.angelica.client.font.BatchingFontRenderer;
import com.gtnewhorizons.angelica.client.font.FontProvider;
import com.gtnewhorizons.angelica.client.font.FontStrategist;

import io.github.cruciblemc.necrotempus.NecroTempusConfig;
import io.github.cruciblemc.necrotempus.modules.features.glyphs.compat.angelica.FontProviderGlyph;

@Mixin(value = FontStrategist.class, remap = false)
public class FontStrategistMixin {

    @Inject(method = "getFontProvider", at = @At("HEAD"), cancellable = true, remap = false)
    private static void nt$getFontProvider(BatchingFontRenderer me, char chr, boolean customFontEnabled,
        boolean forceUnicode, CallbackInfoReturnable<FontProvider> cir) {

        if (!NecroTempusConfig.modernFonts) return;

        final FontProvider provider = FontProviderGlyph.INSTANCE;

        if (provider.isGlyphAvailable(chr)) {
            cir.setReturnValue(provider);
        }

    }

}
