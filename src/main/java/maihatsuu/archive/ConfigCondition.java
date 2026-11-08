package maihatsuu.archive;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.core.HolderLookup;
import org.jetbrains.annotations.Nullable;

/**
 * Fabric resource condition {@code {"condition": "archive:config", "option": "resilience"}}: true when that
 * boolean in {@link ArchiveConfig} is on. Used by the {@code no_resilience} overlay in {@code pack.mcmeta} (wrapped in
 * {@code fabric:not}), which makes Resilience unobtainable and fire, blast and projectile protection obtainable.
 */
public record ConfigCondition(String option) implements ResourceCondition {
	public static final ResourceConditionType<ConfigCondition> TYPE = ResourceConditionType.create(Archive.id("config"),
			Codec.STRING.fieldOf("option").xmap(ConfigCondition::new, ConfigCondition::option));

	static void register() {
		ResourceConditions.register(TYPE);
	}

	@Override
	public ResourceConditionType<?> getType() {
		return TYPE;
	}

	@Override
	public boolean test(@Nullable HolderLookup.Provider registryLookup) {
		try {
			return ArchiveConfig.class.getField(this.option).getBoolean(ArchiveConfig.get());
		} catch (ReflectiveOperationException | IllegalArgumentException e) {
			Archive.LOGGER.error("archive:config condition: '{}' is not a boolean config option", this.option);
			return false;
		}
	}
}
