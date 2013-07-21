package de.howaner.FramePicture.util;

import java.awt.Image;

import org.bukkit.Bukkit;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;

import de.howaner.FramePicture.FramePicturePlugin;

public class Frame {
	
	private String path;
	private Image cacheImage = null;
	private final Short mapId;
	
	public Frame(String path, final Short mapId) {
		this.path = path;
		this.mapId = mapId;
	}
	
	public Short getMapId() {
		return this.mapId;
	}
	
	public String getPicturePath() {
		return this.path;
	}
	
	public void setPicturePath(String path) {
		this.path = path;
		this.cacheImage = null;
		this.update();
	}
	
	public Image getPicture() {
		if (this.cacheImage == null)
		{
			try {
				this.cacheImage = Utils.getPicture(this.getPicturePath());
				if (Config.CHANGE_SIZE_ENABLED) cacheImage = cacheImage.getScaledInstance(Config.SIZE_WIDTH, Config.SIZE_HEIGHT, Image.SCALE_DEFAULT);
			} catch (Exception e) { }
		}
		return this.cacheImage;
	}
	
	public void update() {
		MapView view = Bukkit.getMap(this.mapId);
		if (view == null) {
			view = Utils.generateMap(this.mapId);
			if (view == null) {
				FramePicturePlugin.log.warning("Frame " + this.mapId + " has an error!");
			}
		}
		Image image = this.getPicture();
		if (image == null) {
			FramePicturePlugin.log.warning("The Url \"" + this.getPicturePath() + "\" does not exists!");
			return;
		}
		//Renderer
		MapRenderer renderer = new Renderer(0, 0, image);
		for (MapRenderer render : view.getRenderers())
		{
			view.removeRenderer(render);
		}
		view.addRenderer(renderer);
	}

}
