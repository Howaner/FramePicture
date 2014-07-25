package de.howaner.FramePicture;

import de.howaner.FramePicture.command.FramePictureCommand;
import de.howaner.FramePicture.event.CreateFrameEvent;
import de.howaner.FramePicture.event.RemoveFrameEvent;
import de.howaner.FramePicture.listener.FrameListener;
import de.howaner.FramePicture.listener.FrameLoadListener;
import de.howaner.FramePicture.util.Config;
import de.howaner.FramePicture.util.Frame;
import de.howaner.FramePicture.util.FrameLoader;
import de.howaner.FramePicture.util.Lang;
import de.howaner.FramePicture.util.PictureDatabase;
import de.howaner.FramePicture.util.Utils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;

public class FrameManager {
	public FramePicturePlugin p;
	public static File framesFile = new File("plugins/FramePicture/frames.yml");
	private final Map<Integer, Frame> frames = new HashMap<Integer, Frame>();
	private PictureDatabase pictureDB;
	private FrameLoader frameLoader;
	
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
		//Load FrameLoader
		this.frameLoader = new FrameLoader(this);
		//Load Frames
		this.pictureDB = new PictureDatabase();
		this.pictureDB.startScheduler();
		this.loadFrames();
		this.saveFrames();
		//Listener
		Bukkit.getPluginManager().registerEvents(new FrameListener(this), this.p);
		Bukkit.getPluginManager().registerEvents(new FrameLoadListener(this.frameLoader), this.p);
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
	}
	
	public void onDisable() {
		this.saveFrames();
		if (this.pictureDB != null) {
			this.pictureDB.stopScheduler();
			this.pictureDB.clear();
		}
		Bukkit.getScheduler().cancelTasks(this.p);
	}
	
	public FrameLoader getFrameLoader() {
		return this.frameLoader;
	}
	
	public PictureDatabase getPictureDatabase() {
		return this.pictureDB;
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
		
		return this.isFramePicture(entity.getLocation());
	}
	
	public boolean isFramePicture(Location loc) {
		return this.getFrame(loc) != null;
	}
	
	public boolean removeFrame(int id) {
		return this.removeFrame(this.frames.get(id));
	}
	
	public boolean removeFrame(Frame frame) {
		if (frame == null) return false;
		
		//Event
		RemoveFrameEvent customEvent = new RemoveFrameEvent(frame);
		Bukkit.getPluginManager().callEvent(customEvent);
		if (customEvent.isCancelled()) return false;
		
		this.frames.remove(frame.getId());
		
		if (Config.FRAME_REMOVE_IMAGES && this.getFramesWithImage(frame.getPicture()).isEmpty()) {
			if (this.pictureDB.deleteImage(frame.getPicture())) {
				this.getLogger().log(Level.INFO, "Removed image from frame #{0}.", frame.getId());
			}
		}
		
		this.saveFrames();
		return true;
	}
	
	private int getNewFrameID() {
		int id = -1;
		for (int key : this.frames.keySet()) {
			id = Math.max(id, key);
		}
		
		return id + 1;
	}
	
	public Frame addFrame(String path, ItemFrame entity) {
		Frame frame = new Frame(this.getNewFrameID(), path, entity.getLocation());
		frame.setEntity(entity);
		
		//Event
		CreateFrameEvent customEvent = new CreateFrameEvent(frame, entity);
		Bukkit.getPluginManager().callEvent(customEvent);
		if (customEvent.isCancelled()) return null;
		
		this.frames.put(frame.getId(), frame);
		frame.sendMapData(null);
		//entity.setItem(new ItemStack(Material.AIR));
		
		this.frameLoader.sendFramesToPlayers(Arrays.asList(new Frame[] { frame }));
		this.saveFrames();
		return frame;
	}
	
	public Frame getFrame(int id) {
		return this.frames.get(id);
	}
	
	public Frame getFrame(Location loc) {
		Frame found = null;
		
		for (Frame frame : this.frames.values()) {
			if (Utils.isSameLocation(frame.getLocation(), loc)) {
				found = frame;
				break;
			}
		}
		
		return found;
	}
	
	public List<Frame> getFramesWithImage(String image) {
		List<Frame> frameList = new ArrayList<Frame>();
		for (Frame frame : this.frames.values()) {
			if (frame.getPicture().equals(image))
				frameList.add(frame);
		}
		return frameList;
	}
	
	public List<Frame> getFrames() {
		List<Frame> frameList = new ArrayList<Frame>();
		frameList.addAll(frames.values());
		return frameList;
	}
	
	public List<Frame> addMultiFrames(BufferedImage img, ItemFrame[] frames, int vertical, int horizontal) {
		if (frames.length == 0 || horizontal <= 0) return null;
		img = Utils.scaleImage(img, img.getWidth() * vertical, img.getHeight() * horizontal);
		
		int width = img.getWidth() / vertical;
		int height = img.getHeight() / horizontal;
		
		List<Frame> frameList = new ArrayList<Frame>();
		int globalId = this.getNewFrameID();
		//y = Horizontal
		for (int y = 0; y < horizontal; y++) {
			//x = Vertical
			for (int x = 0; x < vertical; x++) {
				BufferedImage frameImg = Utils.cutImage(img, x * width, y * height, width, height);
				frameImg = Utils.scaleImage(frameImg, 128, 128, false);
				File file = this.pictureDB.writeImage(frameImg, String.format("Frame%s_%s-%s", globalId, x, y));
				
				ItemFrame entity = frames[vertical * y + x];
				//entity.setItem(new ItemStack(Material.AIR));
				
				Frame frame = this.getFrame(entity.getLocation());
				if (frame != null)
					this.frames.remove(frame.getId());
				
				frame = new Frame(this.getNewFrameID(), file.getName(), entity.getLocation());
				frame.setEntity(entity);
				frame.setPicture(file.getName());
				frame.sendMapData(null);
				
				this.frames.put(getNewFrameID(), frame);
				frameList.add(frame);
			}
		}
		
		this.frameLoader.sendFramesToPlayers(frameList);
		this.saveFrames();
		return frameList;
	}
	
	/* Load Frames */
	public void loadFrames() {
		YamlConfiguration config = YamlConfiguration.loadConfiguration(framesFile);
		this.frames.clear();
		
		for (String key : config.getKeys(false)) {
			ConfigurationSection section = config.getConfigurationSection(key);
			final int id = Integer.parseInt(key);
			
			World world =  Bukkit.getWorld(section.getString("World"));
			if (world == null) {
				this.getLogger().log(Level.WARNING, "Can't find World {0} from frame #{1}!", new Object[] {section.getString("World"), String.valueOf(id) });
				continue;
			}
			
			final Location loc = new Location(world,
					section.getInt("X"),
					section.getInt("Y"),
					section.getInt("Z"));
			
			String picture = section.getString("Picture");
			Frame frame = new Frame(id, picture, loc);
			this.frames.put(frame.getId(), frame);
		}
		this.getLogger().log(Level.INFO, "Loaded {0} frames!", this.frames.size());
		
		// Send frames!
		this.frameLoader.sendFramesToPlayers(this.getFrames());
	}
	
	/* Save Frames */
	public void saveFrames() {
		YamlConfiguration config = new YamlConfiguration();
		
		for (Frame frame : this.frames.values()) {
			ConfigurationSection section = config.createSection(String.valueOf(frame.getId()));
			
			section.set("Picture", frame.getPicture());
			section.set("World", frame.getLocation().getWorld().getName());
			section.set("X", frame.getLocation().getBlockX());
			section.set("Y", frame.getLocation().getBlockY());
			section.set("Z", frame.getLocation().getBlockZ());
		}
		
		try {
			config.save(framesFile);
		} catch (IOException e) {
			FramePicturePlugin.log.log(Level.SEVERE, "Error while saving the frames!", e);
		}
	}
}
