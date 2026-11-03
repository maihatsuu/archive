package maihatsuu.archive.test;

import maihatsuu.archive.table.GrindstoneRefund;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Phase 4: anvil repairs/renames only and is free; grindstone only disenchants. */
public class AnvilGrindstoneTests implements FabricGameTest {
	private static AnvilMenu anvil(ServerPlayer player, ItemStack left, ItemStack right) {
		AnvilMenu menu = new AnvilMenu(0, player.getInventory(), ContainerLevelAccess.NULL);
		menu.getSlot(0).set(left);
		menu.getSlot(1).set(right);
		return menu;
	}

	private static ItemStack damaged(ItemStack stack, int damage) {
		stack.setDamageValue(damage);
		return stack;
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anvilRepairsWithMaterialForFree(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		AnvilMenu menu = anvil(player, damaged(new ItemStack(Items.IRON_CHESTPLATE), 200), new ItemStack(Items.IRON_INGOT, 3));
		ItemStack result = menu.getSlot(2).getItem();
		helper.assertTrue(result.is(Items.IRON_CHESTPLATE), "iron chestplate + iron ingot should repair");
		helper.assertTrue(result.getDamageValue() == 0, "one ingot should fully repair, damage now " + result.getDamageValue());
		helper.assertTrue(menu.getCost() == 0, "repair should cost 0 levels, got " + menu.getCost());
		helper.assertTrue(menu.getSlot(2).mayPickup(player), "free result should be pickable");
		menu.getSlot(2).onTake(player, result);
		helper.assertTrue(menu.getSlot(1).getItem().getCount() == 2, "repair should use exactly 1 ingot, "
				+ menu.getSlot(1).getItem().getCount() + " left");
		helper.assertTrue(anvil(player, new ItemStack(Items.IRON_CHESTPLATE), new ItemStack(Items.IRON_INGOT))
				.getSlot(2).getItem().isEmpty(), "undamaged item should give no repair result");
		player.discard();
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void tridentAndMaceRepair(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack trident = anvil(player, damaged(new ItemStack(Items.TRIDENT), 100), new ItemStack(Items.PRISMARINE_SHARD)).getSlot(2).getItem();
		helper.assertTrue(trident.is(Items.TRIDENT) && trident.getDamageValue() == 0, "a prismarine shard should fully repair a trident");
		ItemStack mace = anvil(player, damaged(new ItemStack(Items.MACE), 100), new ItemStack(Items.BREEZE_ROD)).getSlot(2).getItem();
		helper.assertTrue(mace.is(Items.MACE) && mace.getDamageValue() == 0, "a breeze rod should fully repair a mace");
		helper.assertTrue(anvil(player, damaged(new ItemStack(Items.TRIDENT), 100), new ItemStack(Items.BREEZE_ROD))
				.getSlot(2).getItem().isEmpty(), "a breeze rod shouldn't repair a trident");
		player.discard();
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void grindstoneIsTitledDisenchant(GameTestHelper helper) {
		BlockPos pos = new BlockPos(1, 1, 1);
		helper.setBlock(pos, Blocks.GRINDSTONE);
		BlockPos absolute = helper.absolutePos(pos);
		Component title = helper.getLevel().getBlockState(absolute).getMenuProvider(helper.getLevel(), absolute).getDisplayName();
		helper.assertTrue(title.getContents() instanceof TranslatableContents contents && contents.getKey().equals("container.archive.disenchant"),
				"grindstone title should be Disenchant, got " + title);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void craftingGridDoesntCombineItems(GameTestHelper helper) {
		CraftingInput input = CraftingInput.of(2, 1, List.of(damaged(new ItemStack(Items.IRON_PICKAXE), 100),
				damaged(new ItemStack(Items.IRON_PICKAXE), 100)));
		helper.assertTrue(helper.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel()).isEmpty(),
				"two damaged pickaxes should not combine in the crafting grid");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anvilRejectsCombiningAndBooks(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		helper.assertTrue(anvil(player, damaged(new ItemStack(Items.IRON_PICKAXE), 100), new ItemStack(Items.IRON_PICKAXE))
				.getSlot(2).getItem().isEmpty(), "tool + tool should not combine");
		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		EnchantmentHelper.updateEnchantments(book, m -> m.set(DataTests.enchant(helper, "minecraft", "sharpness"), 1));
		helper.assertTrue(anvil(player, new ItemStack(Items.IRON_SWORD), book).getSlot(2).getItem().isEmpty(),
				"enchanted book should not apply");
		helper.assertTrue(anvil(player, new ItemStack(Items.NETHERITE_PICKAXE), new ItemStack(Items.NETHERITE_INGOT))
				.getSlot(2).getItem().isEmpty(), "unbreakable gear should not be repairable");
		helper.assertTrue(anvil(player, new ItemStack(Items.IRON_PICKAXE), ItemStack.EMPTY)
				.getSlot(2).getItem().isEmpty(), "no change should give no result");
		player.discard();
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anvilRenamesForFree(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		AnvilMenu menu = anvil(player, new ItemStack(Items.DIAMOND_SWORD), ItemStack.EMPTY);
		menu.setItemName("Archivist");
		ItemStack result = menu.getSlot(2).getItem();
		helper.assertTrue(result.has(DataComponents.CUSTOM_NAME) && result.getHoverName().getString().equals("Archivist"),
				"rename should apply");
		helper.assertTrue(menu.getCost() == 0 && menu.getSlot(2).mayPickup(player), "rename should be free");
		player.discard();
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void anvilNeverChips(GameTestHelper helper) {
		BlockState anvil = Blocks.ANVIL.defaultBlockState();
		helper.assertTrue(AnvilBlock.damage(anvil) == anvil, "AnvilBlock.damage should leave the anvil unchanged");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void grindstoneOnlyDisenchants(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		ItemStack sword = new ItemStack(Items.IRON_SWORD);
		EnchantmentHelper.updateEnchantments(sword, m -> {
			m.set(DataTests.enchant(helper, "minecraft", "sharpness"), 3);
			m.set(DataTests.enchant(helper, "minecraft", "vanishing_curse"), 1);
		});

		GrindstoneMenu menu = new GrindstoneMenu(0, player.getInventory(), ContainerLevelAccess.NULL);
		menu.getSlot(0).set(sword.copy());
		ItemStack result = menu.getSlot(2).getItem();
		helper.assertTrue(result.is(Items.IRON_SWORD), "single enchanted item should give a result");
		helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(DataTests.enchant(helper, "minecraft", "sharpness"), result) == 0,
				"sharpness should be removed");
		helper.assertTrue(EnchantmentHelper.getItemEnchantmentLevel(DataTests.enchant(helper, "minecraft", "vanishing_curse"), result) == 1,
				"curse should stay");

		// Default costs 5 + 10 + 15 = 30 levels; 50% of that at 50 XP/level = 750 points.
		int refund = GrindstoneRefund.refundPoints(sword);
		helper.assertTrue(refund == 750, "refund should be 750 points, got " + refund);

		menu.getSlot(1).set(new ItemStack(Items.IRON_SWORD));
		helper.assertTrue(menu.getSlot(2).getItem().isEmpty(), "two items should give no result");

		// One input slot: at the texture's spot, and the old bottom slot can't be seen or filled.
		GrindstoneMenu layout = new GrindstoneMenu(0, player.getInventory(), ContainerLevelAccess.NULL);
		helper.assertTrue(layout.getSlot(0).x == 49 && layout.getSlot(0).y == 30, "input slot should be at 49,30");
		helper.assertTrue(!layout.getSlot(1).isActive() && !layout.getSlot(1).mayPlace(sword), "bottom slot should be disabled");
		layout.getSlot(0).set(sword.copy());
		ItemStack second = sword.copy();
		player.getInventory().setItem(0, second);
		layout.quickMoveStack(player, 30);
		helper.assertTrue(layout.getSlot(1).getItem().isEmpty(), "shift-click shouldn't fill the hidden slot");
		player.getInventory().setItem(0, ItemStack.EMPTY);

		GrindstoneMenu cursedOnly = new GrindstoneMenu(0, player.getInventory(), ContainerLevelAccess.NULL);
		ItemStack cursed = new ItemStack(Items.IRON_SWORD);
		EnchantmentHelper.updateEnchantments(cursed, m -> m.set(DataTests.enchant(helper, "minecraft", "vanishing_curse"), 1));
		cursedOnly.getSlot(0).set(cursed);
		helper.assertTrue(cursedOnly.getSlot(2).getItem().isEmpty(), "curse-only item should give no result");
		player.discard();
		helper.succeed();
	}
}
