package maihatsuu.archive.table;

import maihatsuu.archive.table.EnchantCosts.LevelCost;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.ToIntFunction;

/**
 * What the library table offers for an item. Shared by the client screen (for display) and the
 * server (to validate a purchase), so both always agree.
 */
public final class LibraryOptions {
	public enum State {
		AVAILABLE, TOO_EXPENSIVE, INCOMPATIBLE, MAXED,
		/** The next level needs more bookshelves or a tome. */
		LOCKED
	}

	/**
	 * @param conflict for {@link State#INCOMPATIBLE}, the enchant on the item that blocks this one
	 */
	public record Option(Holder<Enchantment> enchant, int currentLevel, int nextLevel, @Nullable LevelCost cost,
						 State state, @Nullable Holder<Enchantment> conflict) {
	}

	/**
	 * Options for {@code item}, given the enchants found on shelves. Enchants that can't go on the item at all are left out.
	 *
	 * @param cost        cost of buying (enchant, level)
	 * @param unlockedMax highest level of each enchant that can be bought here
	 */
	public static List<Option> compute(List<Holder<Enchantment>> available, ItemStack item, Player player, int lapis,
									   BiFunction<Holder<Enchantment>, Integer, LevelCost> cost, ToIntFunction<Holder<Enchantment>> unlockedMax) {
		List<Option> options = new ArrayList<>();
		if (item.isEmpty()) {
			return options;
		}
		ItemEnchantments existing = EnchantmentHelper.getEnchantmentsForCrafting(item);
		for (Holder<Enchantment> enchant : available) {
			if (!enchant.value().isSupportedItem(item)) {
				continue;
			}
			int current = existing.getLevel(enchant);
			int max = enchant.value().getMaxLevel();
			if (current >= max) {
				options.add(new Option(enchant, current, current, null, State.MAXED, null));
				continue;
			}
			int next = current + 1;
			LevelCost levelCost = cost.apply(enchant, next);
			Holder<Enchantment> conflict = current == 0 ? findConflict(enchant, existing) : null;
			State state;
			if (next > unlockedMax.applyAsInt(enchant)) {
				state = State.LOCKED;
			} else if (conflict != null) {
				state = State.INCOMPATIBLE;
			} else if (!player.hasInfiniteMaterials() && (player.experienceLevel < levelCost.xpLevels() || lapis < levelCost.lapis())) {
				state = State.TOO_EXPENSIVE;
			} else {
				state = State.AVAILABLE;
			}
			options.add(new Option(enchant, current, next, levelCost, state, conflict));
		}
		return options;
	}

	@Nullable
	private static Holder<Enchantment> findConflict(Holder<Enchantment> enchant, ItemEnchantments existing) {
		for (Holder<Enchantment> other : existing.keySet()) {
			if (!Enchantment.areCompatible(enchant, other)) {
				return other;
			}
		}
		return null;
	}

	private LibraryOptions() {
	}
}
