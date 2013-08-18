package de.howaner.FramePicture;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.milkbowl.vault.economy.Economy;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;

import de.howaner.FramePicture.command.FramePictureCommand;
import de.howaner.FramePicture.event.CreateFrameEvent;
import de.howaner.FramePicture.event.RemoveFrameEvent;
import de.howaner.FramePicture.listener.FrameListener;
import de.howaner.FramePicture.util.Config;
import de.howaner.FramePicture.util.Frame;
import de.howaner.FramePicture.util.Lang;
import de.howaner.FramePicture.util.Utils;
import org.bukkit.entity.Player;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class FrameManager {
	public FramePicturePlugin p;
	public static File framesFile = new File("plugins/FramePicture/frames.yml");
	private Map<Short, Frame> frames = new HashMap<Short, Frame>();
	public Economy economy;
	
	public FrameManager(FramePicturePlugin plugin) {
		this.p = plugin;
	}
	
	public void onEnable() {
		//Config
		if (!Config.configFile.exists()) Config.save();
		Config.load();
		Config.save();
		//Messages
		Lang.load();
		//Load Frames
		Utils.checkFolder();
		this.loadFrames();
		//Listener
		Bukkit.getPluginManager().registerEvents(new FrameListener(this), this.p);
		//Command
		p.getCommand("FramePicture").setExecutor(new FramePictureCommand(this));
		p.getCommand("fp").setExecutor(new FramePictureCommand(this));
		
		//Money
		if (Config.MONEY_ENABLED && FramePicturePlugin.getEconomy() == null) {
			this.getLogger().warning("Vault not found! Money Support disabled!");
			Config.MONEY_ENABLED = false;
			Config.save();
		}
		//WorldGuard
		if (Config.WORLDGUARD_ENABLED && FramePicturePlugin.getWorldGuard() == null) {
			this.getLogger().warning("WorldGuard not found! WorldGuard Support disabled!");
			Config.WORLDGUARD_ENABLED = false;
			Config.save();
		}
	}
	
	public void onDisable() {
		this.saveFrames();
		Bukkit.getScheduler().cancelTasks(this.p);
	}
	
	public Logger getLogger() {
		return FramePicturePlugin.log;
	}
	
	public boolean isFramePicture(short mapId) {
		return frames.containsKey(mapId);
	}
	
	public boolean isFramePicture(Entity entity) {
		if (entity.getType() != EntityType.ITEM_FRAME)
			return false;
		ItemFrame iFrame = (ItemFrame)entity;
		if (iFrame.getItem().getType() != Material.MAP)
			return false;
		return this.isFramePicture(iFrame.getItem().getDurability());
	}
	
	public boolean removeFrame(Frame frame) {
		for (Entry<Short, Frame> e : this.frames.entrySet()) {
			if (frame == e.getValue()) {
				this.removeFrame(e.getKey());
				return true;
			}
		}
		return false;
	}
	
	public boolean removeFrame(short mapId) {
		Frame frame = this.getFrame(mapId);
		if (frame == null) return false;
		//Event
		RemoveFrameEvent customEvent = new RemoveFrameEvent(frame);
		Bukkit.getPluginManager().callEvent(customEvent);
		if (customEvent.isCancelled())
			return false;
		
		//Delete Picture
		MapView view = Bukkit.getMap(frame.getMapId());
		for (MapRenderer renderer : view.getRenderers()) {
			view.removeRenderer(renderer);
		}
		
		this.frames.remove(mapId);
		this.saveFrames();
		return true;
	}
	
	public Frame addFrame(String path, ItemFrame entity) {
		Frame frame = new Frame(path, Utils.createMapId());
		//Event
		CreateFrameEvent customEvent = new CreateFrameEvent(frame, entity);
		Bukkit.getPluginManager().callEvent(customEvent);
		if (customEvent.isCancelled())
			return null;
		
		this.frames.put(frame.getMapId(), frame);
		frame.update();
		entity.setItem(new ItemStack(Material.MAP, 1, frame.getMapId()));
		
		this.saveFrames();
		return frame;
	}
	
	public Frame getFrame(short mapId) {
		return this.frames.get(mapId);
	}
	
	public List<Frame> getFrames() {
		List<Frame> frameList = new ArrayList<Frame>();
		frameList.addAll(frames.values());
		return frameList;
	}
	
	/* Load Frames */
	public void loadFrames() {
		YamlConfiguration config = YamlConfiguration.loadConfiguration(framesFile);
		for (String key : config.getKeys(false)) {
			if (key.startsWith("Frame")) {
				this.getLogger().info("You have a old Frames File Version!");
				this.getLogger().info("A Update is in Progress..");
				this.loadOldFrames();
				this.saveFrames();
				this.loadFrames();
				this.getLogger().info("Update finished!");
				return;
			}
			Short mapId = Short.parseShort(key);
			if (mapId == null)
				continue;
			String path = config.getString(key);
			//Set Frame
			Frame frame = new Frame(path, mapId);
			this.frames.put(mapId, frame);
			frame.update();
		}
	}
	
	/* Save Frames */
	public void saveFrames() {
		YamlConfiguration config = new YamlConfiguration();
		for (Frame frame : this.getFrames()) {
			config.set(frame.getMapId().toString(), frame.getPath());
		}
		try {
			config.save(framesFile);
		} catch (Exception e) {
			FramePicturePlugin.log.log(Level.WARNING, "Error while saving the Frames!");
		}
	}
	
	/* Update Frames */
	public void loadOldFrames() {
		YamlConfiguration config = YamlConfiguration.loadConfiguration(framesFile);
		for (String key : config.getKeys(false))
		{
			//Load
			ConfigurationSection section = config.getConfigurationSection(key);
			String path = section.getString("URL");
			if (!Utils.isImage(path)) continue;
			short mapId = Short.parseShort(section.getString("MapID"));
			//Set Frame
			Frame frame = new Frame(path, mapId);
			this.frames.put(mapId, frame);
			frame.update();
		}
	}
	
	public void sendMaps() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			this.sendMaps(player);
		}
	}
	
	public void sendMaps(Player player) {
		for (Frame frame : this.getFrames()) {
			this.sendMap(frame, player);
		}
	}
	
	public void sendMap(Frame frame) {
		for (Player player : Bukkit.getOnlinePlayers()) {
			this.sendMap(frame, player);
		}
	}
	
	public void sendMap(Frame frame, Player player) {
		if (!Config.FASTER_RENDERING) return;
		player.sendMap(Bukkit.getMap(frame.getMapId()));
	}

}
