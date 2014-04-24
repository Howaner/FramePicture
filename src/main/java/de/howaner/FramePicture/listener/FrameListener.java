package de.howaner.FramePicture.listener;

import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.managers.RegionManager;
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
import de.howaner.FramePicture.util.PictureDatabase;
import de.howaner.FramePicture.util.Utils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.world.WorldInitEvent;

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
		final ItemFrame entity = (ItemFrame) event.getRightClicked();
		final Player player = event.getPlayer();
		Frame frame = this.manager.getFrame(entity.getLocation());
		if (frame != null)
			event.setCancelled(true);
		
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
			
			final String path = Cache.getCacheCreating(player);
			Cache.removeCacheCreating(player);
			
			//Download Image
			PictureDatabase.FinishDownloadSignal signal = new PictureDatabase.FinishDownloadSignal() {
				@Override
				public void downloadSuccess(File file) {
					if (!Utils.isImage(file)) {
						player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PICTURE.getText().replace("%url", path));
						
						if (FrameListener.this.manager.getFramesWithImage(file.getName()).isEmpty()) {
							file.delete();
						}
						return;
					}
					Frame frame = manager.addFrame(file.getName(), entity);
					if (frame == null) return;
					
					player.sendMessage(Lang.PREFIX.getText() + Lang.FRAME_SET.getText()
						.replace("%url", path)
						.replace("%id", String.valueOf(frame.getId()))
						.replace("%name", file.getName()));
					
					if (Config.MONEY_ENABLED)
						FramePicturePlugin.getEconomy().withdrawPlayer(player.getName(), Config.CREATE_PRICE);
				}

				@Override
				public void downloadError(Exception e) {
					player.sendMessage(Lang.PREFIX.getText() + Lang.DOWNLOAD_ERROR.getText());
					player.sendMessage(Lang.PREFIX.getText() + ChatColor.GRAY + "Is this a correct picture url? " + ChatColor.RESET + path);
				}
			};
			
			if (new File(this.manager.getPictureDatabase().getOutputFolder(), path).exists()) {
				signal.downloadSuccess(new File(this.manager.getPictureDatabase().getOutputFolder(), path));
			} else {
				player.sendMessage(Lang.PREFIX.getText() + Lang.PLEASE_WAIT.getText());
				this.manager.getPictureDatabase().downloadImage(path, signal);
			}
		}
		
		///MULTIFRAME CREATING
		if (Cache.hasCacheMultiCreating(player)) {
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
			if (!player.hasPermission("FramePicture.multiset")) {
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
			
			int moveX, moveZ;
			switch (entity.getFacing()) {
				case SOUTH:
				{
					moveX = 1;
					moveZ = 0;
					break;
				}
				case EAST:
				{
					moveX = 0;
					moveZ = -1;
					break;
				}
				case NORTH:
				{
					moveX = -1;
					moveZ = 0;
					break;
				}
				case WEST:
				{
					moveX = 0;
					moveZ = 1;
					break;
				}
				default:
				{
					moveX = 1;
					moveZ = 0;
					break;
				}
			}
			
			List<ItemFrame> frameList = new ArrayList<ItemFrame>();
			World world = entity.getWorld();
			int x = entity.getLocation().getBlockX();
			int y = entity.getLocation().getBlockY();
			int z = entity.getLocation().getBlockZ();
			ItemFrame cacheFrame = Utils.getFrameAt(new Location(world, x, y, z));
			
			int vertical = 0, horizontal = 1;
			while (cacheFrame != null) {
				frameList.add(cacheFrame);
				vertical++;
				x += moveX;
				z += moveZ;
				cacheFrame = Utils.getFrameAt(new Location(world, x, y, z));
			}
			
			boolean success = true;
			while (success) {
				//Reset Locations
				x = entity.getLocation().getBlockX();
				y--;
				z = entity.getLocation().getBlockZ();
				
				for (int i = 0; i < vertical; i++) {
					ItemFrame cEntity = Utils.getFrameAt(new Location(world, x, y, z));
					if (cEntity == null) {
						success = false;
						break;
					}
					x += moveX;
					z += moveZ;
					frameList.add(cEntity);
					//frameList.add(vertical * horizontal + i, cEntity);
				}
				if (success)
					horizontal++;
			}
			
			final String path = Cache.getCacheMultiCreating(player);
			Cache.removeCacheMultiCreating(player);
			
			final ItemFrame[] frameArray = frameList.toArray(new ItemFrame[0]);
			final int v = vertical;
			final int h = horizontal;
			
			//Download Image
			PictureDatabase.FinishDownloadSignal signal = new PictureDatabase.FinishDownloadSignal() {
				@Override
				public void downloadSuccess(File file) {
					if (!Utils.isImage(file)) {
						player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PICTURE.getText().replace("%url", path));
						
						if (FrameListener.this.manager.getFramesWithImage(file.getName()).isEmpty()) {
							file.delete();
						}
						return;
					}
					
					List<Frame> frames = manager.addMultiFrames(FrameListener.this.manager.getPictureDatabase().loadImage(file.getName()), frameArray, v, h);
					if (frames == null) return;
					
					player.sendMessage(Lang.PREFIX.getText() + Lang.MULTIFRAME_SET.getText()
						.replace("%amount", String.valueOf(frames.size())));
					
					if (Config.MONEY_ENABLED)
						FramePicturePlugin.getEconomy().withdrawPlayer(player.getName(), Config.CREATE_PRICE);
				}

				@Override
				public void downloadError(Exception e) {
					player.sendMessage(Lang.PREFIX.getText() + Lang.DOWNLOAD_ERROR.getText());
					player.sendMessage(Lang.PREFIX.getText() + ChatColor.GRAY + "Is this a correct picture url? " + ChatColor.RESET + path);
				}
			};
			
			if (new File(this.manager.getPictureDatabase().getOutputFolder(), path).exists()) {
				signal.downloadSuccess(new File(this.manager.getPictureDatabase().getOutputFolder(), path));
			} else {
				player.sendMessage(Lang.PREFIX.getText() + Lang.PLEASE_WAIT.getText());
				this.manager.getPictureDatabase().downloadImage(path, signal);
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
			player.sendMessage(Lang.PREFIX.getText() + Lang.GET_URL.getText()
				.replace("%url", frame.getPicture())
				.replace("%id", String.valueOf(frame.getId())));
			Cache.removeCacheGetting(player);
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if (Cache.hasCacheCreating(player)) Cache.removeCacheCreating(player);
		if (Cache.hasCacheMultiCreating(player)) Cache.removeCacheMultiCreating(player);
		if (Cache.hasCacheGetting(player)) Cache.removeCacheGetting(player);
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
	
	@EventHandler (priority = EventPriority.LOW)
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
	public void onWorldInit(WorldInitEvent event) {
		World world = event.getWorld();
		this.manager.replaceTracker(world);
	}
	
	@EventHandler
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		Player player = event.getPlayer();
		this.manager.resendFrames(player);
	}
	
}
