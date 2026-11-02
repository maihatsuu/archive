package maihatsuu.archive.mixin.xp;

import maihatsuu.archive.ArchiveConfig;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Flat XP curve, level cap, and death XP as a fraction of total XP. */
@Mixin(Player.class)
public abstract class PlayerXpMixin {
	@Inject(method = "getXpNeededForNextLevel", at = @At("HEAD"), cancellable = true)
	private void archive$flatCurve(CallbackInfoReturnable<Integer> cir) {
		ArchiveConfig config = ArchiveConfig.get();
		if (config.xpCurve) {
			cir.setReturnValue(config.xpPerLevel);
		}
	}

	@Inject(method = "giveExperiencePoints", at = @At("TAIL"))
	private void archive$capAfterPoints(int points, CallbackInfo ci) {
		archive$clampToCap();
	}

	@Inject(method = "giveExperienceLevels", at = @At("TAIL"))
	private void archive$capAfterLevels(int levels, CallbackInfo ci) {
		archive$clampToCap();
	}

	/** At the cap the level stays put, the bar stays full, and extra XP is discarded. */
	private void archive$clampToCap() {
		ArchiveConfig config = ArchiveConfig.get();
		if (!config.xpCurve) {
			return;
		}
		Player self = (Player) (Object) this;
		if (self.experienceLevel >= config.levelCap) {
			self.experienceLevel = config.levelCap;
			self.experienceProgress = 1.0F;
			self.totalExperience = Math.min(self.totalExperience, config.levelCap * config.xpPerLevel);
		}
	}

	@Inject(method = "getBaseExperienceReward", at = @At("HEAD"), cancellable = true)
	private void archive$deathXp(CallbackInfoReturnable<Integer> cir) {
		ArchiveConfig config = ArchiveConfig.get();
		if (!config.xpCurve) {
			return;
		}
		Player self = (Player) (Object) this;
		if (self.level().getGameRules().getBoolean(GameRules.RULE_KEEPINVENTORY) || self.isSpectator()) {
			cir.setReturnValue(0);
			return;
		}
		double totalPoints = (self.experienceLevel + self.experienceProgress) * config.xpPerLevel;
		cir.setReturnValue((int) Math.floor(totalPoints * config.deathXpFraction));
	}
}
