package de.howaner.FramePicture.event;

import de.howaner.FramePicture.util.Frame;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class ChangeFrameIdEvent extends Event {
	private Frame frame;
	private short oldId;
	private short newId;
	private static HandlerList handlerList = new HandlerList();
	
	public ChangeFrameIdEvent(Frame frame, short oldId, short newId) {
		this.frame = frame;
		this.oldId = oldId;
		this.newId = newId;
	}
	
	public Frame getFrame() {
		return this.frame;
	}
	
	public short getOldId() {
		return this.oldId;
	}
	
	public short getNewId() {
		return this.newId;
	}
	
	public void setNewId(short newId) {
		this.newId = newId;
	}
	
	public HandlerList getHandlers() {
		return handlerList;
	}
	
	public static HandlerList getHandlerList() {
		return handlerList;
	}
	
}
