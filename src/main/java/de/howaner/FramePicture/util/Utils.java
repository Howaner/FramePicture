package de.howaner.FramePicture.util;

import de.howaner.FramePicture.FramePicturePlugin;
import java.awt.Image;
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
	
	public static Image getPicture(String picture) throws Exception {
		Image image;
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
	
	public static void removeMapFile(short id) {
		try {
			File worldFolder = new File(Bukkit.getWorlds().get(0).getName());
			File dataFolder = new File(worldFolder, "data");
			File mapFile = new File(dataFolder, "map_" + id + ".dat");
			if (mapFile.exists())
				mapFile.delete();
		} catch (Exception e) {
			FramePicturePlugin.log.warning("Can't remove the Map Data from #" + id);
		}
	}
	
	public static short createMapId() {
		MapView view = Bukkit.createMap(Bukkit.getWorlds().get(0));
		return view.getId();
	}

}
