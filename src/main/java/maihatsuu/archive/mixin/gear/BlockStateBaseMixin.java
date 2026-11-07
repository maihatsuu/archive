package maihatsuu.archive.mixin.gear;

import maihatsuu.archive.ArchiveConfig;
import maihatsuu.archive.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Block hardness: #archive:deepslate_hardness blocks lose {@code deepslateHardnessReduction} from their vanilla
 * hardness (never below 0), and #archive:obsidian_hardness blocks get {@code obsidianHardness}. Blast resistance is untouched.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
	@Inject(method = "getDestroySpeed", at = @At("RETURN"), cancellable = true)
	private void archive$hardness(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
		ArchiveConfig config = ArchiveConfig.get();
		if (!config.mining) {
			return;
		}
		BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase) (Object) this;
		float vanilla = cir.getReturnValueF();
		if (self.is(ModTags.DEEPSLATE_HARDNESS) && vanilla >= 0) {
			cir.setReturnValue(Math.max(0, vanilla - config.deepslateHardnessReduction));
		} else if (self.is(ModTags.OBSIDIAN_HARDNESS)) {
			cir.setReturnValue(config.obsidianHardness);
		}
	}
}
