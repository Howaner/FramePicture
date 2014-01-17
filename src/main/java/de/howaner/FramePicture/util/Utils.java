package de.howaner.FramePicture.util;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

public class Utils {
	public static File imageFolder = new File("plugins/FramePicture/images");
	public static Map<String, BufferedImage> imageCache = new HashMap<String, BufferedImage>();
	
	public static void checkFolder() {
		if (!imageFolder.exists()) imageFolder.mkdirs();
	}
	
	public static byte[] setCanvasPixel(byte[] buffer, int x, int y, byte color) {
		if ((x < 0) || (y < 0) || (x >= 128) || (y >= 128)) return buffer;
		if (buffer[(y * 128 + x)] != color)
			buffer[(y * 128 + x)] = color;
		return buffer;
	}
	
	public static BufferedImage getPicture(String picture) {
		if (imageCache.containsKey(picture))
			return imageCache.get(picture);
		
		BufferedImage image;
		try {
			if (picture.startsWith("http://") || picture.startsWith("https://") || picture.startsWith("ftp://")) {
				URL url = new URL(picture);
				image = ImageIO.read(url);
			} else {
				File file = new File(imageFolder, picture);
				if (!file.exists())
					return null;
				image = ImageIO.read(file);
			}
		} catch (Exception e) {
			return null;
		}
		
		if (image != null)
			imageCache.put(picture, image);
		return image;
	}
	
	public static boolean isImage(String path) {
		return getPicture(path) != null;
	}
	
	public static BufferedImage scaleImage(BufferedImage image, int width, int height) {
		if (Config.SIZE_CENTER && image.getWidth() < width && image.getHeight() < height) return image;
		if (image.getWidth() == width && image.getHeight() == height) return image;
		float ratio = ((float)image.getHeight()) / image.getWidth();
		int newWidth = width;
		int newHeight = (int) (newWidth * ratio);
		if (newHeight > height) {
			newHeight = height;
			newWidth = (int) (newHeight / ratio);
		}
		BufferedImage resized = new BufferedImage(newWidth, newHeight, image.getType());
		Graphics2D g = resized.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g.drawImage(image, 0, 0, newWidth, newHeight, 0, 0, image.getWidth(), image.getHeight(), null);
		g.dispose();
		return resized;
	}

}
