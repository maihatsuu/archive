package maihatsuu.archive.test;

import maihatsuu.archive.table.EnchantCosts;
import maihatsuu.archive.table.LibraryEnchantMenu;
import maihatsuu.archive.table.LibraryEntry;
import maihatsuu.archive.table.LibraryOptions;
import maihatsuu.archive.table.LibraryScan;
import maihatsuu.archive.table.LibraryUnlocks;
import maihatsuu.archive.tome.TomeItem;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChiseledBookShelfBlockEntity;
import net.minecraft.world.level.GameType;

import java.util.List;

/** Phase 7: library enchanting table. */
public class LibraryTableTests implements FabricGameTest {
	private static final BlockPos TABLE = new BlockPos(1, 1, 1);
	private static final BlockPos SHELF = new BlockPos(3, 1, 1);

	private static ChiseledBookShelfBlockEntity shelf(GameTestHelper helper, BlockPos relative) {
		helper.setBlock(relative, Blocks.CHISELED_BOOKSHELF);
		return (ChiseledBookShelfBlockEntity) helper.getBlockEntity(relative);
	}

	private static ServerPlayer survivalPlayer(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL);
		return player;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void scanFindsShelvedTomesInRadius(GameTestHelper helper) {
		helper.setBlock(TABLE, Blocks.ENCHANTING_TABLE);
		ChiseledBookShelfBlockEntity near = shelf(helper, SHELF);
		near.setItem(0, TomeItem.of(DataTests.enchant(helper, "minecraft", "sharpness")));
		near.setItem(1, TomeItem.of(DataTests.enchant(helper, "minecraft", "sharpness")));
		near.setItem(2, TomeItem.of(DataTests.enchant(helper, "minecraft", "mending")));
		near.setItem(3, new ItemStack(Items.BOOK));
		// 9x9x9 range: 4 blocks out is in, 5 is out.
		ChiseledBookShelfBlockEntity edge = shelf(helper, TABLE.offset(0, 4, 0));
		edge.setItem(0, TomeItem.of(DataTests.enchant(helper, "minecraft", "efficiency")));
		ChiseledBookShelfBlockEntity far = shelf(helper, TABLE.offset(0, 5, 0));
		far.setItem(0, TomeItem.of(DataTests.enchant(helper, "minecraft", "smite")));

		List<Holder<Enchantment>> found = LibraryScan.scan(helper.getLevel(), helper.absolutePos(TABLE), 4).tomes();
		helper.assertTrue(found.size() == 2, "should find sharpness and efficiency, found " + found.size());
		helper.assertTrue(found.contains(DataTests.enchant(helper, "minecraft", "sharpness")), "should find sharpness");
		helper.assertTrue(found.contains(DataTests.enchant(helper, "minecraft", "efficiency")), "should find the tome 4 blocks out");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void buyLevelByLevel(GameTestHelper helper) {
		helper.setBlock(TABLE, Blocks.ENCHANTING_TABLE);
		ChiseledBookShelfBlockEntity shelf = shelf(helper, SHELF);
		Holder<Enchantment> sharpness = DataTests.enchant(helper, "minecraft", "sharpness");
		Holder<Enchantment> smite = DataTests.enchant(helper, "minecraft", "smite");
		shelf.setItem(0, TomeItem.of(sharpness));
		shelf.setItem(1, TomeItem.of(smite));

		ServerPlayer player = survivalPlayer(helper);
		player.giveExperienceLevels(30);
		BlockPos tablePos = helper.absolutePos(TABLE);
		LibraryEnchantMenu menu = new LibraryEnchantMenu(1, player.getInventory(),
				LibraryEnchantMenu.scanEntries(helper.getLevel(), tablePos), ContainerLevelAccess.create(helper.getLevel(), tablePos));
		helper.assertTrue(menu.getEntries().stream().filter(LibraryEntry::fromTome).count() == 2, "menu should list sharpness and smite tomes");
		menu.getSlot(LibraryEnchantMenu.ITEM_SLOT).set(new ItemStack(Items.IRON_SWORD));
		menu.getSlot(LibraryEnchantMenu.LAPIS_SLOT).set(new ItemStack(Items.LAPIS_LAZULI, 10));

		// Default costs: level n = 5n XP levels, n lapis.
		helper.assertTrue(menu.tryBuy(player, sharpness), "should buy sharpness I");
		ItemStack sword = menu.getSlot(LibraryEnchantMenu.ITEM_SLOT).getItem();
		helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(sharpness, sword) == 1, "sword should have sharpness I");
		helper.assertTrue(player.experienceLevel == 25, "should cost 5 levels, now " + player.experienceLevel);
		helper.assertTrue(menu.getSlot(LibraryEnchantMenu.LAPIS_SLOT).getItem().getCount() == 9, "should cost 1 lapis");

		helper.assertTrue(menu.tryBuy(player, sharpness), "should buy sharpness II");
		helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(sharpness, sword) == 2, "sword should have sharpness II");
		helper.assertTrue(player.experienceLevel == 15, "should cost 10 more levels, now " + player.experienceLevel);
		helper.assertTrue(menu.getSlot(LibraryEnchantMenu.LAPIS_SLOT).getItem().getCount() == 7, "should cost 2 more lapis");

