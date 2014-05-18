package de.howaner.FramePicture.tracker;

import de.howaner.FramePicture.FramePicturePlugin;
import de.howaner.FramePicture.util.Frame;
import de.howaner.FramePicture.util.Utils;
import java.lang.reflect.Field;
import net.minecraft.server.v1_7_R3.Entity;
import net.minecraft.server.v1_7_R3.EntityItemFrame;
import net.minecraft.server.v1_7_R3.EntityPlayer;
import net.minecraft.server.v1_7_R3.EntityTrackerEntry;
import net.minecraft.server.v1_7_R3.MathHelper;
import net.minecraft.server.v1_7_R3.Packet;
import net.minecraft.server.v1_7_R3.PacketPlayOutSpawnEntity;
import org.bukkit.Location;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

public class FakeEntityTrackerEntry extends EntityTrackerEntry {

	public FakeEntityTrackerEntry(Entity entity, int i, int j, boolean flag) {
		super(entity, i, j, flag);
	}
	
	@Override
	public void updatePlayer(EntityPlayer entityplayer) {
		if (!(this.tracker instanceof EntityItemFrame)) {
			super.updatePlayer(entityplayer);
			return;
		}
		
		double d0 = entityplayer.locX - this.xLoc / 32;
		double d1 = entityplayer.locZ - this.zLoc / 32;
		
		if ((d0 >= -this.b) && (d0 <= this.b) && (d1 >= -this.b) && (d1 <= this.b)) {
			if ((!this.trackedPlayers.contains(entityplayer)) && ((d(entityplayer)) || (this.tracker.n))) {
				EntityItemFrame entity = (EntityItemFrame) this.tracker;
				Location loc = this.createLocation(entity);
				Frame frame = FramePicturePlugin.getManager().getFrame(loc);
				
				if (frame == null) {
					for (Frame f : FramePicturePlugin.getManager().getUnloadedFrames()) {
						if (Utils.isSameLocation(f.getLocation(), loc)) {
							FramePicturePlugin.getManager().loadFrame(f, (ItemFrame)entity.getBukkitEntity());
							frame = f;
							break;
						}
					}
					
					if (frame == null) {
						super.updatePlayer(entityplayer);
						return;
					}
				}
				entityplayer.removeQueue.remove(Integer.valueOf(this.tracker.getId()));
				
				this.trackedPlayers.add(entityplayer);
				Packet packet = this.createPacket();
				entityplayer.playerConnection.sendPacket(packet);
				
				this.j = this.tracker.motX;
				this.k = this.tracker.motY;
				this.l = this.tracker.motZ;
				this.i = MathHelper.d(this.tracker.getHeadRotation() * 256.0F / 360.0F);
				
				Player player = entityplayer.getBukkitEntity();
				frame.sendMapData(player);
				frame.sendItemMeta(player);
			}
		} else if (this.trackedPlayers.contains(entityplayer)) {
			this.trackedPlayers.remove(entityplayer);
			entityplayer.d(this.tracker);
		}
	}
	
	public Packet createPacket() {
		if (this.tracker.dead || !(this.tracker instanceof EntityItemFrame)) {
			return null;
		}
		EntityItemFrame entity = (EntityItemFrame)this.tracker;
		
		PacketPlayOutSpawnEntity packet = new PacketPlayOutSpawnEntity(this.tracker, 71, entity.direction);
		packet.a(MathHelper.d(entity.x * 32));
		packet.b(MathHelper.d(entity.y * 32));
		packet.c(MathHelper.d(entity.z * 32));
		return packet;
	}
	
	private boolean d(EntityPlayer player) {
		return player.r().getPlayerChunkMap().a(player, this.tracker.ah, this.tracker.aj);
	}
	
	public Location createLocation(EntityItemFrame entity) {
		return entity.getBukkitEntity().getLocation();
	}
	
	public <T> T getPrivateValue(String name, Class<T> type) {
		try {
			Field field = EntityTrackerEntry.class.getDeclaredField(name);
			field.setAccessible(true);
			return (T) field.get(this);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void setPrivateValue(String name, Object value) throws Exception {
		Field field = EntityTrackerEntry.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(this, value);
	}
	
}
