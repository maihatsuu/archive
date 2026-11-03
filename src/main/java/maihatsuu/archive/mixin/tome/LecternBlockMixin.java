package maihatsuu.archive.mixin.tome;

import maihatsuu.archive.tome.LecternTomeMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A lectern holding a tome opens the tome page instead of vanilla's (empty) book screen.
 * Not behind a config toggle: tomes always exist, and vanilla has no way to read one.
 */
@Mixin(LecternBlock.class)
public class LecternBlockMixin {
	@Inject(method = "openScreen", at = @At("HEAD"), cancellable = true)
	private void archive$openTome(Level level, BlockPos pos, Player player, CallbackInfo ci) {
		if (level.getBlockEntity(pos) instanceof LecternBlockEntity lectern && LecternTomeMenu.holdsTome(lectern)) {
			LecternTomeMenu.open(player, lectern);
			ci.cancel();
		}
	}
}
