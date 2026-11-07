package maihatsuu.archive.tome;

import maihatsuu.archive.Archive;
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
import java.util.List;

/**
 * Plans the chiseled bookshelves in a stronghold library: 12 to 24 of its bookshelves, each holding 0 to 6
 * books in random slots, where each book has a 20% chance to be a tome rolled from the loot table
 * {@code archive:shelves/stronghold_library} (filled by {@code loot_tomes} files, so packs can change the tomes).
 * The library's chests are removed while this is on, so the shelves are its only loot.
 * Coordinates are the library piece's local ones, from {@code StrongholdPieces.Library.postProcess}.
 */
public final class LibraryShelves {
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
		LootTable table = level.getServer().reloadableRegistries().getLootTable(LOOT_TABLE);
		LootParams params = new LootParams.Builder(level).create(LootContextParamSets.EMPTY);
		List<Shelf> shelves = new ArrayList<>();
		for (Spot spot : spots.subList(0, count)) {
			List<ItemStack> books = new ArrayList<>(Collections.nCopies(6, ItemStack.EMPTY));
			List<Integer> slots = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5));
			Util.shuffle(slots, random);
			int bookCount = random.nextInt(7);
			for (int i = 0; i < bookCount; i++) {
				books.set(slots.get(i), random.nextFloat() < 0.2F ? tome(table, params, random) : new ItemStack(Items.BOOK));
			}
			shelves.add(new Shelf(spot.x(), spot.y(), spot.z(), spot.facing(), List.copyOf(books)));
		}
		return shelves;
	}

	/** A tome from the shelf loot table, or a plain book if the table gives nothing. */
	private static ItemStack tome(LootTable table, LootParams params, RandomSource random) {
		return table.getRandomItems(params, random).stream().filter(stack -> !stack.isEmpty()).findFirst()
				.map(stack -> stack.copyWithCount(1)).orElseGet(() -> new ItemStack(Items.BOOK));
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
