package de.howaner.FramePicture.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;
import de.howaner.FramePicture.FramePicturePlugin;
import de.howaner.FramePicture.util.Frame;
import de.howaner.FramePicture.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class FramePacketListener implements PacketListener {

	@Override
	public void onPacketSending(PacketEvent pe) {
		if (pe.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
			PacketContainer packet = pe.getPacket();
			final Player player = pe.getPlayer();
			
			int entityID = packet.getIntegers().read(0);
			Location loc = new Location(
					player.getWorld(),
					((double)packet.getIntegers().read(1) / 32.0D),
					((double)packet.getIntegers().read(2) / 32.0D),
					((double)packet.getIntegers().read(3) / 32.0D)
			);
			int entityType = packet.getIntegers().read(9);
			int direction = packet.getIntegers().read(10);

			// Check if the entity is a item frame (Id 71)
			if (entityType != 71) {
				return;
			}

			Chunk chunk = loc.getChunk();
			if (!chunk.isLoaded()) {
				return;
			}

			Frame frame = FramePicturePlugin.getManager().getFrameWithEntityID(chunk, entityID);
			if (frame == null) {
				// Search the frame in the chunk.
				BlockFace facing = this.convertDirectionToBlockFace(direction);
				ItemFrame entity = Utils.getItemFrameFromChunk(chunk, loc, facing);
				if (entity == null) {
					return;
				}

				frame = FramePicturePlugin.getManager().getFrame(loc, facing);
				if (frame == null) {
					return;
				}
				frame.setEntity(entity);
			}

			final Frame frameToSend = frame;
			Bukkit.getScheduler().runTaskLater(FramePicturePlugin.getPlugin(), new Runnable() {
				@Override
				public void run() {
					frameToSend.sendTo(player);
				}
			}, 10L);
		}
	}

	private BlockFace convertDirectionToBlockFace(int direction) {
		switch (direction) {
			case 0:
				return BlockFace.SOUTH;
			case 1:
				return BlockFace.WEST;
			case 2:
				return BlockFace.NORTH;
			case 3:
				return BlockFace.EAST;
			default:
				return BlockFace.NORTH;
		}
	}

	@Override
	public void onPacketReceiving(PacketEvent pe) { }

	@Override
	public ListeningWhitelist getSendingWhitelist() {
		return ListeningWhitelist.newBuilder().
			priority(ListenerPriority.LOW).
			types(PacketType.Play.Server.SPAWN_ENTITY).
			gamePhase(GamePhase.BOTH).
			options(new ListenerOptions[0]).
			build();
	}

	@Override
	public ListeningWhitelist getReceivingWhitelist() {
		return ListeningWhitelist.EMPTY_WHITELIST;
	}

	@Override
	public Plugin getPlugin() {
		return FramePicturePlugin.getPlugin();
	}

}
