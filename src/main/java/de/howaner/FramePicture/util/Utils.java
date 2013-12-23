package de.howaner.FramePicture.util;

import de.howaner.FramePicture.FramePicturePlugin;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ItemFrame;
import org.bukkit.map.MapView;

public class Utils {
	public static File imageFolder = new File("plugins/FramePicture/images");
	
	public static void checkFolder() {
		if (!imageFolder.exists()) imageFolder.mkdirs();
	}
	
	public static List<ItemFrame> getFrameEntitys(short id) {
		List<ItemFrame> frameList = new ArrayList<ItemFrame>();
		for (ItemFrame frame : Bukkit.getWorlds().get(0).getEntitiesByClass(ItemFrame.class)) {
			if (frame.getItem() == null || frame.getItem().getType() != Material.MAP) continue;
			if (frame.getItem().getDurability() == id)
				frameList.add(frame);
		}
		return frameList;
	}
	
	public static BufferedImage getPicture(String picture) throws Exception {
		BufferedImage image;
		if (picture.startsWith("http://") || picture.startsWith("https://") || picture.startsWith("ftp://")) {
			URL url = new URL(picture);
			try {
				image = ImageIO.read(url);
			} catch (Exception e) {
				throw new Exception("No Image!");
			}
		} else {
			File file = new File(imageFolder, picture);
			if (!file.exists()) {
				throw new Exception("Not found!");
			}
			try {
				image = ImageIO.read(file);
			} catch (Exception e) {
				throw new Exception("No Image!");
			}
		}
		return image;
	}
	
	public static boolean isImage(String path) {
		try {
			getPicture(path);
			return true;
		} catch (Exception e) {
			return false;
		}
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
	
	/*public static void removeMapFile(short id) {
		try {
			File worldFolder = new File(Bukkit.getWorlds().get(0).getName());
			File dataFolder = new File(worldFolder, "data");
			File mapFile = new File(dataFolder, "map_" + id + ".dat");
			if (mapFile.exists())
				mapFile.delete();
		} catch (Exception e) {
			FramePicturePlugin.log.warning("Can't remove the Map Data from #" + id);
		}
	}*/
	
	public static short createMapId() {
		MapView view = Bukkit.createMap(Bukkit.getWorlds().get(0));
		return view.getId();
	}

}
