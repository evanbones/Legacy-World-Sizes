package wily.legacy_world_sizes.mixin.base;

import net.minecraft.world.level.dimension.end.EnderDragonFight;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

//? if >=26.1 {
import java.util.List;
//?} else {
/*import it.unimi.dsi.fastutil.objects.ObjectArrayList;
*///?}

@Mixin(EnderDragonFight.class)
public interface EndDragonFightAccessor {
    @Accessor
    //? if >=26.1 {
    List<Integer> getGateways();
    //?} else {
    /*ObjectArrayList<Integer> getGateways();
    *///?}
}
