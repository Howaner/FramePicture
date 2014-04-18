package de.howaner.FramePicture;

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import java.util.logging.Logger;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import de.howaner.FramePicture.util.Lang;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;

public class FramePicturePlugin extends JavaPlugin {
	public static Logger log;
	private static FrameManager manager = null;
	private static FramePicturePlugin instance;
	private static Economy economy = null;
	private boolean invalidBukkit = false;
	
	private void checkBukkitVersion() {
		try {
			Class.forName("net.minecraft.server.v1_7_R2.Packet");
			this.invalidBukkit = false;
		} catch (Exception e) {
			this.invalidBukkit = true;
			return;
		}
	}
	
	@Override
	public void onLoad() {
		log = this.getLogger();
		instance = this;
		
		this.checkBukkitVersion();
		if (!this.invalidBukkit)
			manager = new FrameManager(this);
	}
	
	@Override
	public void onEnable() {
		if (log == null) log = this.getLogger();
		if (instance == null) instance = this;
		
		//Check Bukkit Version
		if (this.invalidBukkit) {
			log.severe("You use a not-supported bukkit version!");
			log.severe("This FramePicture version is for Bukkit 1.7.5!");
			Bukkit.getPluginManager().disablePlugin(this);
			return;
		}
		
		if (manager == null) manager = new FrameManager(this);
		this.setupEconomy();
		manager.onEnable();
		log.info(Lang.PLUGIN_ENABLED.getText());
	}
	
	@Override
	public void onDisable() {
		if (manager != null)
			manager.onDisable();
		log.info(Lang.PLUGIN_DISABLED.getText());
	}
	
	public void setupEconomy() {
		if (Bukkit.getPluginManager().getPlugin("Vault") == null) return;
		RegisteredServiceProvider<Economy> economyProvider = Bukkit.getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
		if (economyProvider != null)
            economy = economyProvider.getProvider();
    }
	
	public static Economy getEconomy() {
		return economy;
	}
	
	public static WorldGuardPlugin getWorldGuard() {
		Plugin plugin = Bukkit.getPluginManager().getPlugin("WorldGuard");
		if (plugin != null && plugin instanceof WorldGuardPlugin)
			return (WorldGuardPlugin)plugin;
		else
			return null;
	}
	
	public static FramePicturePlugin getPlugin() {
		return instance;
	}
	
	public static FrameManager getManager() {
		return manager;
	}

}
