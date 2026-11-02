package maihatsuu.archive.table;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.List;
import java.util.Map;

/**
 * Fixed per-level enchanting costs. Enchants without an entry use {@link #defaultCost(int)}.
 */
public final class EnchantCosts {
	public record LevelCost(int xpLevels, int lapis) {
	}

	private static Map<ResourceLocation, List<LevelCost>> table = Map.of();

	static void setTable(Map<ResourceLocation, List<LevelCost>> newTable) {
		table = Map.copyOf(newTable);
	}

	/** Placeholder formula until the cost table is filled in. */
	public static LevelCost defaultCost(int level) {
		return new LevelCost(5 * level, level);
	}

	/** Cost of buying {@code level} (1-based) of the given enchant. */
	public static LevelCost cost(Holder<Enchantment> enchant, int level) {
		List<LevelCost> levels = enchant.unwrapKey().map(key -> table.get(key.location())).orElse(null);
		if (levels != null && level >= 1 && level <= levels.size()) {
			return levels.get(level - 1);
		}
		return defaultCost(level);
	}

	/** Total XP levels paid for levels 1..{@code level}. */
	public static int totalXpLevels(Holder<Enchantment> enchant, int level) {
		int total = 0;
		for (int i = 1; i <= level; i++) {
			total += cost(enchant, i).xpLevels();
		}
		return total;
	}

	private EnchantCosts() {
	}
}
