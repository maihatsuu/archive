package maihatsuu.archive.mixin.xp;

import maihatsuu.archive.ArchiveConfig;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** A Bottle o' Enchanting gives exactly one level's worth of XP. */
@Mixin(ThrownExperienceBottle.class)
public abstract class ThrownExperienceBottleMixin {
	@ModifyArg(method = "onHit", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"),
			index = 2)
	private int archive$fixedAmount(int amount) {
		ArchiveConfig config = ArchiveConfig.get();
		return config.xpCurve ? config.xpPerLevel : amount;
	}
}