		helper.assertTrue(!menu.tryBuy(player, smite), "smite should be blocked by sharpness");

		player.experienceLevel = 14;
		helper.assertTrue(!menu.tryBuy(player, sharpness), "sharpness III costs 15 levels; 14 isn't enough");
		player.experienceLevel = 30;

		shelf.removeItem(0, 1);
		helper.assertTrue(!menu.tryBuy(player, sharpness), "buying III should fail once the tome leaves the shelf (bookshelves stop at I)");
		helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(sharpness, sword) == 2, "sword should still be sharpness II");
		player.discard();
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void maxLevelStopsBuying(GameTestHelper helper) {
		helper.setBlock(TABLE, Blocks.ENCHANTING_TABLE);
		Holder<Enchantment> silkTouch = DataTests.enchant(helper, "minecraft", "silk_touch");
		shelf(helper, SHELF).setItem(0, TomeItem.of(silkTouch));
		ServerPlayer player = survivalPlayer(helper);
		player.giveExperienceLevels(50);
		BlockPos tablePos = helper.absolutePos(TABLE);
		LibraryEnchantMenu menu = new LibraryEnchantMenu(1, player.getInventory(),
				LibraryEnchantMenu.scanEntries(helper.getLevel(), tablePos), ContainerLevelAccess.create(helper.getLevel(), tablePos));
		menu.getSlot(LibraryEnchantMenu.ITEM_SLOT).set(new ItemStack(Items.IRON_PICKAXE));
		menu.getSlot(LibraryEnchantMenu.LAPIS_SLOT).set(new ItemStack(Items.LAPIS_LAZULI, 10));
		helper.assertTrue(menu.tryBuy(player, silkTouch), "should buy silk touch I");
		helper.assertTrue(!menu.tryBuy(player, silkTouch), "silk touch is max level 1");
		helper.assertTrue(!menu.tryBuy(player, DataTests.enchant(helper, "minecraft", "sharpness")), "sharpness isn't shelved");
		player.discard();
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void costTableLoadsFromData(GameTestHelper helper) {
		Holder<Enchantment> efficiency = DataTests.enchant(helper, "minecraft", "efficiency");
		helper.assertTrue(EnchantCosts.cost(efficiency, 1).equals(new EnchantCosts.LevelCost(1, 1)), "efficiency I from data");
		helper.assertTrue(EnchantCosts.cost(efficiency, 2).equals(new EnchantCosts.LevelCost(2, 1)), "efficiency II from data");
		helper.assertTrue(EnchantCosts.cost(efficiency, 3).equals(EnchantCosts.defaultCost(3)), "efficiency III falls back to default");
		helper.assertTrue(EnchantCosts.allLevels(efficiency).size() == 5, "efficiency has 5 levels");
		// archive's efficiency file sets shelf_max; archive_test's sets levels. Both apply.
		helper.assertTrue(EnchantCosts.shelfMax(efficiency) == 2, "efficiency shelf_max from archive's file");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void enchantingTableOpensLibraryMenu(GameTestHelper helper) {
		helper.setBlock(TABLE, Blocks.ENCHANTING_TABLE);
		ServerPlayer player = survivalPlayer(helper);
		BlockPos tablePos = helper.absolutePos(TABLE);
		player.openMenu(helper.getLevel().getBlockState(tablePos).getMenuProvider(helper.getLevel(), tablePos));
		helper.assertTrue(player.containerMenu instanceof LibraryEnchantMenu, "table should open the library menu, got "
				+ player.containerMenu.getClass().getSimpleName());
		player.closeContainer();
		player.discard();
		helper.succeed();
	}

	private static LibraryEntry entry(GameTestHelper helper, String path) {
		Holder<Enchantment> enchant = DataTests.enchant(helper, "minecraft", path);
		return LibraryUnlocks.compute(helper.getLevel(), helper.absolutePos(TABLE)).stream()
				.filter(e -> e.enchant().equals(enchant)).findFirst().orElse(null);
	}

	private static void placeBookshelves(GameTestHelper helper, int count, Block block) {
		for (int i = 0; i < count; i++) {
			helper.setBlock(new BlockPos(i, 1, 4), block);
		}
	}

	/** Starter data: sharpness shelf_max 2, smite 3. Default shelfSteps {0, 5, 10}. */
	@GameTest(template = EMPTY_STRUCTURE)
	public void tableAloneUnlocksLevelOne(GameTestHelper helper) {
		helper.setBlock(TABLE, Blocks.ENCHANTING_TABLE);
		LibraryEntry sharpness = entry(helper, "sharpness");
		helper.assertTrue(sharpness != null, "sharpness should be offered with no bookshelves");
		helper.assertTrue(sharpness.unlockedMax() == 1 && !sharpness.fromTome(), "sharpness I only, got " + sharpness);
		helper.assertTrue(sharpness.shelvesForNext() == 5, "sharpness II needs 5 bookshelves, got " + sharpness.shelvesForNext());
		helper.assertTrue(entry(helper, "silk_touch") == null, "silk touch has no shelf_max");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void bookshelvesRaiseTheLimit(GameTestHelper helper) {
		helper.setBlock(TABLE, Blocks.ENCHANTING_TABLE);
		placeBookshelves(helper, 5, Blocks.BOOKSHELF);
		LibraryEntry sharpness = entry(helper, "sharpness");
		helper.assertTrue(sharpness.unlockedMax() == 2, "5 shelves: sharpness II, got " + sharpness.unlockedMax());
		helper.assertTrue(sharpness.shelvesForNext() == 0, "sharpness III needs a tome");
		LibraryEntry smite = entry(helper, "smite");
		helper.assertTrue(smite.unlockedMax() == 2 && smite.shelvesForNext() == 10, "5 shelves: smite II, III at 10, got " + smite);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void chiseledBookshelvesDontCount(GameTestHelper helper) {
		helper.setBlock(TABLE, Blocks.ENCHANTING_TABLE);
		placeBookshelves(helper, 5, Blocks.CHISELED_BOOKSHELF);
		helper.assertTrue(entry(helper, "sharpness").unlockedMax() == 1, "chiseled bookshelves shouldn't count");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void tomeUnlocksEveryLevel(GameTestHelper helper) {
		helper.setBlock(TABLE, Blocks.ENCHANTING_TABLE);
		shelf(helper, SHELF).setItem(0, TomeItem.of(DataTests.enchant(helper, "minecraft", "sharpness")));
		LibraryEntry sharpness = entry(helper, "sharpness");
		helper.assertTrue(sharpness.fromTome() && sharpness.unlockedMax() == 5, "tome: sharpness up to V, got " + sharpness);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void cantBuyPastTheBookshelfLimit(GameTestHelper helper) {
		helper.setBlock(TABLE, Blocks.ENCHANTING_TABLE);
		Holder<Enchantment> sharpness = DataTests.enchant(helper, "minecraft", "sharpness");
		ServerPlayer player = survivalPlayer(helper);
		player.giveExperienceLevels(30);
		BlockPos tablePos = helper.absolutePos(TABLE);
		LibraryEnchantMenu menu = new LibraryEnchantMenu(1, player.getInventory(),
				LibraryEnchantMenu.scanEntries(helper.getLevel(), tablePos), ContainerLevelAccess.create(helper.getLevel(), tablePos));
		menu.getSlot(LibraryEnchantMenu.ITEM_SLOT).set(new ItemStack(Items.IRON_SWORD));
		menu.getSlot(LibraryEnchantMenu.LAPIS_SLOT).set(new ItemStack(Items.LAPIS_LAZULI, 10));

		helper.assertTrue(menu.tryBuy(player, sharpness), "should buy sharpness I with no bookshelves");
		helper.assertTrue(player.experienceLevel == 25, "sharpness I costs 5 levels, now " + player.experienceLevel);
		LibraryOptions.Option next = menu.options(player).stream().filter(o -> o.enchant().equals(sharpness)).findFirst().orElseThrow();
		helper.assertTrue(next.state() == LibraryOptions.State.LOCKED, "sharpness II should be locked, got " + next.state());
		helper.assertTrue(!menu.tryBuy(player, sharpness), "sharpness II needs bookshelves");
		helper.assertTrue(player.experienceLevel == 25, "a refused buy shouldn't cost XP");
		helper.assertTrue(menu.getSlot(LibraryEnchantMenu.LAPIS_SLOT).getItem().getCount() == 9, "a refused buy shouldn't cost lapis");

		placeBookshelves(helper, 5, Blocks.BOOKSHELF);
		helper.assertTrue(menu.tryBuy(player, sharpness), "5 bookshelves placed after opening: sharpness II should now buy");
		player.discard();
		helper.succeed();
	}
}
