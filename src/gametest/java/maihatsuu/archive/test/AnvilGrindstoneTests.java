package maihatsuu.archive.test;

import maihatsuu.archive.table.GrindstoneRefund;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

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
		// Iron pickaxe: 250 durability, each ingot repairs 62. Two ingots on 200 damage -> 76.
		AnvilMenu menu = anvil(player, damaged(new ItemStack(Items.IRON_PICKAXE), 200), new ItemStack(Items.IRON_INGOT, 2));
		ItemStack result = menu.getSlot(2).getItem();
		helper.assertTrue(result.is(Items.IRON_PICKAXE), "iron pickaxe + iron ingots should repair");
		helper.assertTrue(result.getDamageValue() == 200 - 62 * 2, "two ingots should repair 124, damage now " + result.getDamageValue());
		helper.assertTrue(menu.getCost() == 0, "repair should cost 0 levels, got " + menu.getCost());
		helper.assertTrue(menu.getSlot(2).mayPickup(player), "free result should be pickable");
		player.discard();
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

		GrindstoneMenu cursedOnly = new GrindstoneMenu(0, player.getInventory(), ContainerLevelAccess.NULL);
		ItemStack cursed = new ItemStack(Items.IRON_SWORD);
		EnchantmentHelper.updateEnchantments(cursed, m -> m.set(DataTests.enchant(helper, "minecraft", "vanishing_curse"), 1));
		cursedOnly.getSlot(0).set(cursed);
		helper.assertTrue(cursedOnly.getSlot(2).getItem().isEmpty(), "curse-only item should give no result");
		player.discard();
		helper.succeed();
	}
}
