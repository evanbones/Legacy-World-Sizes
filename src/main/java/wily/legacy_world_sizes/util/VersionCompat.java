package wily.legacy_world_sizes.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.minecraft.world.level.chunk.ProtoChunk;
import net.minecraft.world.level.chunk.UpgradeData;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.blending.BlendingData;

import java.util.Optional;

//? if >=26.1 {
import net.minecraft.world.level.chunk.PalettedContainerFactory;
//?} else {
/*import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Util;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import java.util.stream.IntStream;
*///?}

public class VersionCompat {
    //? if >=26.1 {
    public static final Codec<ChunkPos> CHUNK_POS_CODEC = ChunkPos.CODEC;
    //?} else {
    /*public static final Codec<ChunkPos> CHUNK_POS_CODEC = Codec.INT_STREAM.<ChunkPos>comapFlatMap(input -> Util.fixedSize(input, 2).map(ints -> new ChunkPos(ints[0], ints[1])), pos -> IntStream.of(pos.x, pos.z)).stable();
    *///?}

    public static int getMinY(LevelHeightAccessor height) {
        //? if >=1.21.2 {
        return height.getMinY();
        //?} else {
        /*return height.getMinBuildHeight();
        *///?}
    }

    public static int getMaxY(LevelHeightAccessor height) {
        //? if >=1.21.2 {
        return height.getMaxY();
        //?} else {
        /*return height.getMaxBuildHeight() - 1;
        *///?}
    }

    public static long getSeed(MinecraftServer server) {
        //? if >=26.1 {
        return server.getWorldGenSettings().options().seed();
        //?} else {
        /*return server.getWorldData().worldGenOptions().seed();
        *///?}
    }

    public static Codec<PalettedContainer<BlockState>> blockStatesCodec(RegistryAccess access) {
        //? if >=26.1 {
        return PalettedContainerFactory.create(access).blockStatesContainerCodec();
        //?} else {
        /*return PalettedContainer.codecRW(Block.BLOCK_STATE_REGISTRY, BlockState.CODEC, PalettedContainer.Strategy.SECTION_STATES, Blocks.AIR.defaultBlockState());
        *///?}
    }

    public static Optional<ChunkStatus> getStatus(CompoundTag chunk) {
        //? if >=1.21.5 {
        return chunk.read("Status", ChunkStatus.CODEC);
        //?} else {
        /*return chunk.contains("Status", Tag.TAG_STRING) ? Optional.of(ChunkStatus.byName(chunk.getString("Status"))) : Optional.empty();
        *///?}
    }

    public static Optional<ListTag> getCompoundList(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        return tag.getList(key);
        //?} else {
        /*return tag.contains(key, Tag.TAG_LIST) ? Optional.of(tag.getList(key, Tag.TAG_COMPOUND)) : Optional.empty();
        *///?}
    }

    public static Optional<CompoundTag> getCompound(ListTag list, int index) {
        //? if >=1.21.5 {
        return list.getCompound(index);
        //?} else {
        /*return list.get(index) instanceof CompoundTag compound ? Optional.of(compound) : Optional.empty();
        *///?}
    }

    public static Optional<Integer> getInt(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        return tag.getInt(key);
        //?} else {
        /*return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? Optional.of(tag.getInt(key)) : Optional.empty();
        *///?}
    }

    public static Optional<Byte> getByte(CompoundTag tag, String key) {
        //? if >=1.21.5 {
        return tag.getByte(key);
        //?} else {
        /*return tag.contains(key, Tag.TAG_ANY_NUMERIC) ? Optional.of(tag.getByte(key)) : Optional.empty();
        *///?}
    }

    public static BlendingData createBlendingData(int minSection, int maxSection) {
        //? if >=26.1 {
        return BlendingData.unpack(new BlendingData.Packed(minSection, maxSection, Optional.empty()));
        //?} else {
        /*CompoundTag tag = new CompoundTag();
        tag.putInt("min_section", minSection);
        tag.putInt("max_section", maxSection);
        return BlendingData.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
        *///?}
    }

    public static ProtoChunk createProtoChunk(ServerLevel level, ChunkPos pos) {
        //? if >=26.1 {
        return new ProtoChunk(pos, UpgradeData.EMPTY, level, level.palettedContainerFactory(), null);
        //?} else {
        /*return new ProtoChunk(pos, UpgradeData.EMPTY, level, level.registryAccess().registryOrThrow(Registries.BIOME), null);
        *///?}
    }

    public static LevelChunkSection copySection(LevelChunkSection section) {
        //? if >=1.21.2 {
        return section.copy();
        //?} else {
        /*return new LevelChunkSection(section.getStates().copy(), section.getBiomes().recreate());
        *///?}
    }

    public static void setBlockState(ChunkAccess chunk, BlockPos pos, BlockState state) {
        //? if >=1.21.5 {
        chunk.setBlockState(pos, state);
        //?} else {
        /*chunk.setBlockState(pos, state, false);
        *///?}
    }
}
