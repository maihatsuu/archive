package maihatsuu.archive.table;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.netty.buffer.ByteBuf;
import maihatsuu.archive.Archive;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.core.Holder;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.enchantment.Enchantment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fixed per-level enchanting costs, from {@code data/<namespace>/enchant_costs/*.json}:
 * <pre>
 * { "enchantment": "minecraft:sharpness",
 *   "levels": [ { "xp": 5, "lapis": 1 }, { "xp": 10, "lapis": 2 } ] }
 * </pre>
 * Levels past the end of the list, and enchants without a file, use {@link #defaultCost(int)}.
 */
public final class EnchantCosts {
	public record LevelCost(int xpLevels, int lapis) {
		public static final StreamCodec<ByteBuf, LevelCost> STREAM_CODEC = StreamCodec.composite(
				ByteBufCodecs.VAR_INT, LevelCost::xpLevels, ByteBufCodecs.VAR_INT, LevelCost::lapis, LevelCost::new);
	}

	private static Map<ResourceLocation, List<LevelCost>> table = Map.of();

	static void setTable(Map<ResourceLocation, List<LevelCost>> newTable) {
		table = Map.copyOf(newTable);
	}

	/** Placeholder formula until the cost table is filled in. */
	public static LevelCost defaultCost(int level) {
		return new LevelCost(5 * level, level);
	}

	/** Cost of buying {@code level} (1-based) of the given enchant. */
	public static LevelCost cost(Holder<Enchantment> enchant, int level) {
		List<LevelCost> levels = enchant.unwrapKey().map(key -> table.get(key.location())).orElse(null);
		if (levels != null && level >= 1 && level <= levels.size()) {
			return levels.get(level - 1);
		}
		return defaultCost(level);
	}

	/** Total XP levels paid for levels 1..{@code level}. */
	public static int totalXpLevels(Holder<Enchantment> enchant, int level) {
		int total = 0;
		for (int i = 1; i <= level; i++) {
			total += cost(enchant, i).xpLevels();
		}
		return total;
	}

	/** Costs of levels 1..max for the given enchant. */
	public static List<LevelCost> allLevels(Holder<Enchantment> enchant) {
		List<LevelCost> levels = new ArrayList<>();
		for (int level = 1; level <= enchant.value().getMaxLevel(); level++) {
			levels.add(cost(enchant, level));
		}
		return levels;
	}

	public static final class ReloadListener extends SimpleJsonResourceReloadListener implements IdentifiableResourceReloadListener {
		public ReloadListener() {
			super(new Gson(), "enchant_costs");
		}

		@Override
		public ResourceLocation getFabricId() {
			return Archive.id("enchant_costs");
		}

		@Override
		protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
			Map<ResourceLocation, List<LevelCost>> parsed = new HashMap<>();
			files.forEach((file, json) -> {
				try {
					JsonObject root = json.getAsJsonObject();
					ResourceLocation enchant = ResourceLocation.parse(root.get("enchantment").getAsString());
					List<LevelCost> levels = new ArrayList<>();
					for (JsonElement level : root.getAsJsonArray("levels")) {
						JsonObject cost = level.getAsJsonObject();
						levels.add(new LevelCost(cost.get("xp").getAsInt(), cost.get("lapis").getAsInt()));
					}
					parsed.put(enchant, List.copyOf(levels));
				} catch (Exception e) {
					Archive.LOGGER.error("Invalid enchant cost file {}", file, e);
				}
			});
			setTable(parsed);
			Archive.LOGGER.info("Loaded enchant costs for {} enchantments", parsed.size());
		}
	}

	private EnchantCosts() {
	}
}
