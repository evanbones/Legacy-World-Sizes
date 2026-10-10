package wily.legacy_world_sizes.config;

import com.google.common.collect.*;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import net.minecraft.util.Util;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.dedicated.DedicatedServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.feature.EndSpikeFeature;
import net.minecraft.world.level.levelgen.feature.SpikeFeature;
import net.minecraft.world.level.levelgen.structure.BuiltinStructureSets;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.placement.ConcentricRingsStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import wily.factoryapi.FactoryAPI;
import wily.factoryapi.base.config.FactoryConfig;
import wily.factoryapi.base.config.FactoryConfigControl;
import wily.factoryapi.base.config.FactoryConfigDisplay;
import wily.factoryapi.util.DynamicUtil;
import wily.legacy_world_sizes.LegacyWorldSizes;
import wily.legacy_world_sizes.mixin.base.*;
import wily.legacy_world_sizes.util.*;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

public class LWSWorldOptions {
    public static final FactoryConfig.StorageHandler WORLD_STORAGE = new FactoryConfig.StorageHandler(true) {
        @Override
        public void save() {
            if (file == null) return;
            super.save();
        }

        @Override
        public <T> DataResult<T> decode(FactoryConfigControl<T> control, Consumer<T> setter, Dynamic<?> dynamic) {
            return super.decode(control, setter, convertToRegistryIfPossible(dynamic));
        }

        @Override
        public <T, E> DataResult<E> encode(FactoryConfigControl<T> control, T value, DynamicOps<E> ops) {
            return super.encode(control, value, createRegistryOps(ops));
        }

        @Override
        public <T> void decodeConfigs(Dynamic<T> dynamic) {
            super.decodeConfigs(DynamicUtil.convertToRegistryIfPossible(dynamic));
        }

        @Override
        public <T> T encodeConfigs(DynamicOps<T> ops) {
            return super.encodeConfigs(createRegistryOps(ops));
        }
    };

    public static <T> Dynamic<T> convertToRegistryIfPossible(Dynamic<T> dynamic) {
        return dynamic.convert(createRegistryOps(dynamic.getOps()));
    }

    public static <T> DynamicOps<T> createRegistryOps(DynamicOps<T> ops) {
        MinecraftServer server = FactoryAPI.currentServer;
        return server == null ? ops : RegistryOps.create(ops, server.registryAccess());
    }

    public static final List<EndSpikeFeature.EndSpike> LEGACY_END_SPIKES = createLegacyEndSpikes(8);

    public static <T> FactoryConfig<T> buildAndRegister(UnaryOperator<FactoryConfig.Builder<T>> consumer, FactoryConfigDisplay.Builder<T> builder) {
        return consumer.apply(new FactoryConfig.Builder<>()).displayFromKey(t -> builder.build(LWSComponents.optionName(t))).buildAndRegister(WORLD_STORAGE);
    }

    public static <T> FactoryConfig<T> buildAndRegister(UnaryOperator<FactoryConfig.Builder<T>> consumer) {
        return buildAndRegister(consumer, FactoryConfigDisplay.builder());
    }

    public static final FactoryConfig<Map<ResourceKey<Level>, LegacyLevelLimit>> legacyLevelLimits = buildAndRegister(b -> b.key("legacyLevelLimits").control(() -> LegacyLevelLimit.MAP_CODEC).defaultValue(Collections.emptyMap()));

    public static final FactoryConfig<Integer> maxEndGateways = buildAndRegister(b -> b.key("maxEndGateways").control(FactoryConfigControl.of(Codec.INT)).defaultValue(20));

    public static final FactoryConfig<Integer> endOuterIslandsRay = buildAndRegister(b -> b.key("endOuterIslandsRay").control(FactoryConfigControl.of(Codec.INT)).defaultValue(64));

    public static final FactoryConfig<BlockPos> endSpawnPoint = buildAndRegister(b -> b.key("endSpawnPoint").control(FactoryConfigControl.of(BlockPos.CODEC)).defaultValue(ServerLevel.END_SPAWN_POINT));

