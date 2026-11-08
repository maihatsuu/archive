package maihatsuu.archive.client;

import maihatsuu.archive.ArchiveConfig;
import maihatsuu.archive.GearComponents;
import maihatsuu.archive.RepairMaterials;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.StringListListEntry;
import me.shedaniel.clothconfig2.impl.builders.SubCategoryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Cloth Config screen for {@link ArchiveConfig}. Edits the live config instance, so settings read at use time
 * apply at once in singleplayer; durability and tool speed are baked in at startup and are marked restart-only.
 */
public final class ArchiveConfigScreen {
	private ArchiveConfigScreen() {
	}

	public static Screen create(Screen parent) {
		ArchiveConfig config = ArchiveConfig.local();
		ArchiveConfig defaults = new ArchiveConfig();

		ConfigBuilder builder = ConfigBuilder.create()
				.setParentScreen(parent)
				.setTitle(Component.translatable("config.archive.title"))
				.setSavingRunnable(config::save);
		ConfigEntryBuilder entries = builder.entryBuilder();

		ConfigCategory general = builder.getOrCreateCategory(Component.translatable("config.archive.category.general"));
		if (ArchiveConfig.get() != config) {
			general.addEntry(entries.startTextDescription(Component.translatable("config.archive.serverNote").withStyle(ChatFormatting.YELLOW)).build());
		}
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
		general.addEntry(entries.startBooleanToggle(name("strongholdShelves"), config.strongholdShelves)
				.setDefaultValue(defaults.strongholdShelves).setTooltip(tooltip("strongholdShelves"))
				.setSaveConsumer(v -> config.strongholdShelves = v).build());
		general.addEntry(entries.startBooleanToggle(name("shelfModdedEnchants"), config.shelfModdedEnchants)
				.setDefaultValue(defaults.shelfModdedEnchants).setTooltip(tooltip("shelfModdedEnchants"))
				.setSaveConsumer(v -> config.shelfModdedEnchants = v).build());
		general.addEntry(entries.startBooleanToggle(name("resilience"), config.resilience)
				.setDefaultValue(defaults.resilience).setTooltip(tooltip("resilience"))
				.setSaveConsumer(v -> config.resilience = v).build());

		ConfigCategory xp = builder.getOrCreateCategory(Component.translatable("config.archive.category.experience"));
		xp.addEntry(entries.startIntField(name("xpPerLevel"), config.xpPerLevel)
				.setDefaultValue(defaults.xpPerLevel).setMin(1)
				.setSaveConsumer(v -> config.xpPerLevel = v).build());
		xp.addEntry(entries.startIntField(name("levelCap"), config.levelCap)
				.setDefaultValue(defaults.levelCap).setMin(1)
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
				.setDefaultValue(defaults.obsidianHardness).setMin(0)
				.setSaveConsumer(v -> config.obsidianHardness = v).build());
		mining.addEntry(hardness.build());

		durabilityTab(builder.getOrCreateCategory(Component.translatable("config.archive.category.durability")), entries, config);

		return builder.build();
	}

