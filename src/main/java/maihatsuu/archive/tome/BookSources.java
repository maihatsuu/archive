package maihatsuu.archive.tome;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/** Vanilla ways of getting enchanted books are removed; tomes replace them. */
public final class BookSources {
	public static boolean isEnchantedBook(ItemStack stack) {
		return stack.is(Items.ENCHANTED_BOOK)
				|| !stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).isEmpty();
	}

	private BookSources() {
	}
}
