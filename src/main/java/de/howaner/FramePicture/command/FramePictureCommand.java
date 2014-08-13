package de.howaner.FramePicture.command;

import de.howaner.FramePicture.FrameManager;
import de.howaner.FramePicture.FramePicturePlugin;
import de.howaner.FramePicture.util.Cache;
import de.howaner.FramePicture.util.Config;
import de.howaner.FramePicture.util.Lang;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class FramePictureCommand implements CommandExecutor {
	private final FrameManager manager;
	private static final Map<String, String> arguments = new HashMap<String, String>();
	
	static {
		arguments.put("set",      "handleSet");
		arguments.put("multiset", "handleMultiset");
		arguments.put("get",      "handleGet");
		arguments.put("reload",   "handleReload");
	}
	
	public FramePictureCommand(FrameManager manager) {
		this.manager = manager;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (args.length == 0) {
			sendHelp(sender);
			return true;
		}
		
		String methodName = arguments.get(args[0].toLowerCase());
		if (methodName == null) {
			sendHelp(sender);
			return true;
		}
		
		try {
			Method method = FramePictureCommand.class.getDeclaredMethod(methodName, CommandSender.class, String[].class);
			method.invoke(this, sender, args);
		} catch (Exception e) {
			sender.sendMessage(Lang.PREFIX.getText() + "Error! Parameter with no function!");
			FramePicturePlugin.log.log(Level.WARNING, "Command error!", e);
		}
		return true;
	}
	
	public void handleSet(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(Lang.PREFIX.getText() + Lang.NO_PLAYER.getText());
			return;
		}
		Player player = (Player)sender;
		
		if (!player.hasPermission("FramePicture.set")) {
			player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
			return;
		}
		
		if ((args.length == 1) && Cache.hasCacheCreating(player)) {
			Cache.removeCacheCreating(player);
			player.sendMessage(Lang.PREFIX.getText() + Lang.CREATING_CANCELLED.getText());
			return;
		}
		
		if (args.length < 2) {
			sendHelp(sender);
			return;
		}
		
		if (Config.MONEY_ENABLED) {
			if (!FramePicturePlugin.getEconomy().has(player, Config.CREATE_PRICE)) {
				player.sendMessage(Lang.NOT_ENOUGH_MONEY.getText());
				return;
			}
		}
		
		StringBuilder pathBuilder = new StringBuilder();
		for (int i = 1; i < args.length; i++) {
			if (i != 1) pathBuilder.append(" ");
			pathBuilder.append(args[i]);
		}
		
		Cache.setCacheCreating(player, pathBuilder.toString());
		player.sendMessage(Lang.PREFIX.getText() + Lang.CLICK_FRAME.getText());
	}
	
	public void handleMultiset(CommandSender sender, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(Lang.PREFIX.getText() + Lang.NO_PLAYER.getText());
			return;
		}
		Player player = (Player)sender;
		
		if (!player.hasPermission("FramePicture.multiset")) {
			player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
			return;
		}
		
		if ((args.length == 1) && Cache.hasCacheMultiCreating(player)) {
			Cache.removeCacheMultiCreating(player);
			player.sendMessage(Lang.PREFIX.getText() + Lang.CREATING_CANCELLED.getText());
			return;
		}
		
		if (args.length < 2) {
			sendHelp(sender);
			return;
		}
		
		if (Cache.hasCacheCreating(player) || Cache.hasCacheMultiCreating(player)) {
			player.sendMessage(Lang.PREFIX.getText() + Lang.ALREADY_SELECTION.getText());
			return;
		}
		
		if (Config.MONEY_ENABLED) {
			if (!FramePicturePlugin.getEconomy().has(player, Config.CREATE_PRICE)) {
				player.sendMessage(Lang.NOT_ENOUGH_MONEY.getText());
				return;
			}
		}
		
		StringBuilder pathBuilder = new StringBuilder();
		for (int i = 1; i < args.length; i++) {
			if (i != 1) pathBuilder.append(" ");
			pathBuilder.append(args[i]);
		}
		
		Cache.setCacheMultiCreating(player, pathBuilder.toString());
		player.sendMessage(Lang.PREFIX.getText() + Lang.CLICK_MULTIFRAME.getText());
	}
	
	public void handleGet(CommandSender sender, String[] args) {
		if (args.length != 1) {
			sendHelp(sender);
			return;
		}
		
		if (!(sender instanceof Player)) {
			sender.sendMessage(Lang.PREFIX.getText() + Lang.NO_PLAYER.getText());
			return;
		}
		Player player = (Player)sender;
		
		if (!player.hasPermission("FramePicture.get")) {
			player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
			return;
		}
		
		if (Cache.hasCacheGetting(player)) {
			player.sendMessage(Lang.PREFIX.getText() + Lang.GETTING_MODE_DISABLED.getText());
			Cache.removeCacheGetting(player);
		} else {
			player.sendMessage(Lang.PREFIX.getText() + Lang.GETTING_MODE_ENABLED.getText());
			Cache.addCacheGetting(player);
		}
	}
	
	public void handleReload(CommandSender sender, String[] args) {
		if (args.length != 1) {
			sendHelp(sender);
			return;
		}
		
		if (sender instanceof Player) {
			Player player = (Player)sender;
			if (!player.hasPermission("FramePicture.reload")) {
				player.sendMessage(Lang.PREFIX.getText() + Lang.NO_PERMISSION.getText());
				return;
			}
		}
		
		if (!Config.configFile.exists()) Config.save();
		Config.load();
		Config.save();
		manager.loadFrames();
		manager.saveFrames();
		
		if (Config.MONEY_ENABLED) {
			FramePicturePlugin.getPlugin().setupEconomy();
			if (FramePicturePlugin.getEconomy() == null) {
				FramePicturePlugin.log.info("Vault not found! Money Support disabled!");
				Config.MONEY_ENABLED = false;
				Config.save();
			}
		}
		
		Lang.load();
		FramePicturePlugin.log.info("Plugin reloaded!");
		sender.sendMessage(Lang.PREFIX.getText() + Lang.PLUGIN_RELOAD.getText());
	}
	
	public boolean sendHelp(CommandSender sender) {
		sender.sendMessage(ChatColor.GREEN + "Help from /FramePicture or /fp:");
		sender.sendMessage("/FramePicture set <URL>  " + ChatColor.GOLD + "--" + ChatColor.WHITE + "  Set a Picture in a Frame.");
		sender.sendMessage("/FramePicture multiset <URL>  " + ChatColor.GOLD + "--" + ChatColor.WHITE + "  Create a Picture wall.");
		sender.sendMessage("/FramePicture get  " + ChatColor.GOLD + "--" + ChatColor.WHITE + "  Get the Url from a Picture");
		sender.sendMessage("/FramePicture reload  " + ChatColor.GOLD + "--" + ChatColor.WHITE + "  Reload the Config.");
		return true;
	}

}
