package de.howaner.FramePicture;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.Material;
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
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.map.MapView;

public class FrameManager {
	public FramePicturePlugin p;
	public static File framesFile = new File("plugins/FramePicture/frames.yml");
	private Map<Integer, Frame> frames = new HashMap<Integer, Frame>();
	
	public FrameManager(FramePicturePlugin plugin) {
		this.p = plugin;
	}
	
	public List<Frame> getFramesInRadius(Location loc, int radius) {
		int minX = loc.getBlockX() - (radius / 2);
		int minZ = loc.getBlockZ() - (radius / 2);
		int maxX = loc.getBlockX() + (radius / 2);
		int maxZ = loc.getBlockZ() + (radius / 2);
		
		List<Frame> frameList = new ArrayList<Frame>();
		for (Frame frame : this.getFrames()) {
			if (frame.getLocation().getWorld() == loc.getWorld() &&
					frame.getLocation().getBlockX() >= minX &&
					frame.getLocation().getBlockX() <= maxX &&
					frame.getLocation().getBlockZ() >= minZ &&
					frame.getLocation().getBlockZ() <= maxZ)
				frameList.add(frame);
		}
		return frameList;
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
		this.saveFrames();
		//Listener
		Bukkit.getPluginManager().registerEvents(new FrameListener(this), this.p);
		//Command
		p.getCommand("FramePicture").setExecutor(new FramePictureCommand(this));
		p.getCommand("fp").setExecutor(new FramePictureCommand(this));
		
		//Money
		if (Config.MONEY_ENABLED && FramePicturePlugin.getEconomy() == null) {
			this.getLogger().warning("Vault not found. Money Support disabled!");
			Config.MONEY_ENABLED = false;
			Config.save();
		}
		//WorldGuard
		if (Config.WORLDGUARD_ENABLED && FramePicturePlugin.getWorldGuard() == null) {
			this.getLogger().warning("WorldGuard not found. WorldGuard Support disabled!");
			Config.WORLDGUARD_ENABLED = false;
			Config.save();
		}
		
		for (Frame frame : this.getFrames())
			frame.checkPlayers();
	}
	
	public void onDisable() {
		this.saveFrames();
		Bukkit.getScheduler().cancelTasks(this.p);
		Utils.imageCache.clear();
	}
	
	public Logger getLogger() {
		return FramePicturePlugin.log;
	}
	
	public boolean isFramePicture(int id) {
		return frames.containsKey(id);
	}
	
	public boolean isFramePicture(Entity entity) {
		if (entity.getType() != EntityType.ITEM_FRAME)
			return false;
		ItemFrame iFrame = (ItemFrame)entity;
		if (iFrame.getItem().getType() != Material.MAP)
			return false;
		
		return this.isFramePicture(entity.getLocation());
		
		//return this.isFramePicture(iFrame.getItem().getDurability() - 1024);
	}
	
	public boolean isFramePicture(Location loc) {
		return this.getFrame(loc) != null;
	}
	
	public boolean removeFrame(Frame frame) {
		for (Entry<Integer, Frame> e : this.frames.entrySet()) {
			if (frame == e.getValue()) {
				this.removeFrame(e.getKey());
				return true;
			}
		}
		return false;
	}
	
	public boolean removeFrame(int id) {
		Frame frame = this.getFrame(id);
		if (frame == null) return false;
		//Event
		RemoveFrameEvent customEvent = new RemoveFrameEvent(frame);
		Bukkit.getPluginManager().callEvent(customEvent);
		if (customEvent.isCancelled()) return false;
		
		this.frames.remove(id);
		//Utils.removeMapFile(mapId);
		this.saveFrames();
		return true;
	}
	
	private int getNewFrameID() {
		int id = -1;
		for (int key : this.frames.keySet())
			if (key > id)
				id = key;
		
		id += 1;
		return id;
	}
	
	public Frame addFrame(String path, ItemFrame entity) {
		Frame frame = new Frame(this.getNewFrameID(), entity, path);
		//Event
		CreateFrameEvent customEvent = new CreateFrameEvent(frame, entity);
		Bukkit.getPluginManager().callEvent(customEvent);
		if (customEvent.isCancelled()) return null;
		
		this.frames.put(frame.getId(), frame);
		entity.setItem(new ItemStack(Material.AIR, 1));
		
		frame.checkPlayers();
		
		this.saveFrames();
		return frame;
	}
	
