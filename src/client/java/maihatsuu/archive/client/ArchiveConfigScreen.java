package maihatsuu.archive.client;

import maihatsuu.archive.ArchiveConfig;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

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
		// Folded groups: tool speeds and block hardness.
		SubCategoryBuilder speeds = entries.startSubCategory(Component.translatable("config.archive.group.toolSpeed"))
				.setExpanded(false);
		speed(entries, speeds, "woodSpeed", config.woodSpeed, defaults.woodSpeed, v -> config.woodSpeed = v);
		speed(entries, speeds, "stoneSpeed", config.stoneSpeed, defaults.stoneSpeed, v -> config.stoneSpeed = v);
		speed(entries, speeds, "ironSpeed", config.ironSpeed, defaults.ironSpeed, v -> config.ironSpeed = v);
		speed(entries, speeds, "goldSpeed", config.goldSpeed, defaults.goldSpeed, v -> config.goldSpeed = v);
		speed(entries, speeds, "diamondSpeed", config.diamondSpeed, defaults.diamondSpeed, v -> config.diamondSpeed = v);
		speed(entries, speeds, "netheriteSpeed", config.netheriteSpeed, defaults.netheriteSpeed, v -> config.netheriteSpeed = v);
		mining.addEntry(speeds.build());
		SubCategoryBuilder hardness = entries.startSubCategory(Component.translatable("config.archive.group.hardness"))
				.setExpanded(false);
		hardness.add(entries.startFloatField(name("deepslateHardnessReduction"), config.deepslateHardnessReduction)
				.setDefaultValue(defaults.deepslateHardnessReduction).setMin(0).setTooltip(tooltip("deepslateHardnessReduction"))
				.setSaveConsumer(v -> config.deepslateHardnessReduction = v).build());
		hardness.add(entries.startFloatField(name("obsidianHardness"), config.obsidianHardness)
				.setDefaultValue(defaults.obsidianHardness).setMin(0).setTooltip(tooltip("obsidianHardness"))
				.setSaveConsumer(v -> config.obsidianHardness = v).build());
		mining.addEntry(hardness.build());

		return builder.build();
	}

	/** A tool speed field: baked into the TOOL component at startup, so it needs a restart. */
	private static void speed(ConfigEntryBuilder entries, SubCategoryBuilder group, String field, float value, float fallback,
			Consumer<Float> save) {
		group.add(entries.startFloatField(name(field), value)
				.setDefaultValue(fallback).setMin(0.1F).setTooltip(tooltip(field))
				.requireRestart()
				.setSaveConsumer(save).build());
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
