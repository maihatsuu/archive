package maihatsuu.archive.client;

import maihatsuu.archive.Archive;
import maihatsuu.archive.registry.ModComponents;
import maihatsuu.archive.registry.ModItems;
import maihatsuu.archive.registry.ModMenus;
import maihatsuu.archive.tome.TomeItem;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;

public class ArchiveClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		TomeItem.clientOpener = stack -> Minecraft.getInstance().setScreen(new TomeScreen(stack));
		MenuScreens.register(ModMenus.LIBRARY_ENCHANT, LibraryEnchantScreen::new);
		// 1 for curse tomes, so the item model switches to the cursed texture.
		ItemProperties.register(ModItems.TOME, Archive.id("cursed"), (stack, level, entity, seed) -> {
			Holder<Enchantment> enchant = stack.get(ModComponents.TOME_ENCHANTMENT);
			return enchant != null && enchant.is(EnchantmentTags.CURSE) ? 1.0F : 0.0F;
		});
	}
}
