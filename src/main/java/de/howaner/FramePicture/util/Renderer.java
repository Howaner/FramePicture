package de.howaner.FramePicture.util;

import java.awt.Image;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class Renderer extends MapRenderer {
	
	private final Image image;
	private boolean rendered = false;
	
	public Renderer(Image image) {
		this.image = image;
	}
	
	public Image getImage() {
		return this.image;
	}
	
	public boolean isRendered() {
		return this.rendered;
	}
	
	private void removeCursors(MapCanvas canvas) {
		for (int i=0; i<canvas.getCursors().size(); i++) {
			canvas.getCursors().removeCursor(canvas.getCursors().getCursor(i));
		}
	}
	
	@Override
	public void render(MapView view, final MapCanvas canvas, Player player) {
		if (this.rendered) return;
		this.removeCursors(canvas);
		this.rendered = true;
		new Thread() {
			@Override
			public void run() {
				try {
					canvas.drawImage(0, 0, image);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

}
