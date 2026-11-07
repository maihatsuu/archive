package maihatsuu.archive;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Mod config, stored as JSON at {@code config/archive.json}.
 * <p>
 * Loaded lazily on first access, because some values (tool speed) are read while vanilla
 * classes initialise, which can happen before the mod initializer runs.
 * Values that affect client-side prediction (XP curve, mining speed, hardness) must match
 * between client and server.
 */
public final class ArchiveConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static ArchiveConfig instance;

	// Subsystem toggles
	public boolean xpCurve = true;
	public boolean durability = true;
	public boolean mining = true;
	public boolean anvil = true;
	public boolean grindstone = true;
	public boolean libraryTable = true;
	public boolean stripBookLoot = true;

	// Experience
	public int xpPerLevel = 50;
	public int levelCap = 100;
	public double deathXpFraction = 0.5;

	// Library enchanting table
	public int tableRadius = 4;
	/** Plain bookshelves needed for each level unlocked without a tome: {0, 5, 10} = I with none, II at 5, III at 10. */
	public int[] shelfSteps = {0, 5, 10};
	/** Show every enchant that fits the item, with locked rows and hints; false shows only what can be enchanted now. Display only. */
	public boolean tableShowLocked = true;

	// Mining
	// Tool speeds: wood, stone, iron and gold stay at vanilla's values unless changed.
	public float woodSpeed = 2.0F;
	public float stoneSpeed = 4.0F;
	public float ironSpeed = 6.0F;
	public float goldSpeed = 12.0F;
	public float diamondSpeed = 12.0F;
	public float netheriteSpeed = 19.0F;
	/** Subtracted from the vanilla hardness of every #archive:deepslate_hardness block (deepslate 3.0 → 2.0, ores 4.5 → 3.5). */
	public float deepslateHardnessReduction = 1.0F;
	public float obsidianHardness = 20.0F;

	/** Item id → max durability, on top of Archive's own values (set an item to its vanilla number to undo Archive's). Applied at startup; needs a restart to change. */
	public Map<String, Integer> durabilityOverrides = new LinkedHashMap<>();
	/** Item id → anvil repair material (an item id or #tag; "" for none), on top of Archive's defaults. Needs durability on. */
	public Map<String, String> repairOverrides = new LinkedHashMap<>();

	public static ArchiveConfig get() {
		if (instance == null) {
			instance = load();
		}
		return instance;
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve(Archive.MOD_ID + ".json");
	}

	/** Writes the current config to disk (used by the config screen). */
	public void save() {
		Path path = path();
		try {
			Files.createDirectories(path.getParent());
			try (Writer writer = Files.newBufferedWriter(path)) {
				GSON.toJson(this, writer);
			}
		} catch (IOException e) {
			Archive.LOGGER.error("Failed to write {}", path, e);
		}
	}

	private static ArchiveConfig load() {
		Path path = path();
		ArchiveConfig config = null;
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				config = GSON.fromJson(reader, ArchiveConfig.class);
			} catch (Exception e) {
				// Don't overwrite a file the user is editing; just run on defaults.
				Archive.LOGGER.error("Failed to read {}, using defaults", path, e);
				return new ArchiveConfig();
			}
		}
		if (config == null) {
			config = new ArchiveConfig();
		}
		// Write back so new keys appear with their defaults.
		config.save();
		return config;
	}
}
