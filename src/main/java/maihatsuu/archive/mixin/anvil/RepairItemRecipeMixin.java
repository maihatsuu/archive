package maihatsuu.archive.mixin.anvil;

import maihatsuu.archive.ArchiveConfig;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Two damaged items of the same kind no longer combine in a crafting grid; repairs go through the anvil. */
@Mixin(RepairItemRecipe.class)
public abstract class RepairItemRecipeMixin {
	@Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
			at = @At("HEAD"), cancellable = true)
	private void archive$noCombining(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
		if (ArchiveConfig.get().anvil) {
			cir.setReturnValue(false);
		}
	}
}
