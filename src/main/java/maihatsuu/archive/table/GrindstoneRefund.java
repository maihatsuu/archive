package maihatsuu.archive.table;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import maihatsuu.archive.ArchiveConfig;
import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/** XP refunded by the grindstone: a fraction of what the removed enchants cost at the library table. */
public final class GrindstoneRefund {
	public static boolean hasNonCurseEnchants(ItemStack stack) {
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
			if (!entry.getKey().is(EnchantmentTags.CURSE)) {
				return true;
			}
		}
		return false;
	}

	/** XP points refunded for disenchanting this stack. */
	public static int refundPoints(ItemStack stack) {
		ArchiveConfig config = ArchiveConfig.get();
		int levels = 0;
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
			if (!entry.getKey().is(EnchantmentTags.CURSE)) {
				levels += EnchantCosts.totalXpLevels(entry.getKey(), entry.getIntValue());
			}
		}
		return (int) Math.floor(levels * config.grindstoneRefundFraction * config.xpPerLevel);
	}

	private GrindstoneRefund() {
	}
}
