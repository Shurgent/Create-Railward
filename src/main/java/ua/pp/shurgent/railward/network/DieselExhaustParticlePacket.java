package ua.pp.shurgent.railward.network;

import com.simibubi.create.foundation.networking.SimplePacketBase;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent.Context;
import ua.pp.shurgent.railward.particle.DieselExhaustParticleData.Profile;

public class DieselExhaustParticlePacket extends SimplePacketBase {

	private final int carriageEntityId;
	private final Vec3 localEmitter;
	private final Profile profile;
	private final Vec3 velocity;

	public DieselExhaustParticlePacket(int carriageEntityId, Vec3 localEmitter, Profile profile, Vec3 velocity) {
		this.carriageEntityId = carriageEntityId;
		this.localEmitter = localEmitter;
		this.profile = profile;
		this.velocity = velocity;
	}

	public DieselExhaustParticlePacket(FriendlyByteBuf buffer) {
		carriageEntityId = buffer.readVarInt();
		localEmitter = readVec3(buffer);
		profile = buffer.readEnum(Profile.class);
		velocity = readVec3(buffer);
	}

	@Override
	public void write(FriendlyByteBuf buffer) {
		buffer.writeVarInt(carriageEntityId);
		writeVec3(buffer, localEmitter);
		buffer.writeEnum(profile);
		writeVec3(buffer, velocity);
	}

	@Override
	public boolean handle(Context context) {
		context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
			() -> () -> DieselExhaustParticleClientHandler.handle(carriageEntityId, localEmitter, profile, velocity)));
		return true;
	}

	private static Vec3 readVec3(FriendlyByteBuf buffer) {
		return new Vec3(buffer.readDouble(), buffer.readDouble(), buffer.readDouble());
	}

	private static void writeVec3(FriendlyByteBuf buffer, Vec3 vector) {
		buffer.writeDouble(vector.x);
		buffer.writeDouble(vector.y);
		buffer.writeDouble(vector.z);
	}
}
