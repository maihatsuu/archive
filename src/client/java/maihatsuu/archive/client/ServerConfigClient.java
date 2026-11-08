package maihatsuu.archive.client;

import maihatsuu.archive.ArchiveConfig;
import maihatsuu.archive.ConfigSyncPayload;
import maihatsuu.archive.GearComponents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientConfigurationNetworking;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

/**
 * Takes the server's config on joining a remote server and drops it on disconnect, rebuilding item durability and
 * tool speeds each time so they match whichever config is in effect. In singleplayer (and when hosting LAN) the
 * server is this game, so its own config already applies and stays editable live.
 */
final class ServerConfigClient {
	private static boolean usingServer;

	static void register() {
		ClientConfigurationNetworking.registerGlobalReceiver(ConfigSyncPayload.TYPE, (payload, context) -> {
			Minecraft client = Minecraft.getInstance();
			client.execute(() -> {
				if (!client.hasSingleplayerServer()) {
					ArchiveConfig.applyServer(payload.json());
					usingServer = true;
					GearComponents.rebuild(ArchiveConfig.get());
				}
			});
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ServerConfigClient::clear));
		ClientConfigurationConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ServerConfigClient::clear));
	}

	private static void clear() {
		if (usingServer) {
			usingServer = false;
			ArchiveConfig.applyServer(null);
			GearComponents.rebuild(ArchiveConfig.get());
		}
	}

	private ServerConfigClient() {
	}
}
