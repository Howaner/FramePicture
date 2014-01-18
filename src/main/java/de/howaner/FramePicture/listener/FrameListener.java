package de.howaner.FramePicture.listener;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.managers.RegionManager;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import de.howaner.FramePicture.FrameManager;
import de.howaner.FramePicture.FramePicturePlugin;
import de.howaner.FramePicture.util.Cache;
import de.howaner.FramePicture.util.Config;
import de.howaner.FramePicture.util.Frame;
import de.howaner.FramePicture.util.Lang;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;

public class FrameListener implements Listener {
	
	private FrameManager manager;
	
	public FrameListener(FrameManager manager) {
		this.manager = manager;
	}
	
	@EventHandler (priority = EventPriority.LOW)
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		//Check
		if (event.isCancelled()) return;
		if (event.getRightClicked().getType() != EntityType.ITEM_FRAME) return;
		ItemFrame entity = (ItemFrame) event.getRightClicked();
		Player player = event.getPlayer();
		Frame frame = this.manager.getFrame(entity.getLocation());
		if (frame != null)
			event.setCancelled(true);
		/*if (frame != null && Config.WORLDGUARD_ENABLED && Config.WORLDGUARD_ROTATE_FRAME && !player.hasPermission("FramePicture.ignoreWorldGuard")) {
			RegionManager rm = FramePicturePlugin.getWorldGuard().getRegionManager(player.getWorld());
			LocalPlayer localPlayer = FramePicturePlugin.getWorldGuard().wrapPlayer(player);
			if (!rm.getApplicableRegions(entity.getLocation()).canBuild(localPlayer)) {
				player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
				event.setCancelled(true);
				return;
			}
		}*/
		///CREATING
		if (Cache.hasCacheCreating(player)) {
			event.setCancelled(true);
			//Money
			if (Config.MONEY_ENABLED) {
				if (FramePicturePlugin.getEconomy().getBalance(player.getName()) < Config.CREATE_PRICE) {
					player.sendMessage(Lang.NOT_ENOUGH_MONEY.getText());
					Cache.removeCacheCreating(player);
					return;
				}
			}
			//Permission
			if (!player.hasPermission("FramePicture.set")) {
				player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
				return;
			}
			
			//WorldGuard Check
			if (Config.WORLDGUARD_ENABLED && Config.WORLDGUARD_BUILD && !player.hasPermission("FramePicture.ignoreWorldGuard")) {
				RegionManager rm = FramePicturePlugin.getWorldGuard().getRegionManager(player.getWorld());
				LocalPlayer localPlayer = FramePicturePlugin.getWorldGuard().wrapPlayer(player);
				if (!rm.getApplicableRegions(entity.getLocation()).canBuild(localPlayer)) {
					player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
					return;
				}
			}
			
			//Is a Item in the Frame?
			if (frame != null) {
				player.sendMessage(Lang.PREFIX.getText() + Lang.ALREADY_FRAME.getText());
				return;
			}
			
			//Create Frame
			String path = Cache.getCacheCreating(player);
			frame = manager.addFrame(path, entity);
			if (frame != null) {
				Cache.removeCacheCreating(player);
				player.sendMessage(Lang.PREFIX.getText() + Lang.FRAME_SET.getText().replace("%url", path).replace("%id", String.valueOf(frame.getId())));
				if (Config.MONEY_ENABLED) FramePicturePlugin.getEconomy().withdrawPlayer(player.getName(), Config.CREATE_PRICE);
			}
		}
		///GETTING
		if (Cache.hasCacheGetting(player)) {
			event.setCancelled(true);
			//Permission
			if (!player.hasPermission("FramePicture.get")) {
				player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
				return;
			}
			//Frame
			if (frame == null) {
				player.sendMessage(Lang.NO_FRAMEPICTURE.getText());
				return;
			}
			player.sendMessage(Lang.PREFIX.getText() + Lang.GET_URL.getText().replace("%url", frame.getPath()).replace("%id", String.valueOf(frame.getId())));
			Cache.removeCacheGetting(player);
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if (Cache.hasCacheCreating(player)) Cache.removeCacheCreating(player);
		if (Cache.hasCacheGetting(player)) Cache.removeCacheGetting(player);
		
		for (Frame frame : this.manager.getFrames()) {
			if (frame.getSeePlayers().contains(player))
				frame.getSeePlayers().remove(player);
		}
	}
	
