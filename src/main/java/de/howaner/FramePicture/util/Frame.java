package de.howaner.FramePicture.util;

import de.howaner.FramePicture.render.ImageRenderer;
import de.howaner.FramePicture.render.TextRenderer;

import org.bukkit.Bukkit;
import org.bukkit.map.MapRenderer;

import de.howaner.FramePicture.FramePicturePlugin;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.server.v1_7_R1.DataWatcher;
import net.minecraft.server.v1_7_R1.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_7_R1.PacketPlayOutMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_7_R1.entity.CraftItemFrame;
import org.bukkit.craftbukkit.v1_7_R1.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R1.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_7_R1.map.RenderData;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Frame {
	private final int id;
	private ItemFrame entity;
	private String path;
	private RenderData cache = null;
	private List<Player> seePlayers = new ArrayList<Player>();
	
	public Frame(final int id, ItemFrame entity, String path) {
		this.id = id;
		this.entity = entity;
		this.path = path;
	}
	
	public int getId() {
		return this.id;
	}
	
	public short getMapId() {
		return (short)(1024 + this.id);
	}
	
	public Location getLocation() {
		return this.entity.getLocation();
	}
	
	public ItemFrame getEntity() {
		return this.entity;
	}
	
	public String getPath() {
		return this.path;
	}
	
	public void setPath(String path) {
		this.path = path;
		//TODO: Send to all players
	}
	
	public List<Player> getSeePlayers() {
		return this.seePlayers;
	}
	
	public void checkPlayer(final Player player) {
		new Thread() {
			@Override
			public void run() {
				List<Frame> frames = FramePicturePlugin.getManager().getFramesInRadius(player.getLocation(), Config.SEE_RADIUS);
				if (frames.contains(Frame.this)) {
					if (Frame.this.seePlayers.contains(player)) return;
					Frame.this.seePlayers.add(player);
					try {
						Thread.sleep(1000L);
					} catch (Exception e) {}
					Frame.this.sendMap(player);
				} else {
					if (!Frame.this.seePlayers.contains(player)) return;
					Frame.this.seePlayers.remove(player);
				}
			}
		}.start();
	}
	
	public void checkPlayers() {
		for (Player player : Bukkit.getOnlinePlayers())
			this.checkPlayer(player);
	}
	
	public BufferedImage getPicture() {
		try {
			BufferedImage image = Utils.getPicture(this.getPath());
			if (image == null) return null;
			if (Config.CHANGE_SIZE_ENABLED)
				image = Utils.scaleImage(image, Config.SIZE_WIDTH, Config.SIZE_HEIGHT);
			return image;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public RenderData getRenderData() {
		if (this.cache != null)
			return this.cache;
		
		RenderData render = new RenderData();
		MapRenderer mapRenderer = this.generateRenderer();
		
		Arrays.fill(render.buffer, (byte)0);
		render.cursors.clear();
		
		Player player = (Bukkit.getOnlinePlayers().length == 0) ? null : Bukkit.getOnlinePlayers()[0];
		FakeMapCanvas canvas = new FakeMapCanvas();
		canvas.setBase(render.buffer);
		mapRenderer.render(canvas.getMapView(), canvas, player);
		
		byte[] buf = canvas.getBuffer();
		for (int i = 0; i < buf.length; i++) {
			byte color = buf[i];
			if ((color >= 0) || (color <= -113)) render.buffer[i] = color;
		}
		
		return render;
	}
	
	public void sendMapContent(Player player) {
		net.minecraft.server.v1_7_R1.EntityItemFrame entity = ((CraftItemFrame)this.entity).getHandle();
		
		ItemStack item = new ItemStack(Material.MAP);
		item.setDurability(this.getMapId());
		
		net.minecraft.server.v1_7_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
		nmsItem.count = 1;
		nmsItem.a(entity);
		
		DataWatcher watcher = new DataWatcher(entity);
		//watcher.a(2, 5);
		watcher.a(2, 5);
		watcher.a(3, Byte.valueOf((byte)0));
		watcher.watch(2, nmsItem);
		watcher.h(2);
		
		PacketPlayOutEntityMetadata packet = new PacketPlayOutEntityMetadata(entity.getId(), watcher, false);
		
		/*PacketPlayOutSpawnEntity spawnPacket = new PacketPlayOutSpawnEntity(entity, 71, entity.direction);
		spawnPacket.a(MathHelper.d(entity.x * 32));
		spawnPacket.b(MathHelper.d(entity.y * 32));
		spawnPacket.c(MathHelper.d(entity.z * 32));*/
		
		((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
	}
	
	public void sendMap(Player player) {
		this.sendMapContent(player);
		
		RenderData data = this.getRenderData();
		for (int x = 0; x < 128; x++) {
			byte[] bytes = new byte[''];
			bytes[1] = ((byte)x);
			for (int y = 0; y < 128; y++) {
				bytes[(y + 3)] = data.buffer[(y * 128 + x)];
			}
			PacketPlayOutMap packet = new PacketPlayOutMap(this.getMapId(), bytes);
			((CraftPlayer)player).getHandle().playerConnection.sendPacket(packet);
		}
	}
	
	public MapRenderer generateRenderer() {
		BufferedImage image = Frame.this.getPicture();
		if (image == null) {
			FramePicturePlugin.log.warning("The Url \"" + Frame.this.getPath() + "\" from Frame #" + Frame.this.getId() + " does not exists!");
			return new TextRenderer("Can't read Image!", this.getId());
		}
		
		ImageRenderer renderer = new ImageRenderer(image);
		if (Config.CHANGE_SIZE_ENABLED && Config.SIZE_CENTER) {
			if (image.getWidth() < 128)
				renderer.imageX = (128 - image.getWidth()) / 2;
			if (image.getHeight() < 128)
				renderer.imageY = (128 - image.getHeight()) / 2;
		}
		
		return renderer;
	}

}
