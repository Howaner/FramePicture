package de.howaner.FramePicture.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.entity.Player;

public class Cache {
	private static Map<Player, String> createCache = new HashMap<Player, String>();
	private static List<Player> removeCache = new ArrayList<Player>();
	
	/* Cache Creating */
	public static boolean hasCacheCreating(Player player) {
		return createCache.containsKey(player);
	}
	
	public static String getCacheCreating(Player player) {
		return createCache.get(player);
	}
	
	public static void setCacheCreating(Player player, String path) {
		createCache.put(player, path);
	}
	
	public static void removeCacheCreating(Player player) {
		createCache.remove(player);
	}
	
	
	/* Cache Getting */
	public static boolean hasCacheGetting(Player player) {
		return removeCache.contains(player);
	}
	
	public static void addCacheGetting(Player player) {
		removeCache.add(player);
	}
	
	public static void removeCacheGetting(Player player) {
		removeCache.remove(player);
	}

}