	@EventHandler (priority = EventPriority.LOW)
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled()) return;
		Player player = event.getPlayer();
		Block block = event.getBlock();
		
		Frame[] frames = new Frame[] {
			this.manager.getFrame(block.getLocation()),
			this.manager.getFrame(block.getRelative(BlockFace.NORTH).getLocation()),
			this.manager.getFrame(block.getRelative(BlockFace.SOUTH).getLocation()),
			this.manager.getFrame(block.getRelative(BlockFace.EAST).getLocation()),
			this.manager.getFrame(block.getRelative(BlockFace.WEST).getLocation())
		};
		
		for (Frame frame : frames) {
			if (frame == null) continue;
			if (Config.WORLDGUARD_ENABLED && Config.WORLDGUARD_BREAK && !player.hasPermission("FramePicture.ignoreWorldGuard")) {
				RegionManager rm = FramePicturePlugin.getWorldGuard().getRegionManager(player.getWorld());
				LocalPlayer localPlayer = FramePicturePlugin.getWorldGuard().wrapPlayer(player);
				if (!rm.getApplicableRegions(frame.getLocation()).canBuild(localPlayer)) {
					player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
					event.setCancelled(true);
					return;
				}
			}
			
			this.manager.removeFrame(frame.getId());
			player.sendMessage(Lang.PREFIX.getText() + Lang.FRAME_REMOVED.getText().replace("%id", String.valueOf(frame.getId())));
		}
	}
	
	@EventHandler
	public void onHangingBreak(HangingBreakEvent event) {
		if (event.isCancelled()) return;
		if (event.getEntity().getType() != EntityType.ITEM_FRAME) return;
		Frame frame = this.manager.getFrame(event.getEntity().getLocation());
		if (frame == null) return;
		
		if (event instanceof HangingBreakByEntityEvent) {
			if (((HangingBreakByEntityEvent)event).getRemover().getType() == EntityType.PLAYER) {
				Player player = (Player) ((HangingBreakByEntityEvent)event).getRemover();
				//WorldGuard Check
				if (Config.WORLDGUARD_ENABLED && Config.WORLDGUARD_BREAK && !player.hasPermission("FramePicture.ignoreWorldGuard")) {
					RegionManager rm = FramePicturePlugin.getWorldGuard().getRegionManager(player.getWorld());
					LocalPlayer localPlayer = FramePicturePlugin.getWorldGuard().wrapPlayer(player);
					if (!rm.getApplicableRegions(frame.getLocation()).canBuild(localPlayer)) {
						player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
						event.setCancelled(true);
						return;
					}
				}
			}
		}
		
		this.manager.removeFrame(frame.getId());
		if (event instanceof HangingBreakByEntityEvent && ((HangingBreakByEntityEvent)event).getRemover().getType() == EntityType.PLAYER) {
			Player player = (Player) ((HangingBreakByEntityEvent)event).getRemover();
			player.sendMessage(Lang.PREFIX.getText() + Lang.FRAME_REMOVED.getText().replace("%id", String.valueOf(frame.getId())));
		}
	}
	
	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		if (event.isCancelled()) return;
		if (event.getFrom().getWorld() == event.getTo().getWorld() &&
				event.getFrom().getBlockX() == event.getTo().getBlockX() &&
				event.getFrom().getBlockY() == event.getTo().getBlockY() &&
				event.getFrom().getBlockZ() == event.getTo().getBlockZ())
			return;
		
		Player player = event.getPlayer();
		for (Frame frame : this.manager.getFrames())
			frame.checkPlayer(player);
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		Player player = event.getPlayer();
		for (Frame frame : this.manager.getFrames())
			frame.checkPlayer(player);
	}
	
}
