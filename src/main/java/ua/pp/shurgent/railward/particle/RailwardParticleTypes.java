package ua.pp.shurgent.railward.particle;

import com.mojang.serialization.Codec;

import net.minecraft.core.particles.ParticleType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import ua.pp.shurgent.railward.Railward;

public final class RailwardParticleTypes {

	private static final DeferredRegister<ParticleType<?>> PARTICLES =
		DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, Railward.MOD_ID);

	public static final RegistryObject<ParticleType<DieselExhaustParticleData>> DIESEL_EXHAUST =
		PARTICLES.register("diesel_exhaust", () -> new ParticleType<>(false,
			DieselExhaustParticleData.DESERIALIZER) {
			@Override
			public Codec<DieselExhaustParticleData> codec() {
				return DieselExhaustParticleData.CODEC;
			}
		});

	private RailwardParticleTypes() {
	}

	public static void register(IEventBus modEventBus) {
		PARTICLES.register(modEventBus);
	}
}
