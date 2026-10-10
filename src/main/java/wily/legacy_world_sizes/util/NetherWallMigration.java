package wily.legacy_world_sizes.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.chunk.storage.RegionFileStorage;
import net.minecraft.world.level.chunk.storage.RegionStorageInfo;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.world.level.levelgen.PositionalRandomFactory;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.storage.LevelResource;
import wily.legacy_world_sizes.mixin.base.RegionFileStorageAccessor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public class NetherWallMigration {
    public static int run(MinecraftServer server, LegacyLevelLimit limit) throws IOException {
        Path path = DimensionType.getStorageFolder(Level.NETHER, server.getWorldPath(LevelResource.ROOT)).resolve("region");
        if (!limit.bedrockBarrier() || !Files.isDirectory(path)) return 0;
        LevelStem stem = server.registryAccess().lookupOrThrow(Registries.LEVEL_STEM).getOrThrow(LevelStem.NETHER).value();
        ChunkGenerator generator = stem.generator();
        DimensionType dimension = stem.type().value();
        LevelHeightAccessor height = LevelHeightAccessor.create(dimension.minY(), dimension.height());
        NoiseGeneratorSettings settings = generator instanceof NoiseBasedChunkGenerator noise ? noise.generatorSettings().value() : NoiseGeneratorSettings.dummy();
        RandomState randomState = RandomState.create(settings, server.registryAccess().lookupOrThrow(Registries.NOISE), VersionCompat.getSeed(server));
        WallReplacement replacement = new WallReplacement(generator, height, settings.defaultBlock(), randomState.getOrCreateRandomFactory(LegacyChunkBounds.BEDROCK_WALLS_RANDOM), VersionCompat.blockStatesCodec(server.registryAccess()));
        RegionStorageInfo info = new RegionStorageInfo(server.getWorldData().getLevelName(), Level.NETHER, "chunk");
        Set<ChunkPos> seen = new LinkedHashSet<>();
        int count = 0;
        try (RegionFileStorage storage = RegionFileStorageAccessor.lws$create(info, path, true)) {
            for (LegacyChunkBounds bounds : limit.bounds()) {
                Set<ChunkPos> chunks = new LinkedHashSet<>();
                OverworldEdgeDeletion.addRing(chunks, bounds, 0);
                for (ChunkPos pos : chunks) {
                    if (!seen.add(pos) || !Files.isRegularFile(OverworldEdgeDeletion.regionPath(path, pos))) continue;
                    CompoundTag chunk = storage.read(pos);
                    if (chunk == null) continue;
                    OverworldEdgeDeletion.validatePosition(chunk, pos);
                    ChunkStatus status = VersionCompat.getStatus(chunk)
                            .orElseThrow(() -> new IOException("Invalid Nether chunk status at " + pos));
                    if (status.isBefore(ChunkStatus.FEATURES) || !replacement.clean(chunk, pos, bounds)) continue;
                    ((RegionFileStorageAccessor) (Object) storage).lws$write(pos, chunk);
                    count++;
                }
            }
            storage.flush();
        }
        OverworldEdgeDeletion.stampBlending(path, info, seen, dimension);
        return count;
    }

    private record WallReplacement(ChunkGenerator generator, LevelHeightAccessor height, BlockState ground, PositionalRandomFactory wallRandom, Codec<PalettedContainer<BlockState>> statesCodec) {
        private boolean clean(CompoundTag chunk, ChunkPos pos, LegacyChunkBounds bounds) throws IOException {
            ListTag sections = VersionCompat.getCompoundList(chunk, "sections").orElseThrow(() -> new IOException("Missing Nether chunk sections at " + pos));
            BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos();
            int floor = Math.max(VersionCompat.getMinY(height), generator.getMinY());
            int roof = generator instanceof FlatLevelSource ? VersionCompat.getMaxY(height) + 1 : Math.min(VersionCompat.getMaxY(height) + 1, generator.getMinY() + generator.getGenDepth());
            boolean changed = false;
            for (int i = 0; i < sections.size(); i++) {
                CompoundTag section = VersionCompat.getCompound(sections, i).orElseThrow(() -> new IOException("Invalid Nether chunk section at " + pos));
                if (!section.contains("block_states")) continue;
                int sectionY = VersionCompat.getByte(section, "Y").orElseThrow(() -> new IOException("Missing Nether section height at " + pos)) * 16;
                if (sectionY < VersionCompat.getMinY(height) || sectionY > VersionCompat.getMaxY(height)) throw new IOException("Invalid Nether section height at " + pos);
                PalettedContainer<BlockState> states = statesCodec.parse(NbtOps.INSTANCE, section.get("block_states")).getOrThrow();
                boolean sectionChanged = false;
                for (int dx = 0; dx < 16; dx++) {
                    int x = pos.getMinBlockX() + dx;
                    int sampleX = Mth.clamp(x, bounds.min().getMinBlockX() + 5, bounds.max().getMinBlockX() - 6) - pos.getMinBlockX();
                    for (int dz = 0; dz < 16; dz++) {
                        int z = pos.getMinBlockZ() + dz;
                        int sampleZ = Mth.clamp(z, bounds.min().getMinBlockZ() + 5, bounds.max().getMinBlockZ() - 6) - pos.getMinBlockZ();
                        for (int dy = 0; dy < 16; dy++) {
                            int y = sectionY + dy;
                            if (y < floor + 5 || y >= roof - 5 && y < roof || !states.get(dx, dy, dz).is(Blocks.BEDROCK)) continue;
                            blockPos.set(x, y, z);
                            int thickness = wallRandom.at(blockPos).nextInt(5);
                            if (x > bounds.min().getMinBlockX() + thickness && z > bounds.min().getMinBlockZ() + thickness && x < bounds.max().getMinBlockX() - 1 - thickness && z < bounds.max().getMinBlockZ() - 1 - thickness) continue;
                            BlockState sample = states.get(sampleX, dy, sampleZ);
                            BlockState state = sample.canOcclude() ? ground : sample.getFluidState().createLegacyBlock();
                            states.set(dx, dy, dz, state);
                            sectionChanged = true;
                        }
                    }
                }
                if (!sectionChanged) continue;
                section.put("block_states", statesCodec.encodeStart(NbtOps.INSTANCE, states).getOrThrow());
                changed = true;
            }
            if (changed) {
                chunk.remove("Heightmaps");
                chunk.putBoolean("isLightOn", false);
                for (int i = 0; i < sections.size(); i++) {
                    CompoundTag section = VersionCompat.getCompound(sections, i).orElseGet(CompoundTag::new);
                    section.remove("BlockLight");
                    section.remove("SkyLight");
                }
            }
            return changed;
        }
    }
}
