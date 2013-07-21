package de.howaner.FramePicture.util;

import java.awt.Image;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class Renderer extends MapRenderer {
	
	private final int middleX;
	private final int middleZ;
	private final Image image;
	private boolean rendered = false;
	
	public Renderer(int middleX, int middleZ, Image image) {
		this.middleX = middleX;
		this.middleZ = middleZ;
		this.image = image;
	}
	
	public int getMiddleX() {
		return this.middleX;
	}
	
	public int getMiddlZ() {
		return this.middleZ;
	}
	
	public Image getImage() {
		return this.image;
	}
	
	public boolean isRendered() {
		return this.rendered;
	}
	
	@Override
	public void render(MapView view, MapCanvas canvas, Player player) {
		if (this.rendered) return;
		canvas.drawImage(middleX, middleZ, image);
		this.rendered = true;
	}

}
