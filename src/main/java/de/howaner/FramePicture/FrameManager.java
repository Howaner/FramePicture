package de.howaner.FramePicture;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import de.howaner.FramePicture.tracker.FakeEntityTracker;
import de.howaner.FramePicture.tracker.FakeEntityTrackerEntry;
import de.howaner.FramePicture.util.Config;
import de.howaner.FramePicture.util.Frame;
import de.howaner.FramePicture.util.Lang;
import de.howaner.FramePicture.util.PictureDatabase;
import de.howaner.FramePicture.util.Utils;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.server.v1_7_R3.EntityItemFrame;
import net.minecraft.server.v1_7_R3.EntityPlayer;
import net.minecraft.server.v1_7_R3.EntityTracker;
import net.minecraft.server.v1_7_R3.EntityTrackerEntry;
import net.minecraft.server.v1_7_R3.WorldServer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.craftbukkit.v1_7_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class FrameManager {
	public FramePicturePlugin p;
	public static File framesFile = new File("plugins/FramePicture/frames.yml");
	private final Map<Integer, Frame> frames = new HashMap<Integer, Frame>();
	private PictureDatabase pictureDB;
	
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
		this.pictureDB = new PictureDatabase();
		this.pictureDB.startScheduler();
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
			frame.setBukkitItem(new ItemStack(Material.AIR));
		
		if (Config.FRAME_LOAD_ON_START)
			this.cacheFrames();
		
		for (World world : Bukkit.getWorlds())
			this.replaceTracker(world);
	}
	
	public void onDisable() {
		this.saveFrames();
		if (this.pictureDB != null) {
			this.pictureDB.stopScheduler();
			this.pictureDB.clear();
		}
		Bukkit.getScheduler().cancelTasks(this.p);
	}
	
	public void cacheFrames() {
		FramePicturePlugin.log.info("Caching frames ...");
		long memory = Utils.getUsedRam();
		for (Frame frame : this.frames.values()) {
			frame.sendItemMeta(null);
			frame.sendMapData(null);
		}
		FramePicturePlugin.log.info("Cached " + this.frames.size() + " frames!");
		long usedMemory = Utils.getUsedRam() - memory;
		if (usedMemory > 0L)
			FramePicturePlugin.log.info("The frame cache use " + usedMemory + "mb memory!");
	}
	
	public void sendFrameToPlayers(Frame frame) {
		WorldServer server = ((CraftWorld)frame.getLocation().getWorld()).getHandle();
		EntityTracker tracker = server.tracker;
		EntityTrackerEntry entry = (EntityTrackerEntry) tracker.trackedEntities.get(frame.getEntity().getEntityId());
		
		entry.trackedPlayers.clear();
		
		/*for (EntityPlayer player : (List<EntityPlayer>)server.players) {
			if (player.removeQueue.contains(frame.getEntity().getEntityId()))
				player.removeQueue.remove(frame.getEntity().getEntityId());
		}*/
		entry.scanPlayers(server.players);
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
		ItemFrame iFrame = (ItemFrame)entity;
		if (iFrame.getItem().getType() != Material.MAP)
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
				this.getLogger().log(Level.INFO, "Removed Image for Frame #{0}.", frame.getId());
			}
		}
		
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
		final Frame frame = new Frame(this.getNewFrameID(), entity, path);
		//Event
		CreateFrameEvent customEvent = new CreateFrameEvent(frame, entity);
		Bukkit.getPluginManager().callEvent(customEvent);
		if (customEvent.isCancelled()) return null;
		
		this.frames.put(frame.getId(), frame);
		frame.setBukkitItem(new ItemStack(Material.AIR));
		
		this.saveFrames();
		this.sendFrameToPlayers(frame);
		return frame;
	}
	
	public Frame getFrame(int id) {
		return this.frames.get(id);
	}
	
	public Frame getFrame(Location loc) {
		List<Frame> frames = new ArrayList<Frame>();
		synchronized(this.frames) {
			frames.addAll(this.frames.values());
		}
		
		for (Frame frame : frames) {
			if (frame.getLocation().equals(loc))
				return frame;
		}
		return null;
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
				
				ItemFrame entity = (y == 0) ? frames[x] : frames[vertical * y + x];
				
				Frame frame = this.getFrame(entity.getLocation());
				if (frame != null)
					this.frames.remove(frame.getId());
				
				frame = new Frame(this.getNewFrameID(), entity, file.getName());
				frame.setBukkitItem(new ItemStack(Material.AIR));
				frame.setPicture(file.getName());
				frame.sendMapData(null);
				
				this.frames.put(getNewFrameID(), frame);
				frameList.add(frame);
				this.sendFrameToPlayers(frame);
			}
		}
		
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
				this.getLogger().log(Level.WARNING, "Can''t find World {0} for Frame #{1}!", new Object[] {section.getString("World"), String.valueOf(id) });
				continue;
			}
			final Location loc = new Location(world,
					section.getInt("X"),
					section.getInt("Y"),
					section.getInt("Z"));
			
			ItemFrame entity = Utils.getFrameAt(loc);
			if (entity == null) {
				this.getLogger().log(Level.WARNING, "The ItemFrame for Frame #{0] couldn''t found! Is the ItemFrame broken?", id);
				continue;
			}
			final ItemFrame e = entity;
			
			String picture;
			if (section.isString("Path")) {
				picture = section.getString("Path");
				if (picture.startsWith("http://") || picture.startsWith("https://") || picture.startsWith("ftp://") || picture.startsWith("file://")) {
					this.pictureDB.downloadImage(picture, new PictureDatabase.FinishDownloadSignal() {
						@Override
						public void downloadSuccess(File file) {
							Frame frame = new Frame(id, e, file.getName());
							FrameManager.this.frames.put(id, frame);
						}

						@Override
						public void downloadError(Exception e) { }
					});
					continue;
				}
			} else {
				picture = section.getString("Picture");
			}
			
			//Set Frame
			Frame frame = new Frame(id, entity, picture);
			this.frames.put(id, frame);
		}
		this.getLogger().log(Level.INFO, "Loaded {0} Frames!", this.frames.size());
	}
	
	/* Save Frames */
	public void saveFrames() {
		YamlConfiguration config = new YamlConfiguration();
		for (Frame frame : this.getFrames()) {
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
			FramePicturePlugin.log.log(Level.WARNING, "Error while saving the Frames!");
			e.printStackTrace();
		}
	}
	
	public void resendFrames(Player player) {
		WorldServer server = ((CraftWorld)player.getWorld()).getHandle();
		if (!(server.tracker instanceof FakeEntityTracker)) return; //This world is not a Framepicture World
		FakeEntityTracker tracker = (FakeEntityTracker)server.tracker;
		
		try {
			Field field = EntityTracker.class.getDeclaredField("c");
			field.setAccessible(true);
			
			Set set = (Set) field.get(tracker);
			for (Object obj : set) {
				if (!(obj instanceof FakeEntityTrackerEntry)) continue;
				FakeEntityTrackerEntry entry = (FakeEntityTrackerEntry) obj;
				List list = new ArrayList();
				list.add(((CraftPlayer)player).getHandle());
				entry.scanPlayers(list);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void replaceTracker(World world) {
		WorldServer server = ((CraftWorld)world).getHandle();
		EntityTracker oldTracker = server.tracker;
		FakeEntityTracker newTracker = new FakeEntityTracker(server);
		
		// Copy
		try {
			Field field = EntityTracker.class.getDeclaredField("c");
			field.setAccessible(true);
			
			Set set = (Set) field.get(oldTracker);
			Set newSet = new HashSet();
			for (Object obj : set) {
				EntityTrackerEntry entry = (EntityTrackerEntry) obj;
				if (entry.tracker instanceof EntityItemFrame && !(entry instanceof FakeEntityTrackerEntry)) {
					Field uField = EntityTrackerEntry.class.getDeclaredField("u");
					uField.setAccessible(true);
					boolean u = (Boolean) uField.get(entry);
					
					entry = new FakeEntityTrackerEntry(entry.tracker, entry.b, entry.c, u);
				}
				newTracker.trackedEntities.a(entry.tracker.getId(), entry);
				entry.trackedPlayers.clear();
				for (EntityPlayer player : (Set<EntityPlayer>)entry.trackedPlayers) {
					if (entry.tracker.getId() < player.removeQueue.size())
						player.removeQueue.remove(entry.tracker.getId());
				}
				entry.scanPlayers(server.players);
				
				newSet.add(entry);
			}
			newTracker.setPrivateValue("c", newSet);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		server.tracker = newTracker;
		getLogger().info("Entity Tracker from world " + world.getName() + " was replaced!");
	}

}
