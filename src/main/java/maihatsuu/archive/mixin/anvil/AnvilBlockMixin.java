package maihatsuu.archive.mixin.anvil;

import maihatsuu.archive.ArchiveConfig;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Anvils never chip. {@code AnvilBlock.damage} is the single place both anvil use
 * ({@code AnvilMenu.onTake}) and falling ({@code FallingBlockEntity.causeFallDamage}) degrade an anvil.
 */
@Mixin(AnvilBlock.class)
public abstract class AnvilBlockMixin {
	@Inject(method = "damage", at = @At("HEAD"), cancellable = true)
	private static void archive$indestructible(BlockState state, CallbackInfoReturnable<BlockState> cir) {
		if (ArchiveConfig.get().anvil) {
			cir.setReturnValue(state);
		}
	}
}
