package maihatsuu.archive.test;

import maihatsuu.archive.registry.ModComponents;
import maihatsuu.archive.registry.ModItems;
import maihatsuu.archive.tome.BookSources;
import maihatsuu.archive.tome.SetTomeFunction;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;

/** Phase 6: no enchanted books from loot or trades; set_tome. */
public class BookSourceTests implements FabricGameTest {
	/** Every vanilla 1.21.1 loot table that can produce an enchanted book (chest param set). */
	private static final List<String> CHEST_TABLES = List.of(
			"chests/abandoned_mineshaft", "chests/ancient_city", "chests/bastion_other", "chests/desert_pyramid",
			"chests/jungle_temple", "chests/pillager_outpost", "chests/shipwreck_map", "chests/simple_dungeon",
			"chests/stronghold_corridor", "chests/stronghold_crossing", "chests/stronghold_library",
			"chests/trial_chambers/reward_ominous_rare", "chests/trial_chambers/reward_rare", "chests/underwater_ruin_big",
			"chests/village/village_desert_house", "chests/village/village_plains_house", "chests/woodland_mansion");
	private static final int ROLLS = 300;

	private static LootTable table(ServerLevel level, String path) {
		return level.getServer().reloadableRegistries().getLootTable(
				ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.withDefaultNamespace(path)));
	}

	private static void assertNoBooks(GameTestHelper helper, String path, LootParams params) {
		LootTable table = table(helper.getLevel(), path);
		helper.assertTrue(table != LootTable.EMPTY, path + " should exist");
		int items = 0;
		for (int i = 0; i < ROLLS; i++) {
			for (ItemStack stack : table.getRandomItems(params)) {
				helper.assertTrue(!stack.isEmpty(), path + " produced an empty stack");
				helper.assertTrue(!BookSources.isEnchantedBook(stack), path + " produced an enchanted book");
				items++;
			}
		}
		helper.assertTrue(items > 0, path + " produced nothing at all in " + ROLLS + " rolls");
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
	public void chestLootHasNoEnchantedBooks(GameTestHelper helper) {
		Vec3 origin = helper.absoluteVec(Vec3.ZERO);
		LootParams params = new LootParams.Builder(helper.getLevel())
				.withParameter(LootContextParams.ORIGIN, origin)
				.create(LootContextParamSets.CHEST);
		for (String path : CHEST_TABLES) {
			assertNoBooks(helper, path, params);
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 400)
	public void fishingBarteringAndGiftsHaveNoEnchantedBooks(GameTestHelper helper) {
		Vec3 origin = helper.absoluteVec(Vec3.ZERO);
		assertNoBooks(helper, "gameplay/fishing/treasure", new LootParams.Builder(helper.getLevel())
				.withParameter(LootContextParams.ORIGIN, origin)
				.withParameter(LootContextParams.TOOL, new ItemStack(Items.FISHING_ROD))
				.create(LootContextParamSets.FISHING));

		Piglin piglin = helper.spawn(EntityType.PIGLIN, BlockPos.ZERO.above());
		assertNoBooks(helper, "gameplay/piglin_bartering", new LootParams.Builder(helper.getLevel())
				.withParameter(LootContextParams.THIS_ENTITY, piglin)
				.create(LootContextParamSets.PIGLIN_BARTER));

		Villager villager = helper.spawn(EntityType.VILLAGER, BlockPos.ZERO.above());
		assertNoBooks(helper, "gameplay/hero_of_the_village/librarian_gift", new LootParams.Builder(helper.getLevel())
				.withParameter(LootContextParams.ORIGIN, origin)
				.withParameter(LootContextParams.THIS_ENTITY, villager)
				.create(LootContextParamSets.GIFT));
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void librariansSellNoBooks(GameTestHelper helper) {
		// Vanilla level-1 librarians pick 2 of 4 listings, one of which is an enchanted book.
		for (int i = 0; i < 40; i++) {
			Villager villager = helper.spawn(EntityType.VILLAGER, BlockPos.ZERO.above());
			villager.setVillagerData(new VillagerData(VillagerType.PLAINS, VillagerProfession.LIBRARIAN, 1));
			for (MerchantOffer offer : villager.getOffers()) {
				helper.assertTrue(!BookSources.isEnchantedBook(offer.getResult()), "librarian offered an enchanted book");
			}
			helper.assertTrue(villager.getOffers().size() == 2, "librarian should still get 2 trades, got " + villager.getOffers().size());
			villager.discard();
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void setTomeFunction(GameTestHelper helper) {
		LootContext context = new LootContext.Builder(new LootParams.Builder(helper.getLevel()).create(LootContextParamSets.EMPTY))
				.create(Optional.empty());
		ItemStack tome = SetTomeFunction.builder(DataTests.enchant(helper, "minecraft", "sharpness")).build()
				.apply(new ItemStack(ModItems.TOME), context);
		helper.assertTrue(tome.has(ModComponents.TOME_ENCHANTMENT), "set_tome should set the enchant");
		ItemStack refused = SetTomeFunction.builder(DataTests.enchant(helper, "minecraft", "mending")).build()
				.apply(new ItemStack(ModItems.TOME), context);
		helper.assertTrue(refused.isEmpty(), "set_tome should refuse mending");
		helper.succeed();
	}

	/** Each structure_tomes file adds its own pool; unknown enchants are skipped (test datapack). */
	@GameTest(template = EMPTY_STRUCTURE)
	public void structureTomesFromDatapack(GameTestHelper helper) {
		LootTable table = helper.getLevel().getServer().reloadableRegistries().getLootTable(
				ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("archive_test", "tome_target")));
		LootParams params = new LootParams.Builder(helper.getLevel()).create(LootContextParamSets.EMPTY);
		for (int i = 0; i < 50; i++) {
			List<String> enchants = table.getRandomItems(params).stream()
					.map(stack -> {
						helper.assertTrue(stack.is(ModItems.TOME), "structure tome pool produced " + stack);
						return stack.get(ModComponents.TOME_ENCHANTMENT).getRegisteredName();
					})
					.sorted()
					.toList();
			helper.assertTrue(enchants.equals(List.of("minecraft:efficiency", "minecraft:sharpness")),
					"expected one sharpness and one efficiency tome, got " + enchants);
		}
		helper.succeed();
	}
}
