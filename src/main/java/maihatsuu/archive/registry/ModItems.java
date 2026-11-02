package maihatsuu.archive.registry;

import maihatsuu.archive.Archive;
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

	public static void init() {
		// Enchanted books are replaced by tomes; hide them everywhere in creative.
		ItemGroupEvents.MODIFY_ENTRIES_ALL.register((group, entries) -> {
			entries.getDisplayStacks().removeIf(stack -> stack.is(Items.ENCHANTED_BOOK));
			entries.getSearchTabStacks().removeIf(stack -> stack.is(Items.ENCHANTED_BOOK));
		});
	}

	private ModItems() {
	}
}
