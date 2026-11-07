package maihatsuu.archive.mixin.gear;

import maihatsuu.archive.ArchiveConfig;
import net.minecraft.world.item.Tiers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Tool mining speed per tier (only diamond and netherite differ from vanilla by default). The TOOL component is built from {@code getSpeed()}
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
		cir.setReturnValue(switch ((Tiers) (Object) this) {
			case WOOD -> config.woodSpeed;
			case STONE -> config.stoneSpeed;
			case IRON -> config.ironSpeed;
			case GOLD -> config.goldSpeed;
			case DIAMOND -> config.diamondSpeed;
			case NETHERITE -> config.netheriteSpeed;
		});
	}
}
