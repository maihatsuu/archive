package maihatsuu.archive.table;

import maihatsuu.archive.ArchiveConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChiseledBookShelfBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Enchant particles flying from every bookshelf and chiseled bookshelf the table counts (the
 * {@code tableRadius} cube) to the table, so players can see a shelf is in range. Same particle and
 * per-shelf rate as vanilla, which only does this for plain bookshelves in its 5×5 ring.
 */
public final class LibraryParticles {
	public static void animate(Level level, BlockPos table, RandomSource random) {
		int radius = ArchiveConfig.get().tableRadius;
		BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -radius; dy <= radius; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					// Roll first so most positions skip the block lookup.
					if ((dx == 0 && dy == 0 && dz == 0) || random.nextInt(16) != 0) {
						continue;
					}
					BlockState state = level.getBlockState(pos.setWithOffset(table, dx, dy, dz));
					if (!state.is(BlockTags.ENCHANTMENT_POWER_PROVIDER) && !(state.getBlock() instanceof ChiseledBookShelfBlock)) {
						continue;
					}
					// As vanilla: the particle starts at origin + velocity and travels back to the origin above the table.
					level.addParticle(ParticleTypes.ENCHANT,
							table.getX() + 0.5, table.getY() + 2.0, table.getZ() + 0.5,
							dx + random.nextFloat() - 0.5, dy - random.nextFloat() - 1.0F, dz + random.nextFloat() - 0.5);
				}
			}
		}
	}

	private LibraryParticles() {
	}
}
