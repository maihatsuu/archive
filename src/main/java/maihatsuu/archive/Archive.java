package maihatsuu.archive;

import maihatsuu.archive.registry.ModComponents;
import maihatsuu.archive.registry.ModItems;
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
		ModComponents.init();
		ModItems.init();
		GearComponents.register();
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
