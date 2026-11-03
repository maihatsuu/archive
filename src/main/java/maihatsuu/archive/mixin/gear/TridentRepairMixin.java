package maihatsuu.archive.mixin.gear;

import maihatsuu.archive.ArchiveConfig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prismarine shards repair the trident (vanilla gives it no repair ingredient). TridentItem doesn't
 * override isValidRepairItem, so this hooks Item's. The mace already repairs with breeze rods in vanilla.
 */
@Mixin(Item.class)
public abstract class TridentRepairMixin {
	@Inject(method = "isValidRepairItem", at = @At("HEAD"), cancellable = true)
	private void archive$tridentRepair(ItemStack stack, ItemStack repair, CallbackInfoReturnable<Boolean> cir) {
		if (ArchiveConfig.get().durability && (Object) this == Items.TRIDENT && repair.is(Items.PRISMARINE_SHARD)) {
			cir.setReturnValue(true);
		}
	}
}
