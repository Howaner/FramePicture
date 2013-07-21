package de.howaner.FramePicture.util;

import java.awt.Image;
import java.io.File;
import java.net.URL;

import javax.imageio.ImageIO;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.map.MapView;

public class Utils {
	public static File imageFolder = new File("plugins/FramePicture/images");
	
	public static void checkFolder() {
		if (!imageFolder.exists()) imageFolder.mkdirs();
	}
	
	public static Image getPicture(String picture) throws Exception {
		Image image;
		if (picture.startsWith("http://") || picture.startsWith("https://") || picture.startsWith("ftp://")) {
			URL url;
			try {
				url = new URL(picture);
			} catch (Exception e) {
				throw new Exception("Not found!");
			}
			try {
				image = ImageIO.read(url);
			} catch (Exception e) {
				throw new Exception("No Image!");
			}
		} else {
			File file = new File(imageFolder, picture);
			if (file == null || !file.exists()) {
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
	
	public static Location getStandardLocation(Location loc) {
		return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
	}
	
	public static short createMapId() {
		MapView view = Bukkit.createMap(Bukkit.getWorlds().get(0));
		return view.getId();
	}
	
	public static MapView generateMap(short mapId) {
		while (true) {
			MapView view = Bukkit.createMap(Bukkit.getWorlds().get(0));
			if (view.getId() == mapId) return view;
			if (view.getId() > mapId) return null;
		}
	}

}
