package de.howaner.FramePicture.render;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import org.bukkit.entity.Player;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

public class TextRenderer extends MapRenderer {
	
	private final String text;
	private Short mapId = null;
	private boolean rendered = false;
	
	public TextRenderer(String text) {
		this.text = text;
	}
	
	public TextRenderer(String text, Short mapId) {
		this.text = text;
		this.mapId = mapId;
	}
	
	public String getText() {
		return this.text;
	}
	
	public Short getMapId() {
		return this.mapId;
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
					BufferedImage image = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
					Graphics g = image.getGraphics();
					g.drawString(TextRenderer.this.text, 5, 12);
					if (TextRenderer.this.mapId != null)
						g.drawString("Map #" + TextRenderer.this.mapId.toString(), 70, 115);
					
					canvas.drawImage(0, 0, image);
					player.sendMap(view);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

}
