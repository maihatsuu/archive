package maihatsuu.archive.client;

import maihatsuu.archive.registry.ModMenus;
import maihatsuu.archive.tome.TomeItem;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;

public class ArchiveClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		TomeItem.clientOpener = stack -> Minecraft.getInstance().setScreen(new TomeScreen(stack));
		MenuScreens.register(ModMenus.LIBRARY_ENCHANT, LibraryEnchantScreen::new);
	}
}
