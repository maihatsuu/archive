package maihatsuu.archive.client;

import maihatsuu.archive.tome.TomeItem;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

public class ArchiveClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		TomeItem.clientOpener = stack -> Minecraft.getInstance().setScreen(new TomeScreen(stack));
	}
}
