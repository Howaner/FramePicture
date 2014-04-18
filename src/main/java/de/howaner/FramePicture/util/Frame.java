package de.howaner.FramePicture.util;

import de.howaner.FramePicture.render.ImageRenderer;
import de.howaner.FramePicture.render.TextRenderer;

import org.bukkit.Bukkit;
import org.bukkit.map.MapRenderer;

import de.howaner.FramePicture.FramePicturePlugin;
import java.awt.image.BufferedImage;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.logging.Level;
import net.minecraft.server.v1_7_R2.DataWatcher;
import net.minecraft.server.v1_7_R2.EntityItemFrame;
import net.minecraft.server.v1_7_R2.NetworkManager;
import net.minecraft.server.v1_7_R2.Packet;
import net.minecraft.server.v1_7_R2.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_7_R2.PacketPlayOutMap;
import net.minecraft.util.io.netty.channel.Channel;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.craftbukkit.v1_7_R2.entity.CraftItemFrame;
import org.bukkit.craftbukkit.v1_7_R2.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R2.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_7_R2.map.RenderData;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Frame {
	private final int id;
	private final ItemFrame entity;
	private String picture;
	private PacketPlayOutEntityMetadata cachedItemPacket = null;
	private PacketPlayOutMap[] cachedDataPacket = null;
	
	public Frame(final int id, ItemFrame entity, String picture) {
		this.id = id;
		this.entity = entity;
		this.picture = picture;
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
	
	public EntityItemFrame getNMSEntity() {
		return ((CraftItemFrame)this.entity).getHandle();
	}
	
	public String getPicture() {
		return this.picture;
	}
	
	public void setPicture(String picture) {
		this.picture = picture;
		this.cachedDataPacket = null;
		this.cachedItemPacket = null;
		
		FramePicturePlugin.getManager().sendFrameToPlayers(this);
	}
	
	public void setBukkitItem(ItemStack item) {
		net.minecraft.server.v1_7_R2.ItemStack nmsStack = CraftItemStack.asNMSCopy(item);
		if (nmsStack != null) {
			nmsStack.count = 1;
			nmsStack.a(this.getNMSEntity());
		}
		
		this.getNMSEntity().getDataWatcher().watch(2, nmsStack);
	}
	
	public BufferedImage getBufferImage() {
		BufferedImage image = FramePicturePlugin.getManager().getPictureDatabase().loadImage(this.picture);
		if (Config.CHANGE_SIZE_ENABLED)
			image = Utils.scaleImage(image, Config.SIZE_WIDTH, Config.SIZE_HEIGHT);
		return image;
	}
	
	public RenderData getRenderData() {
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
	
	public void sendItemMeta(Player player) {
		if (this.cachedItemPacket == null) {
			EntityItemFrame entity = this.getNMSEntity();

			ItemStack item = new ItemStack(Material.MAP);
			item.setDurability(this.getMapId());

			net.minecraft.server.v1_7_R2.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
			nmsItem.count = 1;
			nmsItem.a(entity);

			DataWatcher watcher = new DataWatcher(entity);
			watcher.a(2, 5);
			watcher.a(3, Byte.valueOf((byte)0));
			watcher.watch(2, nmsItem);
			watcher.h(2);

			this.cachedItemPacket = new PacketPlayOutEntityMetadata(entity.getId(), watcher, false);
		}
		
		if (player != null)
			((CraftPlayer)player).getHandle().playerConnection.sendPacket(this.cachedItemPacket);
	}
	
	public void sendMapData(Player player) {
		if (this.cachedDataPacket == null) {
			this.cachedDataPacket = new PacketPlayOutMap[128];
			
			RenderData data = this.getRenderData();
			for (int x = 0; x < 128; x++) {
				byte[] bytes = new byte[''];
				bytes[1] = ((byte)x);
				for (int y = 0; y < 128; y++) {
					bytes[(y + 3)] = data.buffer[(y * 128 + x)];
				}
				
				this.cachedDataPacket[x] = new PacketPlayOutMap(this.getMapId(), bytes);
			}
		}
		
		if (player != null)
			this.sendPacketsFast(player, this.cachedDataPacket);
	}
	
	public void sendPacketsFast(Player player, Packet[] packets) {
		try {
			NetworkManager netty = ((CraftPlayer)player).getHandle().playerConnection.networkManager;
			Field field = NetworkManager.class.getDeclaredField("m");
			field.setAccessible(true);
			Channel channel = (Channel)field.get(netty);
			
			for (Packet packet : packets) {
				channel.write(packet);
			}
			channel.flush();
		} catch (Exception e) {
			FramePicturePlugin.log.log(Level.WARNING, "Cant't send packets!", e);
		}
	}
	
	public MapRenderer generateRenderer() {
		BufferedImage image = Frame.this.getBufferImage();
		if (image == null) {
			FramePicturePlugin.log.warning("The Url \"" + Frame.this.getPicture() + "\" from Frame #" + Frame.this.getId() + " don't exists!");
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
