package de.howaner.FramePicture.listener;

import de.howaner.FramePicture.FramePicturePlugin;
import de.howaner.FramePicture.util.Config;
import de.howaner.FramePicture.util.FrameLoader;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.plugin.java.JavaPlugin;

public class FrameLoadListener implements Listener {
	private final FrameLoader loader;
	
	public FrameLoadListener(FrameLoader loader) {
		this.loader = loader;
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		// Move the player a complete block?
		if (event.getFrom().getBlockX() == event.getTo().getBlockX()
			&& event.getFrom().getBlockY() == event.getTo().getBlockY()
			&& event.getFrom().getBlockZ() == event.getTo().getBlockZ()
			&& event.getFrom().getWorld() == event.getTo().getWorld())
		{
			return;
		}
		
		// Only when the player moves a complete chunk:
		if ((event.getTo().getBlockX() % 16) != 0
			&& (event.getTo().getBlockZ() % 16) != 0
			&& event.getFrom().getWorld() == event.getTo().getWorld())
		{
			return;
		}
		
		this.loader.populateNearbyChunks(event.getPlayer(), event.getTo().getChunk());
	}
	
	@EventHandler
	public void onPlayerJoin(final PlayerJoinEvent event) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(FramePicturePlugin.getPlugin(), new Runnable() {
			@Override
			public void run() {
				if (!event.getPlayer().isOnline()) return;
				FrameLoadListener.this.loader.populateNearbyChunks(event.getPlayer(), event.getPlayer().getLocation().getChunk());
			}
		}, Config.JOIN_WAIT_TIME * 20L);
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		this.loader.removePlayer(event.getPlayer());
	}
	
	@EventHandler
	public void onChunkUnload(ChunkUnloadEvent event) {
		this.loader.onChunkUnload(event.getChunk());
	}

}
