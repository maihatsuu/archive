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

/** Hardness for #archive:deepslate_hardness, #archive:deepslate_ore_hardness and #archive:obsidian_hardness. Blast resistance is untouched. */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {
	@Inject(method = "getDestroySpeed", at = @At("HEAD"), cancellable = true)
	private void archive$hardness(BlockGetter level, BlockPos pos, CallbackInfoReturnable<Float> cir) {
		ArchiveConfig config = ArchiveConfig.get();
		if (!config.mining) {
			return;
		}
		BlockBehaviour.BlockStateBase self = (BlockBehaviour.BlockStateBase) (Object) this;
		if (self.is(ModTags.DEEPSLATE_HARDNESS)) {
			cir.setReturnValue(config.deepslateHardness);
		} else if (self.is(ModTags.DEEPSLATE_ORE_HARDNESS)) {
			cir.setReturnValue(config.deepslateOreHardness);
		} else if (self.is(ModTags.OBSIDIAN_HARDNESS)) {
			cir.setReturnValue(config.obsidianHardness);
		}
	}
}
