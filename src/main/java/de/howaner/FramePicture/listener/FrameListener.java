package de.howaner.FramePicture.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.protection.managers.RegionManager;
import de.howaner.FramePicture.FrameManager;
import de.howaner.FramePicture.FramePicturePlugin;
import de.howaner.FramePicture.util.Cache;
import de.howaner.FramePicture.util.Config;
import de.howaner.FramePicture.util.Frame;
import de.howaner.FramePicture.util.Lang;
import de.howaner.FramePicture.util.PacketSender;
import de.howaner.FramePicture.util.PictureDatabase;
import de.howaner.FramePicture.util.Utils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class FrameListener implements Listener {
	private final FrameManager manager;
	
	public FrameListener(FrameManager manager) {
		this.manager = manager;
	}
	
	@EventHandler (priority = EventPriority.HIGH)
	public void onPlayerInteract(PlayerInteractEntityEvent event) {
		if (event.isCancelled() || (event.getRightClicked().getType() != EntityType.ITEM_FRAME)) return;
		final ItemFrame entity = (ItemFrame) event.getRightClicked();
		final Player player = event.getPlayer();
		
		// SINGLE CREATING
		if (Cache.hasCacheCreating(player)) {
			event.setCancelled(true);
			
			if (Config.MONEY_ENABLED) {
				if (FramePicturePlugin.getEconomy().getBalance(player) < Config.CREATE_PRICE) {
					player.sendMessage(Lang.NOT_ENOUGH_MONEY.getText());
					Cache.removeCacheCreating(player);
					return;
				}
			}
			
			if (!player.hasPermission("FramePicture.set")) {
				player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
				Cache.removeCacheCreating(player);
				return;
			}
			
			if (Config.WORLDGUARD_ENABLED && Config.WORLDGUARD_BUILD && !player.hasPermission("FramePicture.ignoreWorldGuard")) {
				RegionManager rm = FramePicturePlugin.getWorldGuard().getRegionManager(player.getWorld());
				LocalPlayer localPlayer = FramePicturePlugin.getWorldGuard().wrapPlayer(player);
				if (!rm.getApplicableRegions(entity.getLocation()).canBuild(localPlayer)) {
					player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
					return;
				}
			}
			
			Frame frame = this.manager.getFrame(entity);
			if (frame != null) {
				player.sendMessage(Lang.PREFIX.getText() + Lang.ALREADY_FRAME.getText());
				return;
			}
			
			final String path = Cache.getCacheCreating(player);
			PictureDatabase.FinishDownloadSignal signal = new PictureDatabase.FinishDownloadSignal() {
				@Override
				public void downloadSuccess(File file, boolean wasLocal) {
					if (Config.MONEY_ENABLED) {
						if (FramePicturePlugin.getEconomy().getBalance(player) < Config.CREATE_PRICE) {
							if (!wasLocal && FrameListener.this.manager.getFramesWithImage(file.getName()).isEmpty()) {
								file.delete();
							}
							player.sendMessage(Lang.NOT_ENOUGH_MONEY.getText());
							return;
						}
					}
					
					Frame frame = FrameListener.this.manager.addFrame(file.getName(), entity);
					if (frame == null) {
						player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PICTURE.getText().replace("%url", path));
						
						if (!wasLocal && FrameListener.this.manager.getFramesWithImage(file.getName()).isEmpty()) {
							file.delete();
						}
						return;
					}
					
					player.sendMessage(Lang.PREFIX.getText() + Lang.FRAME_SET.getText()
						.replace("%url", path)
						.replace("%id", String.valueOf(frame.getId()))
						.replace("%name", file.getName()));
					
					if (Config.MONEY_ENABLED)
						FramePicturePlugin.getEconomy().withdrawPlayer(player, Config.CREATE_PRICE);
				}

				@Override
				public void downloadError(Exception e) {
					player.sendMessage(Lang.PREFIX.getText() + Lang.DOWNLOAD_ERROR.getText().replace("%url", path));
				}
			};
			
			Cache.removeCacheCreating(player);
			
			if (new File(this.manager.getPictureDatabase().getOutputFolder(), path).exists()) {
				signal.downloadSuccess(new File(this.manager.getPictureDatabase().getOutputFolder(), path), true);
			} else {
				player.sendMessage(Lang.PREFIX.getText() + Lang.PLEASE_WAIT.getText());
				this.manager.getPictureDatabase().downloadImage(path, signal);
			}
		}
		
		// MULTI CREATING
		else if (Cache.hasCacheMultiCreating(player)) {
			event.setCancelled(true);
			
			if (Config.MONEY_ENABLED) {
				if (FramePicturePlugin.getEconomy().getBalance(player) < Config.CREATE_PRICE) {
					player.sendMessage(Lang.NOT_ENOUGH_MONEY.getText());
					Cache.removeCacheCreating(player);
					return;
				}
			}
			
			if (!player.hasPermission("FramePicture.multiset")) {
				player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
				return;
			}
			
			if (Config.WORLDGUARD_ENABLED && Config.WORLDGUARD_BUILD && !player.hasPermission("FramePicture.ignoreWorldGuard")) {
				RegionManager rm = FramePicturePlugin.getWorldGuard().getRegionManager(player.getWorld());
				LocalPlayer localPlayer = FramePicturePlugin.getWorldGuard().wrapPlayer(player);
				if (!rm.getApplicableRegions(entity.getLocation()).canBuild(localPlayer)) {
					player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
					return;
				}
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
			ItemFrame cacheFrame = Utils.getItemFrameFromChunk(entity.getLocation().getChunk(), new Location(world, x, y, z), entity.getFacing());
			
			int vertical = 0, horizontal = 1;
			while (cacheFrame != null) {
				frameList.add(cacheFrame);
				vertical++;
				x += moveX;
				z += moveZ;
				Location loc = new Location(world, x, y, z);
				cacheFrame = Utils.getItemFrameFromChunk(loc.getChunk(), loc, entity.getFacing());
			}
			
			boolean success = true;
			while (success) {
				//Reset Locations
				x = entity.getLocation().getBlockX();
				y--;
				z = entity.getLocation().getBlockZ();
				
				for (int i = 0; i < vertical; i++) {
					Location loc = new Location(world, x, y, z);
					ItemFrame cEntity = Utils.getItemFrameFromChunk(loc.getChunk(), loc, entity.getFacing());
					if (cEntity == null) {
						success = false;
						break;
					}
					x += moveX;
					z += moveZ;
					frameList.add(cEntity);
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
				public void downloadSuccess(File file, boolean wasLocal) {
					BufferedImage image = FrameListener.this.manager.getPictureDatabase().loadImage(file.getName());
					if (image == null) {
						player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PICTURE.getText().replace("%url", path));
						
						if (!wasLocal && FrameListener.this.manager.getFramesWithImage(file.getName()).isEmpty()) {
							file.delete();
						}
						return;
					}
					
					List<Frame> frames = manager.addMultiFrames(image, frameArray, v, h);
					if (frames == null) return;
					
					player.sendMessage(Lang.PREFIX.getText() + Lang.MULTIFRAME_SET.getText()
						.replace("%amount", String.valueOf(frames.size())));
					
					if (Config.MONEY_ENABLED)
						FramePicturePlugin.getEconomy().withdrawPlayer(player, Config.CREATE_PRICE);
				}

				@Override
				public void downloadError(Exception e) {
					player.sendMessage(Lang.PREFIX.getText() + Lang.DOWNLOAD_ERROR.getText().replace("%url", path));
				}
			};
			
			if (new File(this.manager.getPictureDatabase().getOutputFolder(), path).exists()) {
				signal.downloadSuccess(new File(this.manager.getPictureDatabase().getOutputFolder(), path), true);
			} else {
				player.sendMessage(Lang.PREFIX.getText() + Lang.PLEASE_WAIT.getText());
				this.manager.getPictureDatabase().downloadImage(path, signal);
			}
		}
		
		// GETTING
		else if (Cache.hasCacheGetting(player)) {
			event.setCancelled(true);
			
			if (!player.hasPermission("FramePicture.get")) {
				player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
				return;
			}
			
			Frame frame = this.manager.getFrame(entity);
			if (frame == null) {
				player.sendMessage(Lang.NO_FRAMEPICTURE.getText());
				return;
			}
			player.sendMessage(Lang.PREFIX.getText() + Lang.GET_URL.getText()
				.replace("%url", frame.getPicture())
				.replace("%entity", (frame.isLoaded() ? String.valueOf(frame.getEntity().getEntityId()) : "Not loaded"))
				.replace("%id", String.valueOf(frame.getId())));
			Cache.removeCacheGetting(player);
		}
		
		else if (this.manager.getFrame(entity) != null) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if (Cache.hasCacheCreating(player)) Cache.removeCacheCreating(player);
		if (Cache.hasCacheMultiCreating(player)) Cache.removeCacheMultiCreating(player);
		if (Cache.hasCacheGetting(player)) Cache.removeCacheGetting(player);
		if (PacketSender.packetsToSend.containsKey(player)) PacketSender.packetsToSend.remove(player);
	}
	
	@EventHandler (priority = EventPriority.HIGH)
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		if (event.isCancelled() || !Config.WORLDGUARD_ENABLED || !Config.WORLDGUARD_BREAK) return;
		
		RegionManager rm = FramePicturePlugin.getWorldGuard().getRegionManager(player.getWorld());
		LocalPlayer localPlayer = FramePicturePlugin.getWorldGuard().wrapPlayer(player);
		
		BlockFace[] faces = { BlockFace.NORTH, BlockFace.SOUTH, BlockFace.EAST, BlockFace.WEST };
		
		if (!player.hasPermission("FramePicture.ignoreWorldGuard")) {
			for (BlockFace face : faces) {
				Location loc = block.getRelative(face).getLocation();
				BlockFace frameFace = face.getOppositeFace();

				Frame frame = this.manager.getFrame(loc, frameFace);
				if (frame == null) continue;

				if (!rm.getApplicableRegions(frame.getLocation()).canBuild(localPlayer)) {
					player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
					event.setCancelled(true);
					return;
				}
			}
		}
	}
	
	@EventHandler (priority = EventPriority.HIGH)
	public void onHangingBreak(HangingBreakEvent event) {
		if (event.isCancelled() || (event.getEntity().getType() != EntityType.ITEM_FRAME)) return;
		ItemFrame entity = (ItemFrame)event.getEntity();
		Frame frame = this.manager.getFrame(entity);
		if (frame == null) return;
		
		Player player = null;
		if (event instanceof HangingBreakByEntityEvent) {
			Entity remover = ((HangingBreakByEntityEvent)event).getRemover();
			if (remover.getType() == EntityType.PLAYER) {
				player = (Player) remover;
			}
		}

		if ((player != null) && Config.WORLDGUARD_ENABLED && Config.WORLDGUARD_BREAK && !player.hasPermission("FramePicture.ignoreWorldGuard"))
		{
			RegionManager rm = FramePicturePlugin.getWorldGuard().getRegionManager(player.getWorld());
			LocalPlayer localPlayer = FramePicturePlugin.getWorldGuard().wrapPlayer(player);
			if (!rm.getApplicableRegions(frame.getLocation()).canBuild(localPlayer)) {
				player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
				event.setCancelled(true);
				return;
			}
		}
		
		this.manager.removeFrame(frame);
		if (player != null)
			player.sendMessage(Lang.PREFIX.getText() + Lang.FRAME_REMOVED.getText().replace("%id", String.valueOf(frame.getId())));
		
		for (Entity e : entity.getNearbyEntities(32.0, 32.0, 32.0)) {
			if (e.getType() != EntityType.PLAYER) continue;
			Player p = (Player)e;
			this.sendFrameDestroy(p, entity.getEntityId());
		}
	}
	
	@EventHandler
	public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
		PacketSender.packetsToSend.remove(event.getPlayer());
	}
	
	@EventHandler
	public void onEntityDeath(EntityDeathEvent event) {
		if (event.getEntity().getType() == EntityType.ITEM_FRAME) {
			ItemFrame entity = (ItemFrame)event.getEntity();
			Frame frame = this.manager.getFrame(entity);
			if (frame != null) {
				this.manager.removeFrame(frame);
			}
		}
	}
	
	public void sendFrameDestroy(Player player, int entityID) {
		try {
			PacketContainer destroyPacket = ProtocolLibrary.getProtocolManager().createPacket(PacketType.Play.Server.ENTITY_DESTROY);
			destroyPacket.getIntegerArrays().write(0, new int[] { entityID });
			ProtocolLibrary.getProtocolManager().sendServerPacket(player, destroyPacket);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
