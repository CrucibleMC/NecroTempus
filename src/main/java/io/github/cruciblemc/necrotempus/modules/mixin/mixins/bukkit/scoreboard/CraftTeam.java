package io.github.cruciblemc.necrotempus.modules.mixin.mixins.bukkit.scoreboard;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Raises the Bukkit scoreboard team prefix/suffix length cap from 16 to 32 so longer prefixes/suffixes
 * can be set via the Bukkit API. Must stay in sync with the client read limit in
 * {@code S3EPacketTeamsMixin}.
 */
@Pseudo
@Mixin(targets = "org.bukkit.craftbukkit.v1_7_R4.scoreboard.CraftTeam", remap = false)
public abstract class CraftTeam {

    @ModifyConstant(method = "setPrefix", constant = @Constant(intValue = 16), remap = false)
    private int necro$extendPrefixLimit(int original) {
        return 32;
    }

    @ModifyConstant(method = "setSuffix", constant = @Constant(intValue = 16), remap = false)
    private int necro$extendSuffixLimit(int original) {
        return 32;
    }
}
