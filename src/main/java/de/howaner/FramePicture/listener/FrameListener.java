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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

public class FrameListener implements Listener {
	
	private FrameManager manager;
	
	public FrameListener(FrameManager manager) {
		this.manager = manager;
	}
	
	@EventHandler
	public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
		//Check
		if (event.isCancelled()) return;
		if (event.getRightClicked().getType() != EntityType.ITEM_FRAME) return;
		ItemFrame entity = (ItemFrame) event.getRightClicked();
		Player player = event.getPlayer();
		Frame frame = (entity.getItem() != null && entity.getItem().getType() == Material.MAP) ? manager.getFrame(entity.getItem().getDurability()) : null;
		if (frame != null && Config.WORLDGUARD_ENABLED && Config.WORLDGUARD_ROTATE_FRAME && !player.hasPermission("FramePicture.ignoreWorldGuard")) {
			RegionManager rm = FramePicturePlugin.getWorldGuard().getRegionManager(player.getWorld());
			LocalPlayer localPlayer = FramePicturePlugin.getWorldGuard().wrapPlayer(player);
			if (!rm.getApplicableRegions(entity.getLocation()).canBuild(localPlayer)) {
				player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
				event.setCancelled(true);
				return;
			}
		}
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
				player.sendMessage(Lang.PREFIX.getText() + Lang.FRAME_SET.getText().replace("%url", path).replace("%id", frame.getMapId().toString()));
				if (Config.MONEY_ENABLED) FramePicturePlugin.getEconomy().withdrawPlayer(player.getName(), Config.CREATE_PRICE);
			}
		}
		///GETTING
		if (Cache.hasCacheGetting(player)) {
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
			player.sendMessage(Lang.PREFIX.getText() + Lang.GET_URL.getText().replace("%url", frame.getPath()).replace("%id", frame.getMapId().toString()));
			event.setCancelled(true);
			Cache.removeCacheGetting(player);
		}
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		if (Cache.hasCacheCreating(player)) Cache.removeCacheCreating(player);
		if (Cache.hasCacheGetting(player)) Cache.removeCacheGetting(player);
	}
	
	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled()) return;
		Player player = event.getPlayer();
		Block block = event.getBlock();
		Location loc1 = block.getLocation();
		ItemFrame f = null;
		for (ItemFrame frame : block.getWorld().getEntitiesByClass(ItemFrame.class)) {
			if (!this.manager.isFramePicture(frame)) continue;
			Location loc2 = frame.getLocation();
			if (loc1.getWorld() == loc2.getWorld() &&
					Math.max(loc1.getBlockX(), loc2.getBlockX()) - Math.min(loc1.getBlockX(), loc2.getBlockX()) <= 1 &&
					Math.max(loc1.getBlockY(), loc2.getBlockY()) - Math.min(loc1.getBlockY(), loc2.getBlockY()) <= 1 &&
					Math.max(loc1.getBlockZ(), loc2.getBlockZ()) - Math.min(loc1.getBlockZ(), loc2.getBlockZ()) <= 1) {
				f = frame;
				break;
			}
		}
		if (f != null) {
			//WorldGuard Check
			if (Config.WORLDGUARD_ENABLED && Config.WORLDGUARD_BREAK && !player.hasPermission("FramePicture.ignoreWorldGuard")) {
				RegionManager rm = FramePicturePlugin.getWorldGuard().getRegionManager(player.getWorld());
				LocalPlayer localPlayer = FramePicturePlugin.getWorldGuard().wrapPlayer(player);
				if (!rm.getApplicableRegions(f.getLocation()).canBuild(localPlayer)) {
					player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
					event.setCancelled(true);
				}
			}
		}
	}
	
	@EventHandler
	public void onPlayerPickupItem(PlayerPickupItemEvent event) {
		if (event.isCancelled()) return;
		Player player = event.getPlayer();
		ItemStack item = event.getItem().getItemStack();
		if (item.getType() != Material.MAP) return;
		
		Frame frame = this.manager.getFrame(item.getDurability());
		if (frame != null) {
			this.manager.removeFrame(item.getDurability());
			event.getItem().remove();
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onEntityDamage(EntityDamageEvent event) {
		if (event.isCancelled()) return;
		Entity entity = event.getEntity();
		if (manager.isFramePicture(entity)) {
			ItemFrame iFrame = (ItemFrame)entity;
			if (event instanceof EntityDamageByEntityEvent) {
				if (((EntityDamageByEntityEvent)event).getDamager().getType() == EntityType.PLAYER) {
					Player player = (Player) ((EntityDamageByEntityEvent)event).getDamager();
					//WorldGuard Check
					if (Config.WORLDGUARD_ENABLED && Config.WORLDGUARD_BREAK && !player.hasPermission("FramePicture.ignoreWorldGuard")) {
						RegionManager rm = FramePicturePlugin.getWorldGuard().getRegionManager(player.getWorld());
						LocalPlayer localPlayer = FramePicturePlugin.getWorldGuard().wrapPlayer(player);
						if (!rm.getApplicableRegions(entity.getLocation()).canBuild(localPlayer)) {
							player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
							event.setCancelled(true);
							return;
						}
					}
				}
			}
			short id = iFrame.getItem().getDurability();
			manager.removeFrame(id);
			if (event instanceof EntityDamageByEntityEvent && ((EntityDamageByEntityEvent)event).getDamager().getType() == EntityType.PLAYER) {
				Player player = (Player) ((EntityDamageByEntityEvent)event).getDamager();
				player.sendMessage(Lang.PREFIX.getText() + Lang.FRAME_REMOVED.getText());
			}
		}
	}
	
	@EventHandler
	public void onHangingBreak(HangingBreakEvent event) {
		if (event.isCancelled()) return;
		Entity entity = event.getEntity();
		if (manager.isFramePicture(entity)) {
			ItemFrame iFrame = (ItemFrame)entity;
			if (event instanceof HangingBreakByEntityEvent) {
				if (((HangingBreakByEntityEvent)event).getRemover().getType() == EntityType.PLAYER) {
					Player player = (Player) ((HangingBreakByEntityEvent)event).getRemover();
					//WorldGuard Check
					if (Config.WORLDGUARD_ENABLED && Config.WORLDGUARD_BREAK && !player.hasPermission("FramePicture.ignoreWorldGuard")) {
						RegionManager rm = FramePicturePlugin.getWorldGuard().getRegionManager(player.getWorld());
						LocalPlayer localPlayer = FramePicturePlugin.getWorldGuard().wrapPlayer(player);
						if (!rm.getApplicableRegions(entity.getLocation()).canBuild(localPlayer)) {
							player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
							event.setCancelled(true);
							return;
						}
					}
				}
			}
			short id = iFrame.getItem().getDurability();
			manager.removeFrame(id);
			if (event instanceof HangingBreakByEntityEvent && ((HangingBreakByEntityEvent)event).getRemover().getType() == EntityType.PLAYER) {
				Player player = (Player) ((HangingBreakByEntityEvent)event).getRemover();
				player.sendMessage(Lang.PREFIX.getText() + Lang.FRAME_REMOVED.getText());
			}
		}
	}
	
	@EventHandler
	public void onPlayerJoin(PlayerJoinEvent event) {
		if (!Config.FASTER_RENDERING) return;
		Player player = event.getPlayer();
		FramePicturePlugin.getManager().sendMaps(player);
	}

}
