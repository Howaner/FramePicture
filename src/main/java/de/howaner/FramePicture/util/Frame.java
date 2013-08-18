package de.howaner.FramePicture.util;

import java.awt.Image;

import org.bukkit.Bukkit;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import de.howaner.FramePicture.FramePicturePlugin;

public class Frame {
	
	private String path;
	private final Short mapId;
	
	public Frame(String path, final Short mapId) {
		this.path = path;
		this.mapId = mapId;
	}
	
	public Short getMapId() {
		return this.mapId;
	}
	
	public String getPath() {
		return this.path;
	}
	
	public void setPath(String path) {
		this.path = path;
		this.update();
	}
	
	public Image getPicture() {
		try {
			Image image = Utils.getPicture(this.getPath());
			if (image == null) return null;
			if (Config.CHANGE_SIZE_ENABLED)
					image = image.getScaledInstance(Config.SIZE_WIDTH, Config.SIZE_HEIGHT, Image.SCALE_DEFAULT);
			return image;
		} catch (Exception e) {
			return null;
		}
	}
	
	public void update() {
		MapView view = Bukkit.getMap(this.mapId);
		if (view == null) {
			view = Utils.generateMap(this.mapId);
			if (view == null) {
				FramePicturePlugin.log.warning("Map-ID " + this.mapId + " has an Error!");
			}
		}
		
		//Renderer
		for (MapRenderer render : view.getRenderers())
		{
			view.removeRenderer(render);
		}
		
		Image image = this.getPicture();
		if (image == null) {
			FramePicturePlugin.log.warning("The Url \"" + this.getPath() + "\" does not exists!");
			return;
		}
		
		MapRenderer renderer = new Renderer(image);
		view.addRenderer(renderer);
		
		FramePicturePlugin.getManager().sendMap(this);
	}

}
