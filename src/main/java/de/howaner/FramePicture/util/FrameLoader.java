package de.howaner.FramePicture.util;

import de.howaner.FramePicture.FrameManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

public class FrameLoader {
	private final FrameManager manager;
	private final Map<Player, List<Chunk>> loadedFrames = new HashMap<Player, List<Chunk>>();
	
	public FrameLoader(FrameManager manager) {
		this.manager = manager;
	}
	
	public void removePlayer(Player player) {
		this.loadedFrames.remove(player);
	}
	
	public List<Chunk> getLoadedChunksFromPlayer(Player player) {
		if (!this.loadedFrames.containsKey(player)) {
			return new ArrayList<Chunk>();
		}
		
		return this.loadedFrames.get(player);
	}
	
	public void sendChunk(Player player, Chunk chunk) {
		List<Chunk> loadedChunks = this.getLoadedChunksFromPlayer(player);
		if (loadedChunks.contains(chunk))
			return;
		
		for (Entity entity : chunk.getEntities()) {
			if (entity.getType() != EntityType.ITEM_FRAME) continue;
			ItemFrame frameEntity = (ItemFrame) entity;
			
			if (this.manager.isFramePicture(frameEntity)) {
				System.out.println("Found Itemframe!");
				Frame frame = this.manager.getFrame(frameEntity.getLocation());
				frame.setEntity(frameEntity);
				frame.sendToPlayer(player);
			}
		}
	}
	
	public List<Chunk> getNearbyChunks(Chunk chunk) {
		//int radius = Config.FRAMELOADER_CHUNKS / 2;
		List<Chunk> foundedChunks = new ArrayList<Chunk>();
		
		/*for (int x = (chunk.getX() - radius); x <= (chunk.getX() + radius); x++) {
			for (int z = (chunk.getZ() - radius); z <= (chunk.getZ() + radius); z++) {
				Chunk nearbyChunk = chunk.getWorld().getChunkAt(x, z);
				foundedChunks.add(nearbyChunk);
			}
		}*/
		
		for (int x = chunk.getX() - 1; x <= chunk.getX() + 1; x++) {
			for (int z = chunk.getZ() - 1; z <= chunk.getZ() + 1; z++) {
				Chunk nearbyChunk = chunk.getWorld().getChunkAt(x, z);
				foundedChunks.add(nearbyChunk);
			}
		}
		
		return foundedChunks;
	}
	
	public void loadChunk(Chunk singleChunk) {
		List<Chunk> nearbyChunks = this.getNearbyChunks(singleChunk);
		
		for (Chunk chunk : nearbyChunks) {
			for (Entity entity : chunk.getEntities()) {
				if (entity.getType() != EntityType.PLAYER) continue;
				
				Player player = (Player) entity;
				this.sendChunk(player, chunk);
			}
		}
	}
	
	public void sendFramesToPlayers(List<Frame> framesToSend) {
		List<Chunk> chunks = new ArrayList<Chunk>();
		for (Frame frame : framesToSend) {
			Chunk frameChunk = frame.getLocation().getChunk();
			
			for (Chunk chunk : this.getNearbyChunks(frameChunk)) {
				if (chunks.contains(chunk)) continue;
				chunks.add(chunk);
			}
		}
		
		for (Chunk chunk : chunks) {
			for (Entity entity : chunk.getEntities()) {
				if (entity.getType() != EntityType.PLAYER) continue;
				Player player = (Player) entity;
				
				for (Frame frame : framesToSend) {
					if (frame.getEntity() == null) {
						// Search entity
						boolean found = false;
						for (Entity e : chunk.getEntities()) {
							if (e.getType() != EntityType.ITEM_FRAME) continue;
							ItemFrame frameEntity = (ItemFrame) e;
							if (Utils.isSameLocation(frameEntity.getLocation(), frame.getLocation())) {
								frame.setEntity(frameEntity);
								found = true;
								break;
							}
						}
						
						if (!found) continue;
					}
					
					frame.sendToPlayer(player);
				}
			}
		}
	}
	
	public void populateNearbyChunks(Player player, Chunk chunk) {
		List<Chunk> nearbyChunks = this.getNearbyChunks(chunk);
		for (Chunk nearbyChunk : nearbyChunks) {
			this.sendChunk(player, nearbyChunk);
		}
		
		this.loadedFrames.put(player, nearbyChunks);
	}
	
	public void onChunkUnload(Chunk chunk) {
		for (Entry<Player, List<Chunk>> e : this.loadedFrames.entrySet()) {
			List<Chunk> chunks = e.getValue();
			chunks.remove(chunk);
			e.setValue(chunks);
		}
	}
	
	public void reload() {
		// Clear map
		this.loadedFrames.clear();
		
		// Send all frames
		for (Player player : Bukkit.getOnlinePlayers()) {
			this.populateNearbyChunks(player, player.getLocation().getChunk());
		}
	}

}
