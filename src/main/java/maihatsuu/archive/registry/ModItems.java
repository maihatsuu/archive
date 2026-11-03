package maihatsuu.archive.registry;

import maihatsuu.archive.Archive;
import maihatsuu.archive.ArchiveConfig;
import maihatsuu.archive.tome.TomeItem;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Comparator;

public final class ModItems {
	public static final TomeItem TOME = Registry.register(BuiltInRegistries.ITEM, Archive.id("tome"),
			new TomeItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

	/** Enchants a tome may hold: anything not unobtainable, curses included (for minigames and the like). */
	public static boolean isTomeEnchant(Holder<Enchantment> enchant) {
		return !enchant.is(ModTags.UNOBTAINABLE);
	}

	private static boolean hiddenInCreative(ItemStack stack) {
		return stack.is(Items.ENCHANTED_BOOK)
				|| (ArchiveConfig.get().anvil && (stack.is(Items.CHIPPED_ANVIL) || stack.is(Items.DAMAGED_ANVIL)));
	}

	public static void init() {
		// One tome per obtainable enchant at the end of Ingredients, where vanilla lists enchanted books.
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.INGREDIENTS).register(entries ->
				entries.getContext().holders().lookup(Registries.ENCHANTMENT).ifPresent(lookup -> lookup.listElements()
						.filter(ModItems::isTomeEnchant)
						.sorted(Comparator.comparing(holder -> holder.key().location().toString()))
						.forEach(holder -> entries.accept(TomeItem.of(holder)))));
		// Hidden everywhere in creative: enchanted books (replaced by tomes) and chipped/damaged
		// anvils (anvils never degrade while the anvil rework is on).
		ItemGroupEvents.MODIFY_ENTRIES_ALL.register((group, entries) -> {
			entries.getDisplayStacks().removeIf(ModItems::hiddenInCreative);
			entries.getSearchTabStacks().removeIf(ModItems::hiddenInCreative);
		});
	}

	private ModItems() {
	}
}