    public static final FactoryConfig<Boolean> legacyEndSpikes = buildAndRegister(b -> b.key("legacyEndSpikes").control(FactoryConfigControl.of(Codec.BOOL)).defaultValue(false));

    public static final FactoryConfig<Boolean> balancedSeed = buildAndRegister(b -> b.key("balancedSeed").control(FactoryConfigControl.TOGGLE).defaultValue(true), FactoryConfigDisplay.toggleBuilder().tooltip(LWSComponents.staticTooltip(LWSComponents.optionName("balancedSeed.description"))));

    public static final FactoryConfig<LegacyWorldSize> legacyWorldSize = buildAndRegister(b -> b.key("legacyWorldSize").control(new FactoryConfigControl.FromInt<>(LegacyWorldSize.CODEC, LegacyWorldSize.map::getByIndex, LegacyWorldSize.map::indexOf, LegacyWorldSize.map::size)).defaultValue(LegacyWorldSize.CUSTOM), FactoryConfigDisplay.<LegacyWorldSize>builder().valueToComponent(LegacyWorldSize::name).tooltip(LWSComponents.staticTooltip(LWSComponents.optionName("legacyWorldSize.description"))));

    public static final FactoryConfig<LegacyWorldSize> appliedLegacyWorldSize = buildAndRegister(b -> b.key("appliedLegacyWorldSize").control(new FactoryConfigControl.FromInt<>(LegacyWorldSize.CODEC, LegacyWorldSize.map::getByIndex, LegacyWorldSize.map::indexOf, LegacyWorldSize.map::size)).defaultValue(LegacyWorldSize.CUSTOM));

    public static final FactoryConfig<Boolean> overwriteWorldEdge = buildAndRegister(b -> b.key("overwriteWorldEdge").control(FactoryConfigControl.TOGGLE).defaultValue(false), FactoryConfigDisplay.toggleBuilder().tooltip(LWSComponents.staticTooltip(LWSComponents.optionName("overwriteWorldEdge.description"))));

    public static final FactoryConfig<LegacyBiomeScale.AddOctave> addToBiomeFirstOctave = buildAndRegister(b -> b.key("addToBiomeFirstOctave").control(FactoryConfigControl.of(LegacyBiomeScale.AddOctave.CODEC)).defaultValue(LegacyBiomeScale.AddOctave.ZERO));

    public static final FactoryConfig<LegacyBiomeScale> legacyBiomeScale = buildAndRegister(b -> b.key("legacyBiomeScale").control(new FactoryConfigControl.FromInt<>(LegacyBiomeScale.CODEC, LegacyBiomeScale.map::getByIndex, LegacyBiomeScale.map::indexOf, LegacyBiomeScale.map::size)).defaultValue(LegacyBiomeScale.CUSTOM), FactoryConfigDisplay.<LegacyBiomeScale>builder().valueToComponent(LegacyBiomeScale::name).tooltip(LWSComponents.staticTooltip(LWSComponents.optionName("legacyBiomeScale.description"))));


    public static boolean isValidChunk(LevelChunk chunk) {
        return isValidPos(chunk.getLevel().dimension(), chunk.getPos());
    }

    public static boolean isValidChunk(LevelHeightAccessor accessor, ChunkAccess access) {
        if (accessor instanceof Level level) {
            return isValidPos(level.dimension(), access.getPos());
        }
        return true;
    }

    public static boolean isValidChunk(StructureManager manager, ChunkAccess access) {
        if ((((StructureManagerAccessor) manager).getLevel() instanceof WorldGenRegion level)) {
            return isValidPos(level.getLevel().dimension(), access.getPos());
        }
        return true;
    }

    public static boolean isValidPos(ResourceKey<Level> level, int x, int z) {
        LegacyLevelLimit limit = legacyLevelLimits.get().get(level);

        if (limit != null && !limit.bounds().isEmpty()) {
            for (LegacyChunkBounds bounds : limit.bounds()) {
                if (bounds.isInside(x, z)) {
                    return true;
                }
            }

            return false;
        }
        return true;
    }

    public static boolean isValidPos(ResourceKey<Level> level, ChunkPos pos) {
        return isValidPos(level, pos.x(), pos.z());
    }

