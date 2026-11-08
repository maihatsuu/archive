package maihatsuu.archive;

import maihatsuu.archive.registry.ModComponents;
import maihatsuu.archive.registry.ModItems;
import maihatsuu.archive.registry.ModLootFunctions;
import maihatsuu.archive.registry.ModMenus;
import maihatsuu.archive.registry.ModNetworking;
import maihatsuu.archive.table.EnchantCosts;
import maihatsuu.archive.tome.LootTomes;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.server.packs.PackType;
import net.fabricmc.api.ModInitializer;

import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Archive implements ModInitializer {
	public static final String MOD_ID = "archive";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ArchiveConfig.get();
		ConfigCondition.register();
		ConfigSyncPayload.register();
		ModComponents.init();
		ModItems.init();
		ModLootFunctions.init();
		LootTomes.register();
		ModMenus.init();
		ModNetworking.init();
		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(new EnchantCosts.ReloadListener());
		GearComponents.register();
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
