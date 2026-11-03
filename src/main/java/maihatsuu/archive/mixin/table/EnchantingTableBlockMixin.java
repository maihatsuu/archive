package maihatsuu.archive.mixin.table;

import maihatsuu.archive.ArchiveConfig;
import maihatsuu.archive.table.LibraryMenuProvider;
import maihatsuu.archive.table.LibraryParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The enchanting table opens the library menu instead of the random vanilla one, and its particles
 * come from every shelf in the library's range instead of vanilla's bookshelf ring.
 */
@Mixin(EnchantingTableBlock.class)
public abstract class EnchantingTableBlockMixin {
	@Inject(method = "getMenuProvider", at = @At("HEAD"), cancellable = true)
	private void archive$libraryMenu(BlockState state, Level level, BlockPos pos, CallbackInfoReturnable<MenuProvider> cir) {
		if (ArchiveConfig.get().libraryTable && level.getBlockEntity(pos) instanceof EnchantingTableBlockEntity table) {
			cir.setReturnValue(new LibraryMenuProvider(level, pos, ((Nameable) table).getDisplayName()));
		}
	}

	// Block.animateTick (the skipped super call) does nothing.
	@Inject(method = "animateTick", at = @At("HEAD"), cancellable = true)
	private void archive$libraryParticles(BlockState state, Level level, BlockPos pos, RandomSource random, CallbackInfo ci) {
		if (ArchiveConfig.get().libraryTable) {
			LibraryParticles.animate(level, pos, random);
			ci.cancel();
		}
	}
}
