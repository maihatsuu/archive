package maihatsuu.archive.test;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/** Phase 3: unbreakable netherite gear, tool speeds, block hardness. */
public class GearTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void rareGearIsUnbreakable(GameTestHelper helper) {
		for (ItemStack stack : new ItemStack[]{new ItemStack(Items.NETHERITE_PICKAXE), new ItemStack(Items.NETHERITE_CHESTPLATE)}) {
			helper.assertTrue(stack.has(DataComponents.UNBREAKABLE), stack.getItem() + " should be unbreakable");
			helper.assertTrue(!stack.isDamageableItem(), stack.getItem() + " should not take damage");
		}
		for (ItemStack stack : new ItemStack[]{new ItemStack(Items.DIAMOND_PICKAXE), new ItemStack(Items.TRIDENT), new ItemStack(Items.MACE)}) {
			helper.assertTrue(stack.isDamageableItem(), stack.getItem() + " should take damage");
		}
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void durabilityTable(GameTestHelper helper) {
		assertDurability(helper, Items.WOODEN_SWORD, 32);
		assertDurability(helper, Items.IRON_PICKAXE, 512);
		assertDurability(helper, Items.DIAMOND_CHESTPLATE, 2048);
		assertDurability(helper, Items.ELYTRA, 2048);
		assertDurability(helper, Items.TRIDENT, 2048);
		assertDurability(helper, Items.WARPED_FUNGUS_ON_A_STICK, 256);
		helper.succeed();
	}

	private static void assertDurability(GameTestHelper helper, net.minecraft.world.item.Item item, int expected) {
		int actual = new ItemStack(item).getMaxDamage();
		helper.assertTrue(actual == expected, item + " durability should be " + expected + ", got " + actual);
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void toolSpeeds(GameTestHelper helper) {
		float diamond = new ItemStack(Items.DIAMOND_PICKAXE).getDestroySpeed(Blocks.STONE.defaultBlockState());
		float netherite = new ItemStack(Items.NETHERITE_PICKAXE).getDestroySpeed(Blocks.STONE.defaultBlockState());
		float iron = new ItemStack(Items.IRON_PICKAXE).getDestroySpeed(Blocks.STONE.defaultBlockState());
		helper.assertTrue(diamond == 12.0F, "diamond speed should be 12, got " + diamond);
		helper.assertTrue(netherite == 19.0F, "netherite speed should be 19, got " + netherite);
		helper.assertTrue(iron == 6.0F, "iron speed should be vanilla 6, got " + iron);
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void blockHardness(GameTestHelper helper) {
		assertHardness(helper, Blocks.DEEPSLATE, 2.0F);
		assertHardness(helper, Blocks.COBBLED_DEEPSLATE_STAIRS, 2.0F);
		assertHardness(helper, Blocks.DEEPSLATE_DIAMOND_ORE, 3.5F);
		assertHardness(helper, Blocks.DEEPSLATE_COAL_ORE, 3.5F);
		assertHardness(helper, Blocks.OBSIDIAN, 20.0F);
		assertHardness(helper, Blocks.CRYING_OBSIDIAN, 20.0F);
		assertHardness(helper, Blocks.RESPAWN_ANCHOR, 20.0F);
		assertHardness(helper, Blocks.ENDER_CHEST, 20.0F);
		assertHardness(helper, Blocks.REINFORCED_DEEPSLATE, 55.0F);
		assertHardness(helper, Blocks.STONE, 1.5F);
		helper.assertTrue(Blocks.OBSIDIAN.getExplosionResistance() == 1200.0F, "obsidian blast resistance should be unchanged");
		helper.succeed();
	}

	private static void assertHardness(GameTestHelper helper, Block block, float expected) {
		float actual = block.defaultBlockState().getDestroySpeed(helper.getLevel(), BlockPos.ZERO);
		helper.assertTrue(actual == expected, block + " hardness should be " + expected + ", got " + actual);
	}
}
