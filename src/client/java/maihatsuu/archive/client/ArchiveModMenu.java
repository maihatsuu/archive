package maihatsuu.archive.client;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Mod Menu entrypoint: only loaded when Mod Menu is installed. */
public final class ArchiveModMenu implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return ArchiveConfigScreen::create;
	}
}
