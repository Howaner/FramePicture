package de.howaner.FramePicture.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import net.minecraft.server.v1_7_R3.Packet;
import org.bukkit.entity.Player;

public class PacketSender implements Runnable {
	public static Map<Player, List<Packet[]>> packetsToSend = new HashMap<Player, List<Packet[]>>();

	@Override
	public void run() {
		List<Player> playersToRemove = new ArrayList<Player>();
		for (Entry<Player, List<Packet[]>> e : packetsToSend.entrySet()) {
			Player player = e.getKey();
			List<Packet[]> packets = e.getValue();
			
			if (packets.isEmpty()) {
				playersToRemove.add(player);
				continue;
			}
			
			Packet[] packetArray = packets.get(0);
			if (packetArray.length != 0) {
				Utils.sendPacketsFast(player, packetArray);
			}
			
			packets.remove(0);
		}
		
		for (Player player : playersToRemove) {
			packetsToSend.remove(player);
		}
	}
	
	public static void addPacketToPlayer(Player player, Packet[] packet) {
		List<Packet[]> packets = packetsToSend.get(player);
		if (packets == null)
			packets = new ArrayList<Packet[]>();
		
		packets.add(packet);
		packetsToSend.put(player, packets);
	}

}