	public Frame getFrame(int id) {
		return this.frames.get(id);
	}
	
	public Frame getFrame(Location loc) {
		for (Frame frame : this.frames.values()) {
			if (frame.getLocation().equals(loc))
				return frame;
		}
		return null;
	}
	
	public List<Frame> getFrames() {
		List<Frame> frameList = new ArrayList<Frame>();
		frameList.addAll(frames.values());
		return frameList;
	}
	
	public void convertFramesFile() {
		this.getLogger().info("Converting frames.yml ...");
		YamlConfiguration config = YamlConfiguration.loadConfiguration(framesFile);
		YamlConfiguration newConfig = new YamlConfiguration();
		
		int i = 0;
		for (String key : config.getKeys(false)) {
			short id = Short.parseShort(key);
			String path = config.getString(key);
			
			for (ItemFrame entity : Bukkit.getWorlds().get(0).getEntitiesByClass(ItemFrame.class)) {
				if (entity.getItem() == null || entity.getItem().getType() != Material.MAP) continue;
				if (entity.getItem().getDurability() == id) {
					ConfigurationSection section = newConfig.createSection(String.valueOf(i));
					section.set("Path", path);
					section.set("World", entity.getLocation().getWorld().getName());
					section.set("X", entity.getLocation().getBlockX());
					section.set("Y", entity.getLocation().getBlockY());
					section.set("Z", entity.getLocation().getBlockZ());
					this.getLogger().info("Converted Map #" + String.valueOf(id) + "!");
					entity.setItem(new ItemStack(Material.AIR));
					i += 1;
					break;
				}
			}
			if (!newConfig.contains(String.valueOf(i-1))) {
				this.getLogger().info("Can't convert Map #" + id + "!");
			}
		}
		
		try {
			framesFile.renameTo(new File(framesFile.getParentFile(), "frames.yml.old"));
			framesFile = new File("plugins/FramePicture/frames.yml");
			newConfig.save(framesFile);
			this.getLogger().info("Converted " + i + " Frames successfull!");
		} catch (Exception e) {
			this.getLogger().warning("Error while converting: " + e.getMessage());
			e.printStackTrace();
		}
	}
	
	/* Load Frames */
	public void loadFrames() {
		YamlConfiguration config = YamlConfiguration.loadConfiguration(framesFile);
		this.frames.clear();
		for (String key : config.getKeys(false)) {
			ConfigurationSection section = config.getConfigurationSection(key);
			if (section == null || !section.contains("World") || !section.contains("X") || !section.contains("Y") || !section.contains("Z")) {
				this.convertFramesFile();
				this.loadFrames();
				return;
			}
			
			int id = Integer.parseInt(key);
			String path = section.getString("Path");
			Location loc = new Location(Bukkit.getWorld(section.getString("World")),
					section.getInt("X"),
					section.getInt("Y"),
					section.getInt("Z"));
			
			ItemFrame entity = null;
			//Search ItemFrame
			for (ItemFrame e : Bukkit.getWorld(section.getString("World")).getEntitiesByClass(ItemFrame.class)) {
				if (e.getLocation().getWorld() == loc.getWorld() &&
						e.getLocation().getBlockX() == loc.getBlockX() &&
						e.getLocation().getBlockY() == loc.getBlockY() &&
						e.getLocation().getBlockZ() == loc.getBlockZ()) {
					entity = e;
					break;
				}
			}
			
			if (entity == null) {
				this.getLogger().warning("Can't find Map for #" + id + "!");
				continue;
			}
			
			//Set Frame
			Frame frame = new Frame(id, entity, path);
			this.frames.put(id, frame);
		}
		this.getLogger().info("Loaded " + this.frames.size() + " Frames!");
	}
	
	/* Save Frames */
	public void saveFrames() {
		YamlConfiguration config = new YamlConfiguration();
		for (Frame frame : this.getFrames()) {
			ConfigurationSection section = config.createSection(String.valueOf(frame.getId()));
			
			section.set("Path", frame.getPath());
			section.set("World", frame.getLocation().getWorld().getName());
			section.set("X", frame.getLocation().getBlockX());
			section.set("Y", frame.getLocation().getBlockY());
			section.set("Z", frame.getLocation().getBlockZ());
		}
		
		try {
			config.save(framesFile);
		} catch (Exception e) {
			FramePicturePlugin.log.log(Level.WARNING, "Error while saving the Frames!");
			e.printStackTrace();
		}
	}

}
