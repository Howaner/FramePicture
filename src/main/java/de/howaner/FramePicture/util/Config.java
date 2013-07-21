package de.howaner.FramePicture.util;

import java.io.File;
import java.util.logging.Level;

import org.bukkit.configuration.file.YamlConfiguration;

import de.howaner.FramePicture.FramePicturePlugin;

public class Config {
	
	public static boolean CHANGE_SIZE_ENABLED = true;
	public static int SIZE_WIDTH = 134;
	public static int SIZE_HEIGHT = 134;
	public static boolean MONEY_ENABLED = false;
	public static double CREATE_PRICE = 10.0;
	//File
	public static File configFile = new File("plugins/FramePicture/config.yml");
	
	public static void load() {
		YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		CHANGE_SIZE_ENABLED = config.getBoolean("AutoSize.Enabled");
		SIZE_WIDTH = config.getInt("AutoSize.Width");
		SIZE_HEIGHT = config.getInt("AutoSize.Height");
		MONEY_ENABLED = config.getBoolean("Money.Enabled");
		CREATE_PRICE = config.getDouble("Money.CreatePrice");
	}
	
	public static void save() {
		YamlConfiguration config = new YamlConfiguration();
		config.set("AutoSize.Enabled", CHANGE_SIZE_ENABLED);
		config.set("AutoSize.Width", SIZE_WIDTH);
		config.set("AutoSize.Height", SIZE_HEIGHT);
		config.set("Money.Enabled", MONEY_ENABLED);
		config.set("Money.CreatePrice", CREATE_PRICE);
		try {
			config.save(configFile);
		} catch (Exception e) {
			FramePicturePlugin.log.log(Level.WARNING, "Fehler beim Speichern der Konfiguration!");
		}
	}

}
