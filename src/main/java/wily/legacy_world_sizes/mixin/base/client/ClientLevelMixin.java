package wily.legacy_world_sizes.mixin.base.client;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
//? if <26.1 {
/*import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.function.Supplier;
*///?}
import wily.legacy_world_sizes.util.LevelHolder;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin extends Level {
    //? if >=26.1 {
    protected ClientLevelMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, boolean bl, boolean bl2, long l, int i) {
        super(writableLevelData, resourceKey, registryAccess, holder, bl, bl2, l, i);
    }

    @ModifyExpressionValue(method = "<init>", at = @At(value = "NEW", target = "()Lnet/minecraft/world/level/border/WorldBorder;"))
    private WorldBorder createLevels(WorldBorder original) {
        return LevelHolder.withLevel(original, this);
    }
    //?} else {
    /*protected ClientLevelMixin(WritableLevelData writableLevelData, ResourceKey<Level> resourceKey, RegistryAccess registryAccess, Holder<DimensionType> holder, Supplier<ProfilerFiller> profiler, boolean bl, boolean bl2, long l, int i) {
        super(writableLevelData, resourceKey, registryAccess, holder, profiler, bl, bl2, l, i);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void createLevels(CallbackInfo ci) {
        LevelHolder.withLevel(getWorldBorder(), this);
    }
    *///?}
}
