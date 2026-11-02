package maihatsuu.archive.mixin.gear;

import maihatsuu.archive.ArchiveConfig;
import net.minecraft.world.item.Tiers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Diamond and netherite mining speed. The TOOL component is built from {@code getSpeed()}
 * when {@code Items} initialises, so changing the config needs a restart.
 */
@Mixin(Tiers.class)
public abstract class TiersMixin {
	@Inject(method = "getSpeed", at = @At("RETURN"), cancellable = true)
	private void archive$speed(CallbackInfoReturnable<Float> cir) {
		ArchiveConfig config = ArchiveConfig.get();
		if (!config.mining) {
			return;
		}
		Tiers self = (Tiers) (Object) this;
		if (self == Tiers.DIAMOND) {
			cir.setReturnValue(config.diamondSpeed);
		} else if (self == Tiers.NETHERITE) {
			cir.setReturnValue(config.netheriteSpeed);
		}
	}
}