    public static boolean isValidPos(ResourceKey<Level> level, BlockPos pos) {
        return isValidPos(level, SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    public static double getTeleportationScale(ResourceKey<Level> from, ResourceKey<Level> to, double original) {
        LegacyLevelLimit limitFrom = legacyLevelLimits.get().get(from);
        LegacyLevelLimit limitTo = legacyLevelLimits.get().get(to);

        if (limitFrom == null || limitTo == null) return original;

        LegacyChunkBounds boundsFrom = limitFrom.bounds().get(0);
        LegacyChunkBounds boundsTo = limitTo.bounds().get(0);

        double hypFrom = boundsFrom.hyp();
        double hypTo = boundsTo.hyp();

        return hypTo < hypFrom ? Math.max(1d / Math.round(hypFrom / hypTo), original) : Math.min(Math.round(hypTo / hypFrom), original);
    }

    public static void restoreChangedDefaults() {
        balancedSeed.setDefault(LWSCommonOptions.balancedSeed.get());
        legacyWorldSize.setDefault(LWSCommonOptions.legacyWorldSize.get());
        legacyBiomeScale.setDefault(LWSCommonOptions.legacyBiomeScale.get());
        balancedSeed.reset();
        legacyWorldSize.reset();
        legacyBiomeScale.reset();
    }

    public static void setupLegacyWorldSize(RegistryAccess access) {
        setupLegacyWorldSize(access, false);
    }

    public static void setupLegacyWorldSize(RegistryAccess access, boolean preserveEnd) {
        LegacyLevelLimit end = preserveEnd ? legacyLevelLimits.get().get(Level.END) : null;
        int gateways = maxEndGateways.get();
        int islandsRay = endOuterIslandsRay.get();
        BlockPos spawnPoint = endSpawnPoint.get();
        boolean spikes = legacyEndSpikes.get();
        legacyWorldSize.get().applier().accept(new LegacyWorldSize.ApplyContext(access));
        if (end != null) {
            legacyLevelLimits.set(ImmutableMap.<ResourceKey<Level>, LegacyLevelLimit>builder().putAll(legacyLevelLimits.get()).put(Level.END, end).buildKeepingLast());
            maxEndGateways.set(gateways);
            endOuterIslandsRay.set(islandsRay);
            endSpawnPoint.set(spawnPoint);
            legacyEndSpikes.set(spikes);
        }
        legacyBiomeScale.get().applyToAddToBiomeFirstOctave();
        addToBiomeFirstOctave.get().applyToBiomeNoiseParameters(access);
    }

    public static void setupDedicatedServerBalancedSeed(DedicatedServer server) {
        if (balancedSeed.get() && server.getProperties() instanceof SettingsAccessor settings && WorldOptions.parseSeed(settings.getString("level-seed", "")).isEmpty()) {
            LegacyLevelLimit limit = LWSWorldOptions.legacyLevelLimits.get().get(Level.OVERWORLD);
            if (limit != null && server.getProperties() instanceof DedicatedServerPropertiesAccessor accessor) {
                LegacyChunkBounds bounds = limit.bounds().get(0);
                accessor.setWorldOptions(server.getProperties().worldOptions.withSeed(OptionalLong.of(bounds.findBalancedSeed(server.registryAccess(), 100))));
            }
        }
    }

    public static void setupEndLimits() {
        int max = maxEndGateways.get();
        LegacyLevelLimit limit = legacyLevelLimits.get().get(Level.END);

        if (limit != null && !limit.bounds().isEmpty() && (limit.bounds().size() - 1) != max) {
            ImmutableList.Builder<LegacyChunkBounds> bounds = ImmutableList.builder();
            LegacyChunkBounds chunkBounds = limit.bounds().get(0);
            bounds.add(chunkBounds);

            if (max > 0) {
                int ray = Math.max(endOuterIslandsRay.get(), Math.round((11.28f + (float) chunkBounds.hyp()) * max / Mth.TWO_PI));
                LegacyWorldSizes.LOGGER.debug("The end outer islands ray is: {}", ray);
                if (ray != endOuterIslandsRay.get()) {
                    LegacyWorldSizes.LOGGER.debug("Adjusting saved end outer islands ray from {} to {}", endOuterIslandsRay.get(), ray);
                    endOuterIslandsRay.set(ray);
                }
                for (int i = 0; i < max; i++) {
                    double dist = 2.0 * Math.PI * i / max;
                    int x = Math.round(ray * (float) Math.cos(dist));
                    int z = Math.round(ray * (float) Math.sin(dist));
                    LegacyWorldSizes.LOGGER.debug("Moving end bounds {} to: {}, {}", i, x, z);
                    bounds.add(chunkBounds.moveTo(x, z));
                }
            }
            legacyLevelLimits.set(ImmutableMap.<ResourceKey<Level>, LegacyLevelLimit>builder().putAll(legacyLevelLimits.get()).put(Level.END, limit.withBounds(bounds.build())).buildKeepingLast());
            legacyLevelLimits.save();
        }
    }

    public static void setupMaxEndGateways(MinecraftServer server) {
        ServerLevel end = server.getLevel(Level.END);
        int max = LWSWorldOptions.maxEndGateways.get();
        if (max != 20 && end != null && end.getDragonFight() instanceof EndDragonFightAccessor accessor) {
            // The dragon fight always fills the vanilla 20 gateways when it's created, so they can only be safely replaced before any of them has spawned
            if (!end.getDragonFight().hasPreviouslyKilledDragon()) {
                accessor.getGateways().clear();
                accessor.getGateways().addAll(ContiguousSet.create(Range.closedOpen(0, max), DiscreteDomain.integers()));
                if (max > 1) Util.shuffle(accessor.getGateways(), RandomSource.create(end.getSeed()));
            } else {
                // Worlds that already spawned some gateways keep their order, without the ones that don't fit in this size
                accessor.getGateways().removeIf(i -> i >= max);
            }
        }
    }

    public static final List<ResourceKey<StructureSet>> LIMITED_OVERWORLD_STRUCTURE_SETS = List.of(BuiltinStructureSets.VILLAGES, BuiltinStructureSets.PILLAGER_OUTPOSTS, BuiltinStructureSets.DESERT_PYRAMIDS, BuiltinStructureSets.JUNGLE_TEMPLES, BuiltinStructureSets.RUINED_PORTALS, BuiltinStructureSets.WOODLAND_MANSIONS);

    public static void setupValidPlacements(Level level, ChunkGeneratorStructureState genState) {
        LegacyLevelLimit limit = legacyLevelLimits.get().get(level.dimension());
        // Exclusion zones have to check the replaced placements instead of the vanilla ones, so those are set up first
        Map<ResourceKey<StructureSet>, Holder<StructureSet>> replacedSets = new HashMap<>();
        if (level.dimension() == Level.OVERWORLD) {
            for (ResourceKey<StructureSet> key : LIMITED_OVERWORLD_STRUCTURE_SETS) {
                setupStructureValidPlacement(limit, level, genState, key, false, replacedSets);
            }
            setupConcentricStructureValidPlacement(limit, level, genState, BuiltinStructureSets.STRONGHOLDS, level.registryAccess().lookupOrThrow(Registries.BIOME).getOrThrow(BiomeTags.HAS_STRONGHOLD));
        } else if (level.dimension() == Level.NETHER) {
            setupStructureValidPlacement(limit, level, genState, BuiltinStructureSets.NETHER_COMPLEXES, true, replacedSets);
        }
        setupEndCitiesValidPlacement(level, genState);
    }

    public static void setupEndCitiesValidPlacement(Level level, ChunkGeneratorStructureState genState) {
        LegacyLevelLimit limit = legacyLevelLimits.get().get(Level.END);

        if (limit != null && level.dimension() == Level.END && genState instanceof ChunkGeneratorStructureStateAccessor accessor) {
            // The vanilla End Cities would generate on top of the ones placed in each outer island
            ImmutableList.Builder<Holder<StructureSet>> structures = ImmutableList.<Holder<StructureSet>>builder().addAll(genState.possibleStructureSets().stream().filter(set -> !set.is(BuiltinStructureSets.END_CITIES)).toList());
            List<ChunkPos> validPositions = new ArrayList<>();
            RandomSource random = RandomSource.create(genState.getLevelSeed());
            for (int i = 1; i < limit.bounds().size(); i++) {
                if (i > 1 && random.nextBoolean()) continue;
                validPositions.add(limit.bounds().get(i).middle());
            }
            structures.add(Holder.direct(new StructureSet(level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getOrThrow(BuiltinStructureSets.END_CITIES).value().structures(), new RandomSpreadStructurePlacement(1, 1, RandomSpreadType.LINEAR, 10387313) {
                @Override
                public ChunkPos getPotentialStructureChunk(long l, int i, int j) {
                    for (ChunkPos validPosition : validPositions) {
                        if (validPosition.x() == i && validPosition.z() == j) return validPosition;
                    }
                    return validPositions.getFirst();
                }
            })));
            accessor.setHasGeneratedPositions(false);
            accessor.getPlacementsForStructure().clear();

            accessor.setPossibleStructureSets(structures.build());
        }
    }

    public static void setupStructureValidPlacement(LegacyLevelLimit limit, Level level, ChunkGeneratorStructureState genState, ResourceKey<StructureSet> key, boolean alwaysPlace, Map<ResourceKey<StructureSet>, Holder<StructureSet>> replacedSets) {
        if (limit != null && !limit.bounds().isEmpty() && genState instanceof ChunkGeneratorStructureStateAccessor accessor) {
            LegacyChunkBounds bound = limit.bounds().getFirst();
            int width = (bound.max().x() - bound.min().x());
            int depth = (bound.max().z() - bound.min().z());
            int d = Math.min(width, depth) / 6;
            Holder<StructureSet> structureSet = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getOrThrow(key);
            List<Holder<StructureSet>> structureSets = new ArrayList<>(genState.possibleStructureSets());
            if (structureSet.value().placement() instanceof RandomSpreadStructurePlacement old && structureSets.remove(structureSet)) {
                StructurePlacementAccessor oldAccessor = (StructurePlacementAccessor) old;
                Optional<StructurePlacement.ExclusionZone> exclusionZone = oldAccessor.getExclusionZone().map(zone -> zone.otherSet().unwrapKey().map(replacedSets::get).map(set -> new StructurePlacement.ExclusionZone(set, zone.chunkCount())).orElse(zone));
                int separation = Math.max(old.separation(), old.spacing() - d);
                int distanceFromBorder = limit.heightFallOff() ? 4 : 0;
                // Structures split from the same set are placed together, so they also keep their separation from each other
                LimitedRandomStructurePlacement.Group group = new LimitedRandomStructurePlacement.Group();
                if (alwaysPlace) {
                    for (StructureSet.StructureSelectionEntry structure : structureSet.value().structures()) {
                        structureSets.add(Holder.direct(new StructureSet(List.of(structure), new LimitedRandomStructurePlacement(old, separation, oldAccessor.getSalt() / structure.weight(), exclusionZone, List.of(structure), genState, bound, distanceFromBorder, group))));
                    }
                } else {
                    Holder<StructureSet> replaced = Holder.direct(new StructureSet(structureSet.value().structures(), new LimitedRandomStructurePlacement(old, separation, oldAccessor.getSalt(), exclusionZone, structureSet.value().structures(), genState, bound, distanceFromBorder, group)));
                    structureSets.add(replaced);
                    replacedSets.put(key, replaced);
                }
            }
            accessor.setHasGeneratedPositions(false);
            accessor.getPlacementsForStructure().clear();
            accessor.setPossibleStructureSets(ImmutableList.<Holder<StructureSet>>builder().addAll(structureSets).build());
        }
    }

    public static void setupConcentricStructureValidPlacement(LegacyLevelLimit limit, Level level, ChunkGeneratorStructureState genState, ResourceKey<StructureSet> key, HolderSet<Biome> biomes) {
        if (limit != null && genState instanceof ChunkGeneratorStructureStateAccessor accessor) {
            for (LegacyChunkBounds bound : limit.bounds()) {
                Holder<StructureSet> structureSet = level.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET).getOrThrow(key);
                List<Holder<StructureSet>> structures = new ArrayList<>(genState.possibleStructureSets());
                if (structureSet.value().placement() instanceof ConcentricRingsStructurePlacement old && structures.remove(structureSet)) {
                    int width = (bound.max().x() - bound.min().x());
                    int depth = (bound.max().z() - bound.min().z());
                    int d = Math.min(width, depth);

                    structures.add(Holder.direct(new StructureSet(structureSet.value().structures(), new ConcentricRingsStructurePlacement(Math.min(old.distance(), d / 12), old.spread(), Math.min(old.count(), Math.max(d / 52, 1)), biomes))));
                    accessor.setHasGeneratedPositions(false);
                    accessor.getPlacementsForStructure().clear();
                    accessor.setPossibleStructureSets(ImmutableList.<Holder<StructureSet>>builder().addAll(structures).build());
                }
                return;
            }
        }
    }

    public static List<EndSpikeFeature.EndSpike> createLegacyEndSpikes(int amount) {
        ImmutableList.Builder<EndSpikeFeature.EndSpike> builder = ImmutableList.builder();

        for (int i = 0; i < amount; i++) {
            double ang = 2.0 * (-Math.PI + (Math.PI / amount) * i);
            int x = Mth.floor(42.0 * Math.cos(ang));
            int z = Mth.floor(42.0 * Math.sin(ang));
            int width = 2 + i / 3;
            int height = 73 + i * 3;
            builder.add(new EndSpikeFeature.EndSpike(x, z, width, height, i >= amount - 2));
        }

        return builder.build();
    }

    public static class LimitedRandomStructurePlacement extends RandomSpreadStructurePlacement {
        private final List<StructureSet.StructureSelectionEntry> structures;
        private final ChunkGeneratorStructureState structureState;
        private final LegacyChunkBounds bounds;
        private final int distanceFromBorder;
        private final int minSeparation;
        private final Group group;
        private Map<ChunkPos, ChunkPos> cellPositions = Map.of();
        private Set<ChunkPos> structureChunks = Set.of();

        public LimitedRandomStructurePlacement(RandomSpreadStructurePlacement vanilla, int separation, int salt, Optional<StructurePlacement.ExclusionZone> exclusionZone, List<StructureSet.StructureSelectionEntry> structures, ChunkGeneratorStructureState structureState, LegacyChunkBounds bounds, int distanceFromBorder, Group group) {
            super(((StructurePlacementAccessor) vanilla).getLocateOffset(), ((StructurePlacementAccessor) vanilla).getFrequencyReductionMethod(), ((StructurePlacementAccessor) vanilla).getFrequency(), salt, exclusionZone, vanilla.spacing(), separation, vanilla.spreadType());
            this.structures = structures;
            this.structureState = structureState;
            this.bounds = bounds;
            this.distanceFromBorder = distanceFromBorder;
            this.minSeparation = vanilla.separation();
            this.group = group;
            group.placements.add(this);
        }

        private void generateValidPositions(long seed, List<ChunkPos> occupied) {
            Map<ChunkPos, ChunkPos> cellPositions = new HashMap<>();
            Set<ChunkPos> structureChunks = new HashSet<>();
            int minX = Math.floorDiv(bounds.min().x(), spacing());
            int minZ = Math.floorDiv(bounds.min().z(), spacing());
            int maxX = Math.floorDiv(bounds.max().x(), spacing());
            int maxZ = Math.floorDiv(bounds.max().z(), spacing());
            int totalWeight = 0;

            for (StructureSet.StructureSelectionEntry entry : structures) {
                totalWeight += entry.weight();
            }

            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    WorldgenRandom worldgenRandom = new WorldgenRandom(new LegacyRandomSource(0L));
                    worldgenRandom.setLargeFeatureWithSalt(seed, x, z, this.salt());
                    RandomSource fork = worldgenRandom.fork();
                    List<StructureSet.StructureSelectionEntry> actualStructures = new ArrayList<>(structures);
                    int actualWeight = totalWeight;
                    int n = spacing() - separation();
                    ChunkPos pos = null;
                    ChunkPos found = null;

                    while (found == null && !actualStructures.isEmpty()) {
                        int index = 0;

                        if (actualStructures.size() > 1) {
                            int weight = fork.nextInt(actualWeight);
                            for (StructureSet.StructureSelectionEntry entry : actualStructures) {
                                weight -= entry.weight();
                                if (weight < 0) {
                                    break;
                                }

                                index++;
                            }
                        }
                        StructureSet.StructureSelectionEntry entry = actualStructures.get(index);

                        int o = spreadType().evaluate(worldgenRandom, n);
                        int p = spreadType().evaluate(worldgenRandom, n);
                        boolean firstTry = pos == null;
                        pos = new ChunkPos(x * spacing() + o, z * spacing() + p);

                        // Like vanilla, the frequency decides whether this cell has a structure at all, so it isn't rolled again while searching
                        if (firstTry && !super.applyAdditionalChunkRestrictions(pos.x(), pos.z(), seed)) break;

                        if (bounds.isInside(pos.x(), pos.z())) found = findValidChunk(entry, pos, occupied);

                        actualStructures.remove(index);
                        actualWeight -= entry.weight();
                    }

                    if (found != null) {
                        structureChunks.add(found);
                        occupied.add(found);
                    }
                    cellPositions.put(new ChunkPos(x, z), found == null ? pos : found);
                }
            }
            this.cellPositions = cellPositions;
            this.structureChunks = structureChunks;
        }

