package maihatsuu.archive.registry;

import maihatsuu.archive.Archive;
import maihatsuu.archive.table.LibraryEnchantMenu;
import maihatsuu.archive.table.LibraryEntry;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class ModMenus {
	public static final ExtendedScreenHandlerType<LibraryEnchantMenu, java.util.List<LibraryEntry>> LIBRARY_ENCHANT = Registry.register(
			BuiltInRegistries.MENU, Archive.id("library_enchant"),
			new ExtendedScreenHandlerType<>(LibraryEnchantMenu::new, LibraryEntry.LIST_STREAM_CODEC));

	public static void init() {
	}

	private ModMenus() {
	}
}
