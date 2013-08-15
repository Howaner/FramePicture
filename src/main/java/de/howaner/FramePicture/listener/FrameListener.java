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
		///CREATING
		if (Cache.hasCacheCreating(player)) {
			event.setCancelled(true);
			//Money
			if (Config.MONEY_ENABLED) {
				if (manager.economy.getBalance(player.getName()) < Config.CREATE_PRICE) {
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
			if (Config.WORLDGUARD_ENABLED && Config.WORLDGUARD_REGION_CHECK && !player.hasPermission("FramePicture.ignoreWorldGuard")) {
				RegionManager rm = FramePicturePlugin.getWorldGuard().getRegionManager(player.getWorld());
				LocalPlayer localPlayer = FramePicturePlugin.getWorldGuard().wrapPlayer(player);
				if (!rm.getApplicableRegions(entity.getLocation()).canBuild(localPlayer)) {
					player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
					return;
				}
			}
			
			//Is a Item in the Frame?
			if (entity.getItem() != null && entity.getItem().getType() != Material.AIR) {
				player.sendMessage(Lang.PREFIX.getText() + Lang.ALREADY_FRAME_ITEM.getText());
				return;
			}
			
			//Create Frame
			String path = Cache.getCacheCreating(player);
			if (manager.addFrame(path, entity) != null) {
				Cache.removeCacheCreating(player);
				player.sendMessage(Lang.PREFIX.getText() + Lang.FRAME_SET.getText().replace("%url", path));
				if (Config.MONEY_ENABLED) manager.economy.withdrawPlayer(player.getName(), Config.CREATE_PRICE);
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
			Frame frame = manager.getFrame(entity.getItem().getDurability());
			if (frame == null) {
				player.sendMessage(Lang.NO_FRAMEPICTURE.getText());
				return;
			}
			player.sendMessage(Lang.PREFIX.getText() + Lang.GET_URL.getText().replace("%url", frame.getPath()));
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
	public void onHangingBreak(HangingBreakEvent event) {
		if (event.isCancelled()) return;
		Entity entity = event.getEntity();
		if (manager.isFramePicture(entity))
		{
			ItemFrame iFrame = (ItemFrame)entity;
			manager.removeFrame(iFrame.getItem().getDurability());
		}
	}

}
