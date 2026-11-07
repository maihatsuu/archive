package maihatsuu.archive.mixin.gear;

import maihatsuu.archive.ArchiveConfig;
import maihatsuu.archive.RepairMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives gear without a vanilla repair ingredient one ({@link RepairMaterials}): string for bows,
 * crossbows and fishing rods, prismarine shards for the trident, and so on. None of those item classes
 * override isValidRepairItem, so this hooks Item's. The mace already repairs with breeze rods in vanilla.
 */
@Mixin(Item.class)
public abstract class GearRepairMixin {
	@Inject(method = "isValidRepairItem", at = @At("HEAD"), cancellable = true)
	private void archive$gearRepair(ItemStack stack, ItemStack repair, CallbackInfoReturnable<Boolean> cir) {
		if (!ArchiveConfig.get().durability) {
			return;
		}
		Item material = RepairMaterials.MAP.get((Object) this);
		if (material != null && repair.is(material)) {
			cir.setReturnValue(true);
		}
	}
}
