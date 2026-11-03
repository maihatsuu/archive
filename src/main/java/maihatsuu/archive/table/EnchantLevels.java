package maihatsuu.archive.table;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import maihatsuu.archive.ArchiveConfig;
import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * An item's total non-curse enchant levels (Sharpness V + Sweeping Edge III = 8). The grindstone gives
 * that many XP levels back, and the anvil charges that many levels to repair the item.
 */
public final class EnchantLevels {
	public static boolean hasNonCurseEnchants(ItemStack stack) {
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
			if (!entry.getKey().is(EnchantmentTags.CURSE)) {
				return true;
			}
		}
		return false;
	}

	/** Sum of the stack's enchant levels, leaving out curses. */
	public static int total(ItemStack stack) {
		int levels = 0;
		for (Object2IntMap.Entry<Holder<Enchantment>> entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
			if (!entry.getKey().is(EnchantmentTags.CURSE)) {
				levels += entry.getIntValue();
			}
		}
		return levels;
	}

	/**
	 * XP points the grindstone gives back for disenchanting this stack: one level per enchant level.
	 * Exact with Archive's flat XP curve; orbs give points, so it's approximate with the curve off.
	 */
	public static int refundPoints(ItemStack stack) {
		return total(stack) * ArchiveConfig.get().xpPerLevel;
	}

	private EnchantLevels() {
	}
}
