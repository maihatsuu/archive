package maihatsuu.archive;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * The server's whole config as JSON, sent to each joining client during configuration (before play, so EMI,
 * item stacks and the client's XP bar, hardness and repair materials see it from the start). The client uses it in place of its own
 * file until it disconnects; see {@link ArchiveConfig#applyServer}.
 */
public record ConfigSyncPayload(String json) implements CustomPacketPayload {
	public static final Type<ConfigSyncPayload> TYPE = new Type<>(Archive.id("config_sync"));
	public static final StreamCodec<ByteBuf, ConfigSyncPayload> STREAM_CODEC =
			ByteBufCodecs.stringUtf8(1 << 18).map(ConfigSyncPayload::new, ConfigSyncPayload::json);

	static void register() {
		PayloadTypeRegistry.configurationS2C().register(TYPE, STREAM_CODEC);
		ServerConfigurationConnectionEvents.CONFIGURE.register((handler, server) -> {
			if (ServerConfigurationNetworking.canSend(handler, TYPE)) {
				ServerConfigurationNetworking.send(handler, new ConfigSyncPayload(ArchiveConfig.local().toJson()));
			}
		});
	}

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}
}
