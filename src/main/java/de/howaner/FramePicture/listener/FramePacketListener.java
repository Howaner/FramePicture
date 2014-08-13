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
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class FramePacketListener implements PacketListener {

	@Override
	public void onPacketSending(PacketEvent pe) {
		if (pe.getPacketType() == PacketType.Play.Server.SPAWN_ENTITY) {
			PacketContainer packet = pe.getPacket();
			final Player player = pe.getPlayer();
			
			int entityID = packet.getIntegers().read(0);
			int posX = packet.getIntegers().read(1) / 32;
			int posY = packet.getIntegers().read(2) / 32;
			int posZ = packet.getIntegers().read(3) / 32;
			int entityType = packet.getIntegers().read(9);

			// Check if the entity is a item frame (Id 71)
			if (entityType == 71) {
				Chunk chunk = new Location(player.getWorld(), posX, posY, posZ).getChunk();
				if (!chunk.isLoaded()) {
					return;
				}

				final Frame frame = FramePicturePlugin.getManager().getFrameWithEntityID(chunk, entityID);
				if (frame == null) {
					return;
				}
				
				Bukkit.getScheduler().runTaskLater(FramePicturePlugin.getPlugin(), new Runnable() {
					@Override
					public void run() {
						frame.sendTo(player);
					}
				}, 10L);
			}
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
