package de.howaner.FramePicture;

import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import de.howaner.FramePicture.util.Lang;

public class FramePicturePlugin extends JavaPlugin {
	public static Logger log;
	private static FrameManager manager = null;
	private static FramePicturePlugin instance;
	
	@Override
	public void onLoad() {
		log = this.getLogger();
		instance = this;
		manager = new FrameManager(this);
	}
	
	@Override
	public void onEnable() {
		if (log == null) log = this.getLogger();
		if (manager == null) manager = new FrameManager(this);
		manager.onEnable();
		log.info(Lang.PLUGIN_ENABLED.getText());
	}
	
	@Override
	public void onDisable() {
		manager.onDisable();
		log.info(Lang.PLUGIN_DISABLED.getText());
	}
	
	public static FramePicturePlugin getPlugin() {
		if (instance == null) {
			Plugin plugin = Bukkit.getPluginManager().getPlugin("FramePicture");
			if (plugin != null && plugin instanceof FramePicturePlugin)
				instance = (FramePicturePlugin)plugin;
		}
		return instance;
	}
	
	public static FrameManager getManager() {
		return manager;
	}

}
