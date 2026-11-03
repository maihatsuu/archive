package maihatsuu.archive.table;

import maihatsuu.archive.registry.ModItems;
import maihatsuu.archive.tome.TomeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Looks around an enchanting table: the tomes shelved in chiseled bookshelves, and how many plain
 * bookshelves there are ({@code #minecraft:enchantment_power_provider}, which chiseled shelves aren't in).
 */
public final class LibraryScan {
	public record Result(List<Holder<Enchantment>> tomes, int bookshelves) {
	}

	public static Result scan(Level level, BlockPos center, int radius) {
		Set<Holder<Enchantment>> found = new LinkedHashSet<>();
		int bookshelves = 0;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
			if (!level.isLoaded(pos)) {
				continue;
			}
			if (level.getBlockState(pos).is(BlockTags.ENCHANTMENT_POWER_PROVIDER)) {
				bookshelves++;
				continue;
			}
			if (!(level.getBlockEntity(pos) instanceof ChiseledBookShelfBlockEntity shelf)) {
				continue;
			}
			for (int slot = 0; slot < shelf.getContainerSize(); slot++) {
				Holder<Enchantment> enchant = TomeItem.getEnchant(shelf.getItem(slot));
				if (enchant != null && ModItems.isTomeEnchant(enchant)) {
					found.add(enchant);
				}
			}
		}
		return new Result(sortByName(found), bookshelves);
	}

	static List<Holder<Enchantment>> sortByName(Iterable<Holder<Enchantment>> enchants) {
		List<Holder<Enchantment>> list = new ArrayList<>();
		enchants.forEach(list::add);
		list.sort(Comparator.comparing(enchant -> enchant.value().description().getString()));
		return List.copyOf(list);
	}

	private LibraryScan() {
	}
}
