package de.howaner.FramePicture.render;

import java.awt.Image;

import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class ImageRenderer extends MapRenderer {
	
	private final Image image;
	private boolean rendered = false;
	public int imageX = 0;
	public int imageY = 0;
	
	public ImageRenderer(Image image) {
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
	public void render(final MapView view, final MapCanvas canvas, final Player player) {
		if (this.rendered) return;
		this.removeCursors(canvas);
		this.rendered = true;
		new Thread() {
			@Override
			public void run() {
				try {
					canvas.drawImage(ImageRenderer.this.imageX, ImageRenderer.this.imageY, ImageRenderer.this.image);
					player.sendMap(view);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

}
