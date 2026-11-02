package maihatsuu.archive.mixin.xp;

import maihatsuu.archive.ArchiveConfig;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** {@code /xp set ... levels} sets the level directly, bypassing giveExperienceLevels; cap it too. */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerXpMixin {
	@Inject(method = "setExperienceLevels", at = @At("TAIL"))
	private void archive$capSetLevels(int level, CallbackInfo ci) {
		ArchiveConfig config = ArchiveConfig.get();
		ServerPlayer self = (ServerPlayer) (Object) this;
		if (config.xpCurve && self.experienceLevel >= config.levelCap) {
			self.experienceLevel = config.levelCap;
			self.experienceProgress = 1.0F;
		}
	}
}
