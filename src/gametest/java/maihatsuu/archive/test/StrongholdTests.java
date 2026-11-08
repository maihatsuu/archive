package maihatsuu.archive.test;

import maihatsuu.archive.registry.ModItems;
import maihatsuu.archive.tome.LibraryShelves;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.structures.StrongholdPieces;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StrongholdTests implements FabricGameTest {
	/** Generates a tall library high above the test, checks its chiseled shelves, then again chunk-half by chunk-half. */
	@GameTest(template = EMPTY_STRUCTURE, batch = "stronghold")
	public void libraryShelves(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		BlockPos origin = helper.absolutePos(new BlockPos(4, 40, 0));
		BoundingBox box = BoundingBox.orientBox(origin.getX(), origin.getY(), origin.getZ(), -4, -1, 0, 14, 11, 15, Direction.NORTH);
		StrongholdPieces.Library piece = new StrongholdPieces.Library(0, level.random, box, Direction.NORTH);

		piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random, box, new ChunkPos(origin), origin);
		Map<BlockPos, String> first = shelves(helper, level, box);
		helper.assertTrue(first.size() >= 12 && first.size() <= 24, "expected 12-24 chiseled shelves, got " + first.size());
		helper.assertTrue(BlockPos.betweenClosedStream(box).noneMatch(pos -> level.getBlockState(pos).is(Blocks.CHEST)), "library still has a chest");
		helper.assertTrue(BlockPos.betweenClosedStream(box).noneMatch(pos -> level.getBlockState(pos).is(Blocks.CAVE_AIR)), "chest gap still carved");

		// Generated in two parts, as two chunks would, with different randoms: same shelves and books.
		clear(level, box);
		int mid = (box.minX() + box.maxX()) / 2;
		BoundingBox west = new BoundingBox(box.minX(), box.minY(), box.minZ(), mid, box.maxY(), box.maxZ());
		BoundingBox east = new BoundingBox(mid + 1, box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ());
		piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random, west, new ChunkPos(origin), origin);
		piece.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.random, east, new ChunkPos(origin), origin);
		Map<BlockPos, String> second = shelves(helper, level, box);
		helper.assertTrue(first.equals(second), "split generation differs: " + first + " vs " + second);

		clear(level, box);
		helper.succeed();
	}

	/** Modded enchants (here the test datapack's) join the shelf roll; vanilla and Archive ones come only from the table. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void moddedShelfTomes(GameTestHelper helper) {
		List<String> ids = LibraryShelves.moddedTomes(helper.getLevel()).stream().map(e -> e.unwrapKey().orElseThrow().location().toString()).toList();
		helper.assertTrue(ids.contains("archive_test:shelf_test"), "modded enchant should be a shelf tome: " + ids);
		helper.assertTrue(ids.stream().noneMatch(id -> id.startsWith("minecraft:") || id.startsWith("archive:")), "only modded enchants: " + ids);
		helper.succeed();
	}

	/** The shelf tome table (empty apart from its loot_tomes pool) always gives one tome. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void libraryShelfTable(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		LootTable table = level.getServer().reloadableRegistries().getLootTable(LibraryShelves.LOOT_TABLE);
		for (int i = 0; i < 20; i++) {
			List<ItemStack> drops = table.getRandomItems(new LootParams.Builder(level).create(LootContextParamSets.EMPTY));
			helper.assertTrue(drops.size() == 1 && drops.get(0).is(ModItems.TOME), "expected one tome, got " + drops);
		}
		helper.succeed();
	}

	/** Every chiseled shelf in the box: faces open space, holds only books and tomes, and its block shows its slots. */
	private static Map<BlockPos, String> shelves(GameTestHelper helper, ServerLevel level, BoundingBox box) {
		Map<BlockPos, String> found = new HashMap<>();
		BlockPos.betweenClosedStream(box).forEach(pos -> {
			BlockState state = level.getBlockState(pos);
			if (!state.is(Blocks.CHISELED_BOOKSHELF)) {
				return;
			}
			BlockPos front = pos.relative(state.getValue(HorizontalDirectionalBlock.FACING));
			helper.assertTrue(level.getBlockState(front).getCollisionShape(level, front).isEmpty(),
					"shelf at " + pos + " faces " + level.getBlockState(front));
			ChiseledBookShelfBlockEntity entity = (ChiseledBookShelfBlockEntity) level.getBlockEntity(pos);
			StringBuilder contents = new StringBuilder();
			for (int slot = 0; slot < 6; slot++) {
				ItemStack stack = entity.getItem(slot);
				helper.assertTrue(stack.isEmpty() || stack.is(Items.BOOK) || stack.is(ModItems.TOME), "unexpected " + stack + " at " + pos);
				helper.assertTrue(state.getValue(ChiseledBookShelfBlock.SLOT_OCCUPIED_PROPERTIES.get(slot)) == !stack.isEmpty(),
						"slot " + slot + " state mismatch at " + pos);
				contents.append(stack.isEmpty() ? "-" : stack.getItem() + "" + stack.getComponentsPatch()).append(';');
			}
			found.put(pos.immutable(), state.getValue(HorizontalDirectionalBlock.FACING) + ":" + contents);
		});
		return found;
	}

	private static void clear(ServerLevel level, BoundingBox box) {
		BlockPos.betweenClosedStream(box).forEach(pos -> level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2));
	}
}
