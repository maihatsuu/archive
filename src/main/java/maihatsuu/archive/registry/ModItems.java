package maihatsuu.archive.registry;

import maihatsuu.archive.Archive;
import maihatsuu.archive.ArchiveConfig;
import maihatsuu.archive.tome.TomeItem;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.Comparator;

public final class ModItems {
	public static final TomeItem TOME = Registry.register(BuiltInRegistries.ITEM, Archive.id("tome"),
			new TomeItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

	public static final CreativeModeTab TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Archive.id("archive"),
			FabricItemGroup.builder()
					.title(Component.translatable("itemGroup.archive"))
					.icon(() -> new ItemStack(TOME))
					.displayItems((parameters, output) -> parameters.holders().lookup(Registries.ENCHANTMENT).ifPresent(lookup ->
							lookup.listElements()
									.filter(ModItems::isTomeEnchant)
									.sorted(Comparator.comparing(holder -> holder.key().location().toString()))
									.forEach(holder -> output.accept(TomeItem.of(holder)))))
					.build());

	/** Enchants a tome may hold: not unobtainable, not a curse. */
	public static boolean isTomeEnchant(Holder<Enchantment> enchant) {
		return !enchant.is(ModTags.UNOBTAINABLE) && !enchant.is(EnchantmentTags.CURSE);
	}

	private static boolean hiddenInCreative(ItemStack stack) {
		return stack.is(Items.ENCHANTED_BOOK)
				|| (ArchiveConfig.get().anvil && (stack.is(Items.CHIPPED_ANVIL) || stack.is(Items.DAMAGED_ANVIL)));
	}

	public static void init() {
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
