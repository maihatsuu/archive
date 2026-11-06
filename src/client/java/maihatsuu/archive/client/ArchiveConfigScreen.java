package maihatsuu.archive.client;

import maihatsuu.archive.ArchiveConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Cloth Config screen for {@link ArchiveConfig}. Edits the live config instance, so settings read at use time
 * apply at once in singleplayer; durability and tool speed are baked in at startup and are marked restart-only.
 * {@code durabilityOverrides} stays JSON-only.
 */
public final class ArchiveConfigScreen {
	private ArchiveConfigScreen() {
	}

	public static Screen create(Screen parent) {
		ArchiveConfig config = ArchiveConfig.get();
		ArchiveConfig defaults = new ArchiveConfig();

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable("config.archive.title"))
				.setSavingRunnable(config::save);
		ConfigEntryBuilder entries = builder.entryBuilder();

		ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.archive.category.general"));
		general.addEntry(entries.startBooleanToggle(name("xpCurve"), config.xpCurve)
				.setDefaultValue(defaults.xpCurve).setTooltip(tooltip("xpCurve"))
				.setSaveConsumer(v -> config.xpCurve = v).build());
		general.addEntry(entries.startBooleanToggle(name("durability"), config.durability)
				.setDefaultValue(defaults.durability).setTooltip(tooltip("durability"))
				.requireRestart()
				.setSaveConsumer(v -> config.durability = v).build());
		general.addEntry(entries.startBooleanToggle(name("mining"), config.mining)
				.setDefaultValue(defaults.mining).setTooltip(tooltip("mining"))
				.requireRestart()
				.setSaveConsumer(v -> config.mining = v).build());
		general.addEntry(entries.startBooleanToggle(name("anvil"), config.anvil)
				.setDefaultValue(defaults.anvil).setTooltip(tooltip("anvil"))
				.setSaveConsumer(v -> config.anvil = v).build());
		general.addEntry(entries.startBooleanToggle(name("grindstone"), config.grindstone)
				.setDefaultValue(defaults.grindstone).setTooltip(tooltip("grindstone"))
				.setSaveConsumer(v -> config.grindstone = v).build());
		general.addEntry(entries.startBooleanToggle(name("libraryTable"), config.libraryTable)
				.setDefaultValue(defaults.libraryTable).setTooltip(tooltip("libraryTable"))
				.setSaveConsumer(v -> config.libraryTable = v).build());
		general.addEntry(entries.startBooleanToggle(name("stripBookLoot"), config.stripBookLoot)
				.setDefaultValue(defaults.stripBookLoot).setTooltip(tooltip("stripBookLoot"))
				.setSaveConsumer(v -> config.stripBookLoot = v).build());
		general.addEntry(entries.startTextDescription(Component.translatable("config.archive.durabilityOverrides")).build());

		ConfigCategory xp = builder.getOrCreateCategory(Component.translatable("config.archive.category.experience"));
		xp.addEntry(entries.startIntField(name("xpPerLevel"), config.xpPerLevel)
				.setDefaultValue(defaults.xpPerLevel).setMin(1).setTooltip(tooltip("xpPerLevel"))
				.setSaveConsumer(v -> config.xpPerLevel = v).build());
		xp.addEntry(entries.startIntField(name("levelCap"), config.levelCap)
				.setDefaultValue(defaults.levelCap).setMin(1).setTooltip(tooltip("levelCap"))
				.setSaveConsumer(v -> config.levelCap = v).build());
		xp.addEntry(entries.startDoubleField(name("deathXpFraction"), config.deathXpFraction)
				.setDefaultValue(defaults.deathXpFraction).setMin(0).setMax(1).setTooltip(tooltip("deathXpFraction"))
				.setSaveConsumer(v -> config.deathXpFraction = v).build());

		ConfigCategory table = builder.getOrCreateCategory(Component.translatable("config.archive.category.table"));
		table.addEntry(entries.startIntField(name("tableRadius"), config.tableRadius)
				.setDefaultValue(defaults.tableRadius).setMin(1).setMax(16).setTooltip(tooltip("tableRadius"))
				.setSaveConsumer(v -> config.tableRadius = v).build());
		table.addEntry(entries.startIntList(name("shelfSteps"), boxed(config.shelfSteps))
				.setDefaultValue(boxed(defaults.shelfSteps)).setMin(0).setTooltip(tooltip("shelfSteps"))
				.setErrorSupplier(list -> list.isEmpty()
						? Optional.of(Component.translatable("config.archive.shelfSteps.empty"))
						: Optional.empty())
				.setSaveConsumer(v -> config.shelfSteps = v.stream().mapToInt(Integer::intValue).sorted().toArray())
				.build());
		table.addEntry(entries.startBooleanToggle(name("tableShowLocked"), config.tableShowLocked)
				.setDefaultValue(defaults.tableShowLocked).setTooltip(tooltip("tableShowLocked"))
				.setSaveConsumer(v -> config.tableShowLocked = v).build());

		ConfigCategory mining = builder.getOrCreateCategory(Component.translatable("config.archive.category.mining"));
		mining.addEntry(entries.startFloatField(name("diamondSpeed"), config.diamondSpeed)
				.setDefaultValue(defaults.diamondSpeed).setMin(0.1F).setTooltip(tooltip("diamondSpeed"))
				.requireRestart()
				.setSaveConsumer(v -> config.diamondSpeed = v).build());
		mining.addEntry(entries.startFloatField(name("netheriteSpeed"), config.netheriteSpeed)
				.setDefaultValue(defaults.netheriteSpeed).setMin(0.1F).setTooltip(tooltip("netheriteSpeed"))
				.requireRestart()
				.setSaveConsumer(v -> config.netheriteSpeed = v).build());
		mining.addEntry(entries.startFloatField(name("deepslateHardness"), config.deepslateHardness)
				.setDefaultValue(defaults.deepslateHardness).setMin(0).setTooltip(tooltip("deepslateHardness"))
				.setSaveConsumer(v -> config.deepslateHardness = v).build());
		mining.addEntry(entries.startFloatField(name("deepslateOreHardness"), config.deepslateOreHardness)
				.setDefaultValue(defaults.deepslateOreHardness).setMin(0).setTooltip(tooltip("deepslateOreHardness"))
				.setSaveConsumer(v -> config.deepslateOreHardness = v).build());
		mining.addEntry(entries.startFloatField(name("obsidianHardness"), config.obsidianHardness)
				.setDefaultValue(defaults.obsidianHardness).setMin(0).setTooltip(tooltip("obsidianHardness"))
				.setSaveConsumer(v -> config.obsidianHardness = v).build());

		return builder.build();
	}

	private static Component name(String field) {
		return Component.translatable("config.archive." + field);
	}

	private static Component tooltip(String field) {
		return Component.translatable("config.archive." + field + ".tooltip");
	}

	private static List<Integer> boxed(int[] values) {
		return Arrays.stream(values).boxed().toList();
	}
}
