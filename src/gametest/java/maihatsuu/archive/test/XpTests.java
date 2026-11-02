package maihatsuu.archive.test;

import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.projectile.ThrownExperienceBottle;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Phase 2: flat XP curve, level cap, death XP, experience bottle. */
public class XpTests implements FabricGameTest {
	@GameTest(template = EMPTY_STRUCTURE)
	public void fiftyPointsIsOneLevel(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		helper.assertTrue(player.getXpNeededForNextLevel() == 50, "level 0 should need 50 XP");
		player.giveExperiencePoints(50);
		helper.assertTrue(player.experienceLevel == 1, "50 points should give level 1, got " + player.experienceLevel);
		player.giveExperienceLevels(40);
		helper.assertTrue(player.getXpNeededForNextLevel() == 50, "level 41 should still need 50 XP");
		player.discard();
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void levelsAreCappedAtOneHundred(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.giveExperienceLevels(150);
		helper.assertTrue(player.experienceLevel == 100, "giveExperienceLevels should cap at 100, got " + player.experienceLevel);
		helper.assertTrue(player.experienceProgress == 1.0F, "bar should be full at the cap");
		player.giveExperiencePoints(500);
		helper.assertTrue(player.experienceLevel == 100, "extra points should be discarded at the cap");

		player.setExperienceLevels(0);
		player.experienceProgress = 0;
		player.giveExperiencePoints(99 * 50 + 25);
		helper.assertTrue(player.experienceLevel == 99, "4975 points should be level 99, got " + player.experienceLevel);
		player.giveExperiencePoints(10_000);
		helper.assertTrue(player.experienceLevel == 100, "points past the cap should stop at 100");

		player.setExperienceLevels(250);
		helper.assertTrue(player.experienceLevel == 100, "setExperienceLevels should cap at 100");
		player.discard();
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void deathDropsHalfOfTotalXp(GameTestHelper helper) {
		ServerPlayer player = helper.makeMockServerPlayerInLevel();
		player.giveExperienceLevels(10);
		player.giveExperiencePoints(20);
		// 10 levels * 50 + 20 points = 520 points; half = 260.
		int reward = player.getExperienceReward(helper.getLevel(), null);
		helper.assertTrue(reward == 260, "death reward should be 260, got " + reward);
		player.discard();
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void bottleGivesOneLevel(GameTestHelper helper) {
		helper.setBlock(new BlockPos(1, 0, 1), Blocks.STONE);
		Vec3 pos = helper.absoluteVec(new Vec3(1.5, 3, 1.5));
		ThrownExperienceBottle bottle = new ThrownExperienceBottle(helper.getLevel(), pos.x, pos.y, pos.z);
		bottle.setDeltaMovement(0, -0.5, 0);
		helper.getLevel().addFreshEntity(bottle);
		helper.succeedWhen(() -> {
			int total = helper.getEntities(EntityType.EXPERIENCE_ORB).stream().mapToInt(ExperienceOrb::getValue).sum();
			helper.assertTrue(total == 50, "bottle orbs should total 50, got " + total);
		});
	}
}
