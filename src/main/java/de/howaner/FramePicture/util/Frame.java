package de.howaner.FramePicture.util;

import de.howaner.FramePicture.FramePicturePlugin;
import de.howaner.FramePicture.render.ImageRenderer;
import de.howaner.FramePicture.render.TextRenderer;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import net.minecraft.server.v1_7_R4.DataWatcher;
import net.minecraft.server.v1_7_R4.EntityItemFrame;
import net.minecraft.server.v1_7_R4.PacketPlayOutEntityMetadata;
import net.minecraft.server.v1_7_R4.PacketPlayOutMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftItemFrame;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_7_R4.inventory.CraftItemStack;
import org.bukkit.craftbukkit.v1_7_R4.map.RenderData;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.map.MapRenderer;

public class Frame {
	private final int id;
	private ItemFrame entity;
	private final BlockFace face;
	private final Location loc;
	private final String picture;
	private PacketPlayOutEntityMetadata cachedItemPacket = null;
	private PacketPlayOutMap[] cachedDataPacket = null;
	
	public Frame(final int id, String picture, Location loc, BlockFace face) {
		this.id = id;
		this.picture = picture;
		this.loc = loc;
		this.face = face;
	}
	
	public boolean isLoaded() {
		return (this.entity != null);
	}
	
	public int getId() {
		return this.id;
	}
	
	public short getMapId() {
		return (short)(2300 + this.id);
	}
	
	public Location getLocation() {
		return this.loc;
	}
	
	public ItemFrame getEntity() {
		return this.entity;
	}
	
	public BlockFace getFacing() {
		return this.face;
	}
	
	public String getPicture() {
		return this.picture;
	}
	
	public void setEntity(ItemFrame entity) {
		this.entity = entity;
		this.cachedItemPacket = null;
	}
	
	public void clearCache() {
		this.cachedDataPacket = null;
		this.cachedItemPacket = null;
	}
	
	public BufferedImage getBufferImage() {
		BufferedImage image = FramePicturePlugin.getManager().getPictureDatabase().loadImage(this.picture);
		if (image != null && Config.CHANGE_SIZE_ENABLED)
			image = Utils.scaleImage(image, Config.SIZE_WIDTH, Config.SIZE_HEIGHT);
		return image;
	}
	
	private RenderData getRenderData() {
		RenderData render = new RenderData();
		MapRenderer mapRenderer = this.generateRenderer();
		
		Arrays.fill(render.buffer, (byte)0);
		render.cursors.clear();
		
		FakeMapCanvas canvas = new FakeMapCanvas();
		canvas.setBase(render.buffer);
		mapRenderer.render(canvas.getMapView(), canvas, null);
		
		byte[] buf = canvas.getBuffer();
		for (int i = 0; i < buf.length; i++) {
			byte color = buf[i];
			if ((color >= 0) || (color <= -113)) render.buffer[i] = color;
		}
		
		return render;
	}
	
	public void sendTo(Player player) {
		this.sendItemMeta(player);
		this.sendMapData(player);
	}
	
	private void sendItemMeta(Player player) {
		if (!this.isLoaded()) return;
		
		if (this.cachedItemPacket == null) {
			EntityItemFrame entity = ((CraftItemFrame)this.entity).getHandle();

			ItemStack item = new ItemStack(Material.MAP);
			item.setDurability(this.getMapId());

			net.minecraft.server.v1_7_R4.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
			nmsItem.count = 1;
			nmsItem.a(entity);

			DataWatcher watcher = new DataWatcher(entity);
			watcher.add(2, 5);
			watcher.a(3, (byte)0);
			watcher.watch(2, nmsItem);
			watcher.update(2);

			this.cachedItemPacket = new PacketPlayOutEntityMetadata(entity.getId(), watcher, false);
		}
		
		if (player != null)
			((CraftPlayer)player).getHandle().playerConnection.sendPacket(this.cachedItemPacket);
	}
	
	private void sendMapData(Player player) {
		if (this.cachedDataPacket == null) {
			this.cachedDataPacket = new PacketPlayOutMap[128];
			
			RenderData data = this.getRenderData();
			for (int x = 0; x < 128; x++) {
				byte[] bytes = new byte[''];
				bytes[1] = ((byte)x);
				for (int y = 0; y < 128; y++) {
					bytes[(y + 3)] = data.buffer[y * 128 + x];
				}
				
				this.cachedDataPacket[x] = new PacketPlayOutMap(this.getMapId(), bytes);
			}
		}
		
		if (player != null)
			PacketSender.addPacketToPlayer(player, this.cachedDataPacket);
	}
	
	public MapRenderer generateRenderer() {
		BufferedImage image = Frame.this.getBufferImage();
		if (image == null) {
			FramePicturePlugin.log.warning("The picture \"" + Frame.this.getPicture() + "\" from frame #" + Frame.this.getId() + " doesn't exists!");
			return new TextRenderer("Can't read image!", this.getId());
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
