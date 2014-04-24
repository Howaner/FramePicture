package de.howaner.FramePicture.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.bukkit.Location;
import org.bukkit.entity.ItemFrame;

public class Utils {
	
	public static long getUsedRam() {
		Runtime runtime = Runtime.getRuntime();
		return ((runtime.totalMemory() - runtime.freeMemory()) / (1024*1024));
	}
	
	public static byte[] setCanvasPixel(byte[] buffer, int x, int y, byte color) {
		if ((x < 0) || (y < 0) || (x >= 128) || (y >= 128)) return buffer;
		if (buffer[(y * 128 + x)] != color)
			buffer[(y * 128 + x)] = color;
		return buffer;
	}
	
	public static BufferedImage scaleImage(BufferedImage image, int width, int height) {
		return scaleImage(image, width, height, true);
	}
	
	public static BufferedImage scaleImage(BufferedImage image, int width, int height, boolean checks) {
		if (checks && Config.SIZE_CENTER && image.getWidth() < width && image.getHeight() < height) return image;
		if (image.getWidth() == width && image.getHeight() == height) return image;
		float ratio = ((float)image.getHeight()) / image.getWidth();
		int newWidth = width;
		int newHeight = height;
		if (checks) {
			newHeight = (int) (newWidth * ratio);
			if (newHeight > height) {
				newHeight = height;
				newWidth = (int) (newHeight / ratio);
			}
		}
		
		BufferedImage resized = new BufferedImage(newWidth, newHeight, image.getType());
		Graphics2D g = resized.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(image, 0, 0, newWidth, newHeight, 0, 0, image.getWidth(), image.getHeight(), null);
		g.dispose();
		return resized;
	}
	
	public static BufferedImage cutImage(BufferedImage image, int posX, int posY, int width, int height) {
		return image.getSubimage(posX, posY, width, height);
	}
	
	public static ItemFrame getFrameAt(Location loc) {
		for (ItemFrame frame : loc.getWorld().getEntitiesByClass(ItemFrame.class)) {
			if (frame.getLocation().getBlockX() == loc.getBlockX() &&
				frame.getLocation().getBlockY() == loc.getBlockY() &&
				frame.getLocation().getBlockZ() == loc.getBlockZ())
				return frame;
		}
		return null;
	}
	
	public static boolean isImage(File file) {
		try {
			BufferedImage image = ImageIO.read(file);
			return (image != null);
		} catch (Exception e) {
			return false;
		}
	}
	
	public static int diff(int v1, int v2) {
		return Math.max(v1, v2) - Math.min(v1, v2);
	}

}
