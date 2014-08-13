package de.howaner.FramePicture;

import com.comphenix.protocol.ProtocolLibrary;
import de.howaner.FramePicture.command.FramePictureCommand;
import de.howaner.FramePicture.listener.ChunkListener;
import de.howaner.FramePicture.listener.FrameListener;
import de.howaner.FramePicture.listener.FramePacketListener;
import de.howaner.FramePicture.util.Config;
import de.howaner.FramePicture.util.Frame;
import de.howaner.FramePicture.util.Lang;
import de.howaner.FramePicture.util.PacketSender;
import de.howaner.FramePicture.util.PictureDatabase;
import de.howaner.FramePicture.util.Utils;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import net.minecraft.server.v1_7_R4.EntityPlayer;
import net.minecraft.server.v1_7_R4.EntityTracker;
import net.minecraft.server.v1_7_R4.EntityTrackerEntry;
import net.minecraft.server.v1_7_R4.WorldServer;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class FrameManager {
	public FramePicturePlugin p;
	public static File framesFile = new File("plugins/FramePicture/frames.yml");
	private final Map<String, List<Frame>> frames = new HashMap<String, List<Frame>>();
	private PictureDatabase pictureDB;
	
	public FrameManager(FramePicturePlugin plugin) {
		this.p = plugin;
	}
	
	public void onEnable() {
		if (!Config.configFile.exists()) Config.save();
		Config.load();
		Config.save();
		
		Lang.load();
		
		this.pictureDB = new PictureDatabase();
		this.pictureDB.startScheduler();
		this.loadFrames();
		this.saveFrames();
		
		Bukkit.getPluginManager().registerEvents(new ChunkListener(this), this.p);
		Bukkit.getPluginManager().registerEvents(new FrameListener(this), this.p);
		
		p.getCommand("FramePicture").setExecutor(new FramePictureCommand(this));
		
		ProtocolLibrary.getProtocolManager().addPacketListener(new FramePacketListener());
		
		if (Config.MONEY_ENABLED && FramePicturePlugin.getEconomy() == null) {
			FramePicturePlugin.log.warning("Vault not found. Money Support disabled!");
			Config.MONEY_ENABLED = false;
			Config.save();
		}
		
		if (Config.WORLDGUARD_ENABLED && FramePicturePlugin.getWorldGuard() == null) {
			FramePicturePlugin.log.warning("WorldGuard not found. WorldGuard Support disabled!");
			Config.WORLDGUARD_ENABLED = false;
			Config.save();
		}
		
		if (Config.FRAME_LOAD_ON_START)
			this.cacheFrames();
		
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this.p, new PacketSender(), Config.FRAME_LOADING_DELAY, Config.FRAME_LOADING_DELAY);
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
		long memory = Utils.getUsedMemory();
		
		int amount = 0;
		for (List<Frame> frameList : this.frames.values()) {
			for (Frame frame : frameList) {
				frame.sendTo(null);
				amount++;
			}
		}
		FramePicturePlugin.log.info("Cached " + amount + " frames!");
		long usedMemory = Utils.getUsedMemory() - memory;
		if (usedMemory > 0L)
			FramePicturePlugin.log.info("The frame cache use " + usedMemory + "mb memory!");
	}
	
	public PictureDatabase getPictureDatabase() {
		return this.pictureDB;
	}
	
	public void removeFrame(Frame frame) {
		if (frame == null) return;
		
		Chunk chunk = frame.getLocation().getChunk();
		List<Frame> frameList = this.getFramesInChunk(chunk.getX(), chunk.getZ());
		frameList.remove(frame);
		this.setFramesInChunk(chunk.getX(), chunk.getZ(), frameList);
		
		if (Config.FRAME_REMOVE_IMAGES && this.getFramesWithImage(frame.getPicture()).isEmpty()) {
			if (this.pictureDB.deleteImage(frame.getPicture())) {
				FramePicturePlugin.log.log(Level.INFO, "Removed image for frame #{0}.", frame.getId());
			}
		}
		
		this.saveFrames();
	}
	
	public void setFramesInChunk(int chunkX, int chunkZ, List<Frame> frames) {
		this.frames.put(String.format("%d|%d", chunkX, chunkZ), frames);
	}
	
	public Frame getFrame(Location loc, BlockFace face) {
		Chunk chunk = loc.getChunk();
		List<Frame> frameList = this.getFramesInChunk(chunk.getX(), chunk.getZ());
		
		for (Frame frame : frameList) {
			if (!frame.isLoaded()) continue;
			ItemFrame entity = frame.getEntity();
			if (Utils.isSameLocation(entity.getLocation(), loc) && ((face == null) || (entity.getFacing() == face))) {
				return frame;
			}
		}
		return null;
	}
	
	public List<Frame> getFramesInChunk(int chunkX, int chunkZ) {
		List<Frame> frameList = this.frames.get(String.format("%d|%d", chunkX, chunkZ));
		if (frameList == null) {
			frameList = new ArrayList<Frame>();
		}
		return frameList;
	}
	
	public Frame getFrameWithEntityID(Chunk chunk, int entityId) {
		List<Frame> frameList = this.getFramesInChunk(chunk.getX(), chunk.getZ());
		for (Frame frame : frameList) {
			if (frame.isLoaded() && (frame.getEntity().getEntityId() == entityId)) {
				return frame;
			}
		}
		return null;
	}
	
	public void sendFrame(Frame frame) {
		if (!frame.isLoaded()) return;
		ItemFrame entity = frame.getEntity();
		
		WorldServer worldServer = ((CraftWorld)entity.getWorld()).getHandle();
		EntityTracker tracker = worldServer.tracker;
		EntityTrackerEntry trackerEntry = (EntityTrackerEntry) tracker.trackedEntities.d(entity.getEntityId());
		if (trackerEntry == null) return;
		
		for (Object playerNMS : trackerEntry.trackedPlayers) {
			Player player = ((EntityPlayer)playerNMS).getBukkitEntity();
			frame.sendTo(player);
		}
	}
	
	public int getNewFrameID() {
		int highestId = -1;
		for (List<Frame> frameList : this.frames.values()) {
			for (Frame frame : frameList) {
				highestId = Math.max(highestId, frame.getId());
			}
		}
		
		return (highestId + 1);
	}
	
	public Frame addFrame(String pictureURL, ItemFrame entity) {
		Frame frame = new Frame(this.getNewFrameID(), pictureURL, entity.getLocation(), entity.getFacing());
		frame.setEntity(entity);
		
		if (frame.getBufferImage() == null) {
			return null;
		}
		
		Chunk chunk = entity.getLocation().getChunk();
		List<Frame> frameList = this.getFramesInChunk(chunk.getX(), chunk.getZ());
		frameList.add(frame);
		this.setFramesInChunk(chunk.getX(), chunk.getZ(), frameList);
		
		this.frames.put(String.format("%d|%d", chunk.getX(), chunk.getZ()), frameList);
		Utils.setFrameItemWithoutSending(entity, new ItemStack(Material.AIR));
		
		this.sendFrame(frame);
		this.saveFrames();
		return frame;
	}
	
	public List<Frame> getFramesWithImage(String image) {
		List<Frame> frameList = new ArrayList<Frame>();
		for (List<Frame> frames : this.frames.values()) {
			for (Frame frame : frames) {
				if (frame.getPicture().equals(image)) {
					frameList.add(frame);
				}
			}
		}
		return frameList;
	}
	
	public List<Frame> getFrames() {
		List<Frame> frameList = new ArrayList<Frame>();
		for (List<Frame> frames : this.frames.values()) {
			frameList.addAll(frames);
		}
		return frameList;
	}
	
	public Frame getFrame(ItemFrame entity) {
		for (List<Frame> frameList : this.frames.values()) {
			for (Frame frame : frameList) {
				if (frame.isLoaded() && (frame.getEntity().getEntityId() == entity.getEntityId())) {
					return frame;
				}
			}
		}
		return null;
	}
	
	public List<Frame> addMultiFrames(BufferedImage img, ItemFrame[] frames, int vertical, int horizontal) {
		if (frames.length == 0 || horizontal <= 0) return null;
		img = Utils.scaleImage(img, img.getWidth() * vertical, img.getHeight() * horizontal);
		
		int width = img.getWidth() / vertical;
		int height = img.getHeight() / horizontal;
		
		List<Frame> frameList = new ArrayList<Frame>();
		int globalId = this.getNewFrameID();
		int id = globalId;
		//y = Horizontal
		for (int y = 0; y < horizontal; y++) {
			//x = Vertical
			for (int x = 0; x < vertical; x++) {
				BufferedImage frameImg = Utils.cutImage(img, x * width, y * height, width, height);
				frameImg = Utils.scaleImage(frameImg, 128, 128, false);
				File file = this.pictureDB.writeImage(frameImg, String.format("Frame%s_%s-%s", globalId, x, y));
				
				ItemFrame entity = frames[vertical * y + x];
				Utils.setFrameItemWithoutSending(entity, new ItemStack(Material.AIR));
				Frame frame = this.getFrame(entity);
				if (frame != null)
					this.removeFrame(frame);
				
				frame = new Frame(globalId, file.getName(), entity.getLocation(), entity.getFacing());
				frame.setEntity(entity);
				
				Chunk chunk = frame.getLocation().getChunk();
				List<Frame> chunkFrames = this.getFramesInChunk(chunk.getX(), chunk.getZ());
				chunkFrames.add(frame);
				this.setFramesInChunk(chunk.getX(), chunk.getZ(), chunkFrames);
				
				globalId++;
				frameList.add(frame);
				this.sendFrame(frame);
			}
		}
		
		this.saveFrames();
		return frameList;
	}
	
	public void loadFrames() {
		YamlConfiguration config = YamlConfiguration.loadConfiguration(framesFile);
		this.frames.clear();
		
		for (String key : config.getKeys(false)) {
			ConfigurationSection section = config.getConfigurationSection(key);
			final int id = Integer.parseInt(key);
			
			World world =  Bukkit.getWorld(section.getString("World"));
			if (world == null) {
				FramePicturePlugin.log.log(Level.WARNING, "Can't find world {0}!", section.getString("World"));
				continue;
			}
			final Location loc = new Location(world,
					section.getInt("X"),
					section.getInt("Y"),
					section.getInt("Z"));
			
			BlockFace face = null;
			if (section.contains("Facing")) {
				face = BlockFace.valueOf(section.getString("Facing"));
			}
			
			String picture = section.getString("Picture");
			Frame frame = new Frame(id, picture, loc, face);
			Chunk chunk = loc.getChunk();
			
			if (chunk.isLoaded()) {
				ItemFrame entity = Utils.getItemFrameFromChunk(chunk, loc, face);
				if (entity != null) {
					Utils.setFrameItemWithoutSending(entity, new ItemStack(Material.AIR));
					frame.setEntity(entity);
					this.sendFrame(frame);
				}
			}
			
			List<Frame> frameList = this.getFramesInChunk(chunk.getX(), chunk.getZ());
			frameList.add(frame);
			this.setFramesInChunk(chunk.getX(), chunk.getZ(), frameList);
		}
		FramePicturePlugin.log.log(Level.INFO, "Loaded {0} frames!", this.getFrames().size());
	}
	
	public void saveFrames() {
		YamlConfiguration config = new YamlConfiguration();
		
		for (List<Frame> frameList : this.frames.values()) {
			for (Frame frame : frameList) {
				ConfigurationSection section = config.createSection(String.valueOf(frame.getId()));
				
				section.set("Picture", frame.getPicture());
				section.set("World", frame.getLocation().getWorld().getName());
				section.set("X", frame.getLocation().getBlockX());
				section.set("Y", frame.getLocation().getBlockY());
				section.set("Z", frame.getLocation().getBlockZ());
				
				if (frame.getFacing() != null) {
					section.set("Facing", frame.getFacing().name());
				}
			}
		}
		
		try {
			config.save(framesFile);
		} catch (IOException e) {
			FramePicturePlugin.log.log(Level.WARNING, "Error while saving the frames!", e);
		}
	}

}
