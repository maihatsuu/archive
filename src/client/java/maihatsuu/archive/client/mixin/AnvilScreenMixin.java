package maihatsuu.archive.client.mixin;

import maihatsuu.archive.ArchiveConfig;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** No "Too Expensive!" at 40+ levels: a repair just costs the item's enchant levels, however many. */
@Mixin(AnvilScreen.class)
public class AnvilScreenMixin {
	@ModifyConstant(method = "renderLabels", constant = @Constant(intValue = 40))
	private int archive$noCostCap(int cap) {
		return ArchiveConfig.get().anvil ? Integer.MAX_VALUE : cap;
	}
}
