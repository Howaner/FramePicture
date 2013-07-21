package de.howaner.FramePicture.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import de.howaner.FramePicture.util.Frame;

public class RemoveFrameEvent extends Event implements Cancellable {
	private Frame frame;
	private boolean cancelled = false;
	
	public RemoveFrameEvent(Frame frame) {
		this.frame = frame;
	}
	
	public Frame getFrame() {
		return this.frame;
	}
	
	@Override
	public boolean isCancelled() {
		return this.cancelled;
	}
	
	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
	
	@Override
	public HandlerList getHandlers() {
		return new HandlerList();
	}

}