        private ChunkPos findValidChunk(StructureSet.StructureSelectionEntry entry, ChunkPos start, List<ChunkPos> occupied) {
            BiomeSource biomeSource = ((ChunkGeneratorStructureStateAccessor) structureState).getBiomeSource();
            HolderSet<Biome> biomes = entry.structure().value().biomes();

            for (BlockPos.MutableBlockPos blockPos : BlockPos.spiralAround(new BlockPos(start.x(), 0, start.z()), spacing(), Direction.EAST, Direction.SOUTH)) {
                if (bounds.isInside(blockPos.getX(), blockPos.getZ(), -distanceFromBorder) && isSeparatedFrom(occupied, blockPos.getX(), blockPos.getZ()) && biomes.contains(biomeSource.getNoiseBiome(QuartPos.fromSection(blockPos.getX()), QuartPos.fromBlock(60), QuartPos.fromSection(blockPos.getZ()), structureState.randomState().sampler())) && super.applyInteractionsWithOtherStructures(structureState, blockPos.getX(), blockPos.getZ())) {
                    return new ChunkPos(blockPos.getX(), blockPos.getZ());
                }
            }
            return null;
        }

        private boolean isSeparatedFrom(List<ChunkPos> occupied, int x, int z) {
            for (ChunkPos pos : occupied) {
                if (Math.max(Math.abs(pos.x() - x), Math.abs(pos.z() - z)) <= minSeparation) return false;
            }
            return true;
        }

