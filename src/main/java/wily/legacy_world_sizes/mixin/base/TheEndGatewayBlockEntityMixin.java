package wily.legacy_world_sizes.mixin.base;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.TheEndGatewayBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import wily.legacy_world_sizes.config.LWSWorldOptions;
import wily.legacy_world_sizes.util.LegacyChunkBounds;
import wily.legacy_world_sizes.util.LegacyLevelLimit;

@Mixin(TheEndGatewayBlockEntity.class)
public class TheEndGatewayBlockEntityMixin {
    // Gateways without a known exit search outwards for terrain, which only exists in the outer island bounds, so we send them to the outer island facing them
    @Inject(method = "findOrCreateValidTeleportPos", at = @At("HEAD"), cancellable = true)
    private static void findOrCreateValidTeleportPos(ServerLevel level, BlockPos endGatewayPos, CallbackInfoReturnable<BlockPos> cir) {
        if (level.dimension() != Level.END) return;
        LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(Level.END);
        if (limit == null || limit.bounds().size() < 2 || !limit.bounds().getFirst().isInside(SectionPos.blockToSectionCoord(endGatewayPos.getX()), SectionPos.blockToSectionCoord(endGatewayPos.getZ()))) return;

        ChunkPos center = limit.bounds().getFirst().middle();
        double gatewayAngle = Math.atan2(endGatewayPos.getZ() - center.getMiddleBlockZ(), endGatewayPos.getX() - center.getMiddleBlockX());
        LegacyChunkBounds closest = null;
        double closestAngle = Double.MAX_VALUE;
        for (LegacyChunkBounds bounds : limit.bounds().subList(1, limit.bounds().size())) {
            ChunkPos middle = bounds.middle();
            double angle = Math.atan2(middle.getMiddleBlockZ() - center.getMiddleBlockZ(), middle.getMiddleBlockX() - center.getMiddleBlockX()) - gatewayAngle;
            // Wrapped to [-PI, PI] so angles on both sides of the -X axis compare correctly
            double difference = Math.abs(Math.atan2(Math.sin(angle), Math.cos(angle)));
            if (difference < closestAngle) {
                closest = bounds;
                closestAngle = difference;
            }
        }
        cir.setReturnValue(closest.findOrCreateValidTeleportPos(level));
    }
}
