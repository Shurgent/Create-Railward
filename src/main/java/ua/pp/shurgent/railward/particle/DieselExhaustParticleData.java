package ua.pp.shurgent.railward.particle;

import java.util.Locale;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.serialization.Codec;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.network.FriendlyByteBuf;

public class DieselExhaustParticleData implements ParticleOptions {

	public static final Codec<DieselExhaustParticleData> CODEC = Codec.INT.xmap(
		id -> new DieselExhaustParticleData(Profile.byId(id)), data -> data.profile.id);

	@SuppressWarnings("deprecation")
	public static final Deserializer<DieselExhaustParticleData> DESERIALIZER =
		new Deserializer<DieselExhaustParticleData>() {
			@Override
			public DieselExhaustParticleData fromCommand(ParticleType<DieselExhaustParticleData> type,
				StringReader reader) throws CommandSyntaxException {
				reader.expect(' ');
				return new DieselExhaustParticleData(Profile.byId(reader.readInt()));
			}

			@Override
			public DieselExhaustParticleData fromNetwork(ParticleType<DieselExhaustParticleData> type,
				FriendlyByteBuf buffer) {
				return new DieselExhaustParticleData(Profile.byId(buffer.readVarInt()));
			}
		};

	private final Profile profile;

	public DieselExhaustParticleData(Profile profile) {
		this.profile = profile;
	}

	public Profile profile() {
		return profile;
	}

	@Override
	public ParticleType<?> getType() {
		return RailwardParticleTypes.DIESEL_EXHAUST.get();
	}

	@Override
	public void writeToNetwork(FriendlyByteBuf buffer) {
		buffer.writeVarInt(profile.id);
	}

	@Override
	public String writeToString() {
		return String.format(Locale.ROOT, "railward:diesel_exhaust %d", profile.id);
	}

	public enum Profile {
		STARTUP_LIGHT(0, .72f, .74f, .73f, .48f, .52f, .34f, .80f),
		IDLE(1, .39f, .46f, .47f, .43f, .55f, .36f, .88f),
		NORMAL(2, .31f, .38f, .39f, .66f, .68f, .43f, 1.06f),
		FULL(3, .075f, .082f, .078f, .92f, .92f, .56f, 1.32f);

		private static final Profile[] VALUES = values();

		public final int id;
		public final float red;
		public final float green;
		public final float blue;
		public final float alpha;
		public final float rapidGrowth;
		public final float slowGrowth;
		public final float rotationMultiplier;

		Profile(int id, float red, float green, float blue, float alpha, float rapidGrowth,
			float slowGrowth, float rotationMultiplier) {
			this.id = id;
			this.red = red;
			this.green = green;
			this.blue = blue;
			this.alpha = alpha;
			this.rapidGrowth = rapidGrowth;
			this.slowGrowth = slowGrowth;
			this.rotationMultiplier = rotationMultiplier;
		}

		public static Profile byId(int id) {
			return VALUES[Math.floorMod(id, VALUES.length)];
		}
	}
}
