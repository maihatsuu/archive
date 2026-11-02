package maihatsuu.archive.table;

import maihatsuu.archive.registry.ModItems;
import maihatsuu.archive.tome.TomeItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Finds the tomes shelved in chiseled bookshelves around an enchanting table. */
public final class LibraryScan {
	public static List<Holder<Enchantment>> scan(Level level, BlockPos center, int radius) {
		Set<Holder<Enchantment>> found = new LinkedHashSet<>();
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius), center.offset(radius, radius, radius))) {
			if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof ChiseledBookShelfBlockEntity shelf)) {
				continue;
			}
			for (int slot = 0; slot < shelf.getContainerSize(); slot++) {
				Holder<Enchantment> enchant = TomeItem.getEnchant(shelf.getItem(slot));
				if (enchant != null && ModItems.isTomeEnchant(enchant)) {
					found.add(enchant);
				}
			}
		}
		return found.stream()
				.sorted(Comparator.comparing(enchant -> enchant.value().description().getString()))
				.toList();
	}

	private LibraryScan() {
	}
}
