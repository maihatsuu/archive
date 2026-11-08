package maihatsuu.archive.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import maihatsuu.archive.ArchiveConfig;
import maihatsuu.archive.ConfigSyncPayload;
import maihatsuu.archive.GearComponents;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class ConfigSyncTests implements FabricGameTest {
	/**
	 * A server config replaces the local one (except display settings) until cleared, and rebuilding items makes
	 * durability, unbreakable and tool speeds follow it, then go back. This is what a client does on join and leave.
	 */
	@GameTest(template = EMPTY_STRUCTURE)
	public void serverConfigOverridesLocal(GameTestHelper helper) {
		ArchiveConfig local = ArchiveConfig.local();
		JsonObject json = JsonParser.parseString(local.toJson()).getAsJsonObject();
		json.addProperty("xpPerLevel", local.xpPerLevel + 7);
		json.addProperty("tableShowLocked", !local.tableShowLocked);
		json.addProperty("diamondSpeed", 33.0F);
		JsonObject durability = new JsonObject();
		durability.addProperty("minecraft:diamond_sword", 77);
		durability.addProperty("minecraft:netherite_sword", 100);
		durability.addProperty("minecraft:iron_sword", 0);
		json.add("durabilityOverrides", durability);

		var buf = Unpooled.buffer();
		ConfigSyncPayload.STREAM_CODEC.encode(buf, new ConfigSyncPayload(json.toString()));
		String received = ConfigSyncPayload.STREAM_CODEC.decode(buf).json();
		try {
			ArchiveConfig.applyServer(received);
			GearComponents.rebuild(ArchiveConfig.get());
			helper.assertTrue(ArchiveConfig.get().xpPerLevel == local.xpPerLevel + 7, "server xpPerLevel should apply");
			helper.assertTrue(ArchiveConfig.get().tableShowLocked == local.tableShowLocked, "display setting should stay local");
			helper.assertTrue(new ItemStack(Items.DIAMOND_SWORD).getMaxDamage() == 77, "server durability should apply");
			helper.assertTrue(new ItemStack(Items.NETHERITE_SWORD).isDamageableItem(), "netherite set to 100 should break");
			helper.assertTrue(!new ItemStack(Items.IRON_SWORD).isDamageableItem()
					&& new ItemStack(Items.IRON_SWORD).has(DataComponents.UNBREAKABLE), "iron set to 0 should be unbreakable");
			float speed = new ItemStack(Items.DIAMOND_PICKAXE).getDestroySpeed(Blocks.STONE.defaultBlockState());
			helper.assertTrue(speed == 33.0F, "server tool speed should apply, got " + speed);
		} finally {
			ArchiveConfig.applyServer(null);
			GearComponents.rebuild(ArchiveConfig.get());
		}
		helper.assertTrue(ArchiveConfig.get() == local, "local config should be back after disconnect");
		helper.assertTrue(new ItemStack(Items.DIAMOND_SWORD).getMaxDamage() == 2048, "local durability should be back");
		helper.assertTrue(!new ItemStack(Items.NETHERITE_SWORD).isDamageableItem(), "netherite should be unbreakable again");
		helper.assertTrue(new ItemStack(Items.IRON_SWORD).isDamageableItem() && new ItemStack(Items.IRON_SWORD).getMaxDamage() == 512,
				"iron should break again");
		helper.assertTrue(new ItemStack(Items.DIAMOND_PICKAXE).getDestroySpeed(Blocks.STONE.defaultBlockState()) == local.diamondSpeed,
				"local tool speed should be back");
		helper.succeed();
	}
}