        @Override
        public ChunkPos getPotentialStructureChunk(long l, int i, int j) {
            group.generateValidPositions(l);
            ChunkPos pos = new ChunkPos(i, j);
            if (structureChunks.contains(pos)) return pos;

            int cellX = Math.floorDiv(i, spacing());
            int cellZ = Math.floorDiv(j, spacing());
            // Cells outside the bounds have no structure, so their origin works as a position that never has one
            return cellPositions.getOrDefault(new ChunkPos(cellX, cellZ), new ChunkPos(cellX * spacing(), cellZ * spacing()));
        }

        @Override
        public boolean applyAdditionalChunkRestrictions(int i, int j, long l) {
            group.generateValidPositions(l);
            return structureChunks.contains(new ChunkPos(i, j));
        }

        public static class Group {
            private final List<LimitedRandomStructurePlacement> placements = new ArrayList<>();
            private volatile boolean generatedPositions = false;

            private void generateValidPositions(long seed) {
                if (generatedPositions) return;
                synchronized (this) {
                    if (generatedPositions) return;
                    List<ChunkPos> occupied = new ArrayList<>();
                    for (LimitedRandomStructurePlacement placement : placements) {
                        placement.generateValidPositions(seed, occupied);
                    }
                    generatedPositions = true;
                }
            }
        }
    }
}