	/**
	 * Each item in Archive's durability table folds open to its durability (0 = unbreakable) and repair material,
	 * both defaulting to Archive's values; a value that differs is saved as an override. Overrides for other items (modded gear) are edited as "id=durability" lines.
	 */
	private static void durabilityTab(ConfigCategory category, ConfigEntryBuilder entries, ArchiveConfig config) {
		Map<String, Integer> overrides = config.durabilityOverrides;
		category.addEntry(entries.startTextDescription(Component.translatable("config.archive.durability.unbreakableHint")).build());
		// One folded group per material (tools and armor together), then everything else.
		Map<String, SubCategoryBuilder> groups = new LinkedHashMap<>();
		for (String material : MATERIALS) {
			groups.put(material, entries.startSubCategory(Component.translatable("config.archive.group." + material)).setExpanded(false));
		}
		SubCategoryBuilder other = entries.startSubCategory(Component.translatable("config.archive.group.otherGear")).setExpanded(false);
		// Each item folds open to its durability and repair material.
		GearComponents.DEFAULT_DURABILITY.forEach((id, archiveValue) -> {
			SubCategoryBuilder item = entries.startSubCategory(itemName(id)).setExpanded(false);
			item.add(entries.startIntField(name("durabilityValue"), overrides.getOrDefault(id, archiveValue))
					.setDefaultValue(archiveValue).setMin(0)
					.requireRestart()
					.setSaveConsumer(v -> {
						if (v.equals(archiveValue)) {
							overrides.remove(id);
						} else {
							overrides.put(id, v);
						}
					}).build());
			if (RepairMaterials.DEFAULTS.containsKey(id)) {
				item.add(repairField(entries, config, id));
			}
			groups.getOrDefault(RepairMaterials.materialPrefix(id), other).add(item.build());
		});
		groups.values().forEach(group -> category.addEntry(group.build()));
		category.addEntry(other.build());

		List<String> extra = new ArrayList<>();
		overrides.forEach((id, value) -> {
			if (!GearComponents.DEFAULT_DURABILITY.containsKey(id)) {
				extra.add(id + "=" + value);
			}
		});
		category.addEntry(entries.startStrList(name("durabilityExtra"), extra)
				.setDefaultValue(List.of())
				// New lines start with the format as placeholder text (which doesn't parse, so it can't be saved
				// by accident), at the top of an already expanded list so they're easy to spot.
				.setExpanded(true).setInsertInFront(true)
				.setCreateNewInstance(list -> new StringListListEntry.StringListCell(PLACEHOLDER, list))
				.setAddButtonTooltip(Component.translatable("config.archive.durabilityExtra.add"))
				.setCellErrorSupplier(line -> parseOverride(line) == null
						? Optional.of(Component.translatable("config.archive.durabilityExtra.invalid"))
						: Optional.empty())
				.requireRestart()
				.setSaveConsumer(lines -> {
					overrides.keySet().removeIf(id -> !GearComponents.DEFAULT_DURABILITY.containsKey(id));
					for (String line : lines) {
						Map.Entry<String, Integer> entry = parseOverride(line);
						if (entry != null && !GearComponents.DEFAULT_DURABILITY.containsKey(entry.getKey())) {
							overrides.put(entry.getKey(), entry.getValue());
						}
					}
				}).build());
	}

	/** Durability groups, by item id prefix ({@link RepairMaterials#materialPrefix}), weakest first. */
	private static final List<String> MATERIALS = List.of("leather", "wooden", "stone", "chainmail", "iron", "golden", "diamond", "netherite");

	private static final String PLACEHOLDER = "namespace:item=durability";

	/** An item's repair material: saving Archive's default clears the override, anything else is saved as one. */
	private static AbstractConfigListEntry<String> repairField(ConfigEntryBuilder entries, ArchiveConfig config, String id) {
		Map<String, String> overrides = config.repairOverrides;
		String archiveValue = RepairMaterials.DEFAULTS.get(id);
		return entries.startStrField(name("repairMaterial"), overrides.getOrDefault(id, archiveValue))
				.setDefaultValue(archiveValue)
				.setErrorSupplier(v -> RepairMaterials.isValidMaterial(v.trim())
						? Optional.empty()
						: Optional.of(Component.translatable("config.archive.repairMaterial.invalid")))
				.setSaveConsumer(v -> {
					String material = v.trim();
					if (material.equals(archiveValue)) {
						overrides.remove(id);
					} else {
						overrides.put(id, material);
					}
				}).build();
	}

	/** "namespace:item=durability" with a valid id and a number of 0 (unbreakable) or more, else null. */
	private static Map.Entry<String, Integer> parseOverride(String line) {
		int eq = line.indexOf('=');
		if (eq < 0) {
			return null;
		}
		ResourceLocation id = ResourceLocation.tryParse(line.substring(0, eq).trim());
		try {
			int value = Integer.parseInt(line.substring(eq + 1).trim());
			return id == null || value < 0 ? null : Map.entry(id.toString(), value);
		} catch (NumberFormatException e) {
			return null;
		}
	}

	private static Component itemName(String id) {
		ResourceLocation location = ResourceLocation.tryParse(id);
		return location == null ? Component.literal(id)
				: BuiltInRegistries.ITEM.getOptional(location).map(Item::getDescription).orElse(Component.literal(id));
	}

	/** A tool speed field: baked into the TOOL component at startup, so it needs a restart. */
	private static void speed(ConfigEntryBuilder entries, SubCategoryBuilder group, String field, float value, float fallback,
			Consumer<Float> save) {
		group.add(entries.startFloatField(name(field), value)
				.setDefaultValue(fallback).setMin(0.1F)
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
