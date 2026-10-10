package wily.legacy_world_sizes.util;

import com.mojang.serialization.Codec;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.synth.NormalNoise;
import wily.factoryapi.util.ListMap;
import wily.legacy_world_sizes.LegacyWorldSizes;
import wily.legacy_world_sizes.config.LWSWorldOptions;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public record LegacyBiomeScale(Identifier id, Component name, Optional<AddOctave> octaveFunction) {
    public static final ListMap<Identifier, LegacyBiomeScale> map = new ListMap<>();
    public static final Codec<LegacyBiomeScale> CODEC = map.createCodec(Identifier.CODEC);

    public static final List<ResourceKey<NormalNoise.NoiseParameters>> BIOME_SCALE_NOISE_PARAMETERS = List.of(Noises.TEMPERATURE, Noises.VEGETATION, Noises.CONTINENTALNESS);

    public static final LegacyBiomeScale SMALL = register(new LegacyBiomeScale(LegacyWorldSizes.createModLocation("small"), LWSComponents.optionName("biomeScales.small"), Optional.of(new AddOctave(2))));

    public static final LegacyBiomeScale MEDIUM = register(new LegacyBiomeScale(LegacyWorldSizes.createModLocation("medium"), LWSComponents.optionName("biomeScales.medium"), Optional.of(new AddOctave(1))));

    public static final LegacyBiomeScale CUSTOM = register(new LegacyBiomeScale(LegacyWorldSizes.createModLocation("custom"), LWSComponents.optionName("biomeScales.large"), Optional.empty()));

    public interface OctaveFunction extends Function<Integer, Integer> {
        default void applyToNoiseParameters(Holder.Reference<NormalNoise.NoiseParameters> noiseParameters) {
            OctaveFunctionHolder holder = ((OctaveFunctionHolder)  (Object) noiseParameters.value());
            if (holder.getOctaveFunction() != OctaveFunction.this) {
                int oldValue = noiseParameters.value().firstOctave();
                holder.setOctaveFunction(OctaveFunction.this);
                LegacyWorldSizes.LOGGER.debug("Adjusted {} firstOctave from {} to {}", noiseParameters.key().identifier(), oldValue, noiseParameters.value().firstOctave());
            }
        }

        default void applyToBiomeNoiseParameters(RegistryAccess access) {
            for (ResourceKey<NormalNoise.NoiseParameters> key : BIOME_SCALE_NOISE_PARAMETERS) {
                access.lookupOrThrow(Registries.NOISE).get(key).ifPresent(this::applyToNoiseParameters);
            }
        }
    }

    public record AddOctave(int value) implements OctaveFunction {
        public static final Codec<AddOctave> CODEC = Codec.INT.xmap(AddOctave::new, AddOctave::value);
        public static final AddOctave ZERO = new AddOctave(0);

        @Override
        public void applyToBiomeNoiseParameters(RegistryAccess access) {
            if (value != 0)
                OctaveFunction.super.applyToBiomeNoiseParameters(access);
        }

        @Override
        public Integer apply(Integer integer) {
            return integer + value;
        }
    }

    public void applyToAddToBiomeFirstOctave() {
        octaveFunction.ifPresent(LWSWorldOptions.addToBiomeFirstOctave::set);
    }

    public interface OctaveFunctionHolder {
        OctaveFunction getOctaveFunction();

        void setOctaveFunction(OctaveFunction octaveFunction);
    }

    public static LegacyBiomeScale register(LegacyBiomeScale biomeScale) {
        map.put(biomeScale.id(), biomeScale);
        return biomeScale;
    }
}
