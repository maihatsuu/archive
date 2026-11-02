package maihatsuu.archive.registry;

import maihatsuu.archive.Archive;
import maihatsuu.archive.tome.SetTomeFunction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.storage.loot.functions.LootItemFunctionType;

public final class ModLootFunctions {
	public static final LootItemFunctionType<SetTomeFunction> SET_TOME = Registry.register(
			BuiltInRegistries.LOOT_FUNCTION_TYPE, Archive.id("set_tome"), new LootItemFunctionType<>(SetTomeFunction.CODEC));

	public static void init() {
	}

	private ModLootFunctions() {
	}
}
