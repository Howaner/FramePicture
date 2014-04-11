package de.howaner.FramePicture.util;

import java.io.File;
import java.util.logging.Level;

import org.bukkit.configuration.file.YamlConfiguration;

import de.howaner.FramePicture.FramePicturePlugin;

public class Config {
	
	public static boolean CHANGE_SIZE_ENABLED = true;
	public static int SIZE_WIDTH = 128;
	public static int SIZE_HEIGHT = 128;
	public static boolean SIZE_CENTER = true;
	public static boolean MONEY_ENABLED = false;
	public static double CREATE_PRICE = 10.0;
	public static boolean WORLDGUARD_ENABLED = false;
	public static boolean WORLDGUARD_BUILD = true;
	public static boolean WORLDGUARD_BREAK = true;
	public static boolean FRAME_REMOVE_IMAGES = false;
	public static boolean FRAME_LOAD_ON_START = true;
	//File
	public static File configFile = new File("plugins/FramePicture/config.yml");
	
	public static void load() {
		YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		CHANGE_SIZE_ENABLED = config.getBoolean("AutoSize.Enabled");
		SIZE_WIDTH = config.getInt("AutoSize.Width");
		SIZE_HEIGHT = config.getInt("AutoSize.Height");
		SIZE_CENTER = config.getBoolean("AutoSize.Center");
		MONEY_ENABLED = config.getBoolean("Money.Enabled");
		CREATE_PRICE = config.getDouble("Money.CreatePrice");
		WORLDGUARD_ENABLED = config.getBoolean("WorldGuard.Enabled");
		WORLDGUARD_BUILD = config.getBoolean("WorldGuard.ProtectBuild");
		WORLDGUARD_BREAK = config.getBoolean("WorldGuard.ProtectBreak");
		if (config.isBoolean("Frame.RemoveImageWhenFrameDestroy"))
			FRAME_REMOVE_IMAGES = config.getBoolean("Frame.RemoveImageWhenFrameDestroy");
		if (config.isBoolean("Frame.LoadOnStart"))
			FRAME_LOAD_ON_START = config.getBoolean("Frame.CacheOnStart");
		
		FramePicturePlugin.log.info("Configuration loaded!");
	}
	
	public static void save() {
		YamlConfiguration config = new YamlConfiguration();
		config.set("AutoSize.Enabled", CHANGE_SIZE_ENABLED);
		config.set("AutoSize.Width", SIZE_WIDTH);
		config.set("AutoSize.Height", SIZE_HEIGHT);
		config.set("AutoSize.Center", SIZE_CENTER);
		config.set("Money.Enabled", MONEY_ENABLED);
		config.set("Money.CreatePrice", CREATE_PRICE);
		config.set("WorldGuard.Enabled", WORLDGUARD_ENABLED);
		config.set("WorldGuard.ProtectBuild", WORLDGUARD_BUILD);
		config.set("WorldGuard.ProtectBreak", WORLDGUARD_BREAK);
		config.set("Frame.RemoveImageWhenFrameDestroy", FRAME_REMOVE_IMAGES);
		config.set("Frame.CacheOnStart", FRAME_LOAD_ON_START);
		try {
			config.save(configFile);
		} catch (Exception e) {
			FramePicturePlugin.log.log(Level.WARNING, "Error while saving the Config!");
			e.printStackTrace();
		}
	}

}
