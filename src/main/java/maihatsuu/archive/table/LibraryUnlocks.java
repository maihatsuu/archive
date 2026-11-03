package maihatsuu.archive.table;

import maihatsuu.archive.ArchiveConfig;
import maihatsuu.archive.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * What an enchanting table offers, and up to which level. A shelved tome unlocks every level of its
 * enchant. Without one, plain bookshelves unlock enchants that have a {@code shelf_max}: one level for
 * each {@code shelfSteps} entry the bookshelf count reaches, up to {@code shelf_max}.
 * Used both when the table opens and when buying, so the two always agree.
 */
public final class LibraryUnlocks {
	public static List<LibraryEntry> compute(Level level, BlockPos pos) {
		LibraryScan.Result scan = LibraryScan.scan(level, pos, ArchiveConfig.get().tableRadius);
		int[] steps = ArchiveConfig.get().shelfSteps.clone();
		Arrays.sort(steps);
		int shelfLevel = shelfLevel(steps, scan.bookshelves());

		List<LibraryEntry> entries = new ArrayList<>();
		Set<Holder<Enchantment>> tomes = new HashSet<>(scan.tomes());
		for (Holder<Enchantment> enchant : scan.tomes()) {
			entries.add(new LibraryEntry(enchant, EnchantCosts.allLevels(enchant), enchant.value().getMaxLevel(), true, 0));
		}

		Registry<Enchantment> registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
		List<Holder<Enchantment>> shelfOnly = new ArrayList<>();
		for (var id : EnchantCosts.shelfEnchants()) {
			registry.getHolder(ResourceKey.create(Registries.ENCHANTMENT, id))
					.filter(enchant -> !tomes.contains(enchant) && ModItems.isTomeEnchant(enchant))
					.ifPresent(shelfOnly::add);
		}
		for (Holder<Enchantment> enchant : LibraryScan.sortByName(shelfOnly)) {
			// Bookshelves can never go past the last step, whatever shelf_max says.
			int cap = Math.min(Math.min(EnchantCosts.shelfMax(enchant), enchant.value().getMaxLevel()), steps.length);
			int unlocked = Math.min(cap, shelfLevel);
			if (unlocked < 1) {
				continue;
			}
			int shelvesForNext = unlocked < cap ? steps[unlocked] : 0;
			entries.add(new LibraryEntry(enchant, EnchantCosts.allLevels(enchant), unlocked, false, shelvesForNext));
		}
		return entries;
	}

	/** How many steps the bookshelf count reaches ({@code steps} sorted ascending). */
	static int shelfLevel(int[] steps, int bookshelves) {
		int reached = 0;
		while (reached < steps.length && bookshelves >= steps[reached]) {
			reached++;
		}
		return reached;
	}

	private LibraryUnlocks() {
	}
}
