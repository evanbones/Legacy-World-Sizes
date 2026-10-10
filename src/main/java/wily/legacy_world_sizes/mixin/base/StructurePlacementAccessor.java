package wily.legacy_world_sizes.mixin.base;

import net.minecraft.core.Vec3i;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Optional;

@Mixin(StructurePlacement.class)
public interface StructurePlacementAccessor {
    @Accessor
    int getSalt();

    @Accessor
    Vec3i getLocateOffset();

    @Accessor
    StructurePlacement.FrequencyReductionMethod getFrequencyReductionMethod();

    @Accessor
    float getFrequency();

    @Accessor
    Optional<StructurePlacement.ExclusionZone> getExclusionZone();
}
