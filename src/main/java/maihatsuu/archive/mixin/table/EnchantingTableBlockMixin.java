package maihatsuu.archive.mixin.table;

import maihatsuu.archive.ArchiveConfig;
import maihatsuu.archive.table.LibraryMenuProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.Nameable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EnchantingTableBlock;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** The enchanting table opens the library menu instead of the random vanilla one. */
@Mixin(EnchantingTableBlock.class)
public abstract class EnchantingTableBlockMixin {
	@Inject(method = "getMenuProvider", at = @At("HEAD"), cancellable = true)
	private void archive$libraryMenu(BlockState state, Level level, BlockPos pos, CallbackInfoReturnable<MenuProvider> cir) {
		if (ArchiveConfig.get().libraryTable && level.getBlockEntity(pos) instanceof EnchantingTableBlockEntity table) {
			cir.setReturnValue(new LibraryMenuProvider(level, pos, ((Nameable) table).getDisplayName()));
		}
	}
}
