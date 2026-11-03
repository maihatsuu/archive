package maihatsuu.archive.registry;

import maihatsuu.archive.Archive;
import maihatsuu.archive.table.LibraryEnchantMenu;
import maihatsuu.archive.table.LibraryEntry;
import maihatsuu.archive.tome.LecternTomeMenu;
import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class ModMenus {
	public static final ExtendedScreenHandlerType<LibraryEnchantMenu, java.util.List<LibraryEntry>> LIBRARY_ENCHANT = Registry.register(
			BuiltInRegistries.MENU, Archive.id("library_enchant"),
			new ExtendedScreenHandlerType<>(LibraryEnchantMenu::new, LibraryEntry.LIST_STREAM_CODEC));

	public static final ExtendedScreenHandlerType<LecternTomeMenu, ItemStack> LECTERN_TOME = Registry.register(
			BuiltInRegistries.MENU, Archive.id("lectern_tome"),
			new ExtendedScreenHandlerType<>(LecternTomeMenu::new, ItemStack.STREAM_CODEC));

	public static void init() {
	}

	private ModMenus() {
	}
}
