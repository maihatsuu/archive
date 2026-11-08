package maihatsuu.archive.tome;

import maihatsuu.archive.Archive;
import maihatsuu.archive.ArchiveConfig;
import maihatsuu.archive.registry.ModItems;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.Util;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Plans the chiseled bookshelves in a stronghold library: 12 to 24 of its bookshelves, each holding 0 to 6
 * books in random slots, where each book has a 20% chance to be a tome rolled from the loot table
 * {@code archive:shelves/stronghold_library} (filled by {@code loot_tomes} files, so packs can change the tomes).
 * With config {@code shelfModdedEnchants}, modded enchants that no shelf file lists join the roll at weight 1
 * each, beside the table's own weights, so enchant mods show up without a datapack.
 * The library's chests are removed while this is on, so the shelves are its only loot.
 * Coordinates are the library piece's local ones, from {@code StrongholdPieces.Library.postProcess}.
 */
public final class LibraryShelves {
	private static final Set<String> VANILLA_NAMESPACES = Set.of("minecraft", Archive.MOD_ID);
	public static final ResourceKey<LootTable> LOOT_TABLE = ResourceKey.create(Registries.LOOT_TABLE, Archive.id("shelves/stronghold_library"));

	/** One chiseled bookshelf: local position, local facing, and six slots (empty stacks for gaps). */
	public record Shelf(int x, int y, int z, Direction facing, List<ItemStack> books) {
	}

	private record Spot(int x, int y, int z, Direction facing) {
	}

	public static List<Shelf> plan(boolean tall, RandomSource random, ServerLevel level) {
		List<Spot> spots = spots(tall, random);
		Util.shuffle(spots, random);
		int count = Math.min(spots.size(), 12 + random.nextInt(13));
		Tomes tomes = Tomes.of(level);
		List<Shelf> shelves = new ArrayList<>();
		for (Spot spot : spots.subList(0, count)) {
			List<ItemStack> books = new ArrayList<>(Collections.nCopies(6, ItemStack.EMPTY));
			List<Integer> slots = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5));
			Util.shuffle(slots, random);
			int bookCount = random.nextInt(7);
			for (int i = 0; i < bookCount; i++) {
				books.set(slots.get(i), random.nextFloat() < 0.2F ? tomes.roll(random) : new ItemStack(Items.BOOK));
			}
			shelves.add(new Shelf(spot.x(), spot.y(), spot.z(), spot.facing(), List.copyOf(books)));
		}
		return shelves;
	}

	/** Modded, obtainable, non-curse enchants no shelf {@code loot_tomes} file lists, by id; empty when the config is off. */
	public static List<Holder<Enchantment>> moddedTomes(ServerLevel level) {
		if (!ArchiveConfig.get().shelfModdedEnchants) {
			return List.of();
		}
		Map<ResourceLocation, Integer> listed = LootTomes.tomes(LOOT_TABLE.location());
		return level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).holders()
				.filter(e -> !VANILLA_NAMESPACES.contains(e.key().location().getNamespace()))
				.filter(e -> !listed.containsKey(e.key().location()))
				.filter(e -> ModItems.isTomeEnchant(e) && !e.is(EnchantmentTags.CURSE))
				.sorted(Comparator.comparing(e -> e.key().location()))
				.<Holder<Enchantment>>map(e -> e).toList();
	}

	/** The shelf loot table, plus the modded enchants rolled beside it. */
	private record Tomes(LootTable table, LootParams params, int tableWeight, List<Holder<Enchantment>> modded) {
		static Tomes of(ServerLevel level) {
			LootTable table = level.getServer().reloadableRegistries().getLootTable(LOOT_TABLE);
			LootParams params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
			Registry<Enchantment> registry = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
			Map<ResourceLocation, Integer> listed = LootTomes.tomes(LOOT_TABLE.location());
			int tableWeight = listed.entrySet().stream().filter(e -> registry.containsKey(e.getKey())).mapToInt(Map.Entry::getValue).sum();
			return new Tomes(table, params, tableWeight, moddedTomes(level));
		}

		/** A modded tome (weight 1 each) or one from the table; a plain book if the table gives nothing. */
		ItemStack roll(RandomSource random) {
			if (!this.modded.isEmpty()) {
				int pick = random.nextInt(this.tableWeight + this.modded.size());
				if (pick < this.modded.size()) {
					return TomeItem.of(this.modded.get(pick));
				}
			}
			return this.table.getRandomItems(this.params, random).stream().filter(stack -> !stack.isEmpty()).findFirst()
					.map(stack -> stack.copyWithCount(1)).orElseGet(() -> new ItemStack(Items.BOOK));
		}
	}

	/** Every bookshelf a chiseled one can replace, facing a free side. */
	private static List<Spot> spots(boolean tall, RandomSource random) {
		List<Spot> spots = new ArrayList<>();
		// Wall shelves (x = 1 and x = 12), between the plank pillars every 4 blocks; the tall library has a second storey.
		for (int z = 2; z <= 12; z++) {
			if ((z - 1) % 4 == 0) {
				continue;
			}
			for (int y = 1; y <= (tall ? 9 : 4); y++) {
				if (y == 5) {
					continue;
				}
				spots.add(new Spot(1, y, z, Direction.EAST));
				spots.add(new Spot(12, y, z, Direction.WEST));
			}
		}
		// Free-standing rows, open on both sides. (3, 3, 5) is where vanilla's chest would go; it stays a plain shelf.
		for (int z = 3; z < 12; z += 2) {
			for (int x : new int[] {3, 4, 6, 7, 9, 10}) {
				for (int y = 1; y <= 3; y++) {
					if (x == 3 && y == 3 && z == 5) {
						continue;
					}
					spots.add(new Spot(x, y, z, random.nextBoolean() ? Direction.NORTH : Direction.SOUTH));
				}
			}
		}
		return spots;
	}

	private LibraryShelves() {
	}
}
