package de.howaner.FramePicture.util;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.server.v1_8_R1.Packet;
import org.bukkit.entity.Player;

public class PacketSender implements Runnable {
	private static final Queue<QueuedPacket> queue = new ConcurrentLinkedQueue<QueuedPacket>();

	@Override
	public void run() {
		int loads = 0;
		while (!queue.isEmpty() && (loads++ <= Config.FRAME_LOADS_PER_TICK)) {
			QueuedPacket packet = queue.poll();
			if (!packet.player.isOnline()) return;

			Utils.sendPacketFast(packet.player, packet.packet);
		}
	}

	public static void removePlayerFromQueue(Player player) {
		synchronized (queue) {
			Iterator<QueuedPacket> itr = queue.iterator();
			while (itr.hasNext()) {
				if (itr.next().player == player) {
					itr.remove();
				}
			}
		}
	}
	
	public static void addPacketToQueue(Player player, Packet packet) {
		queue.add(new QueuedPacket(player, packet));
	}

	private static class QueuedPacket {
		public Player player;
		public Packet packet;

		public QueuedPacket(Player player, Packet packet) {
			this.player = player;
			this.packet = packet;
		}
	}

}
