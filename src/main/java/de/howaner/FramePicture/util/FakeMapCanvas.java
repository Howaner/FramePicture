package de.howaner.FramePicture.util;

import java.lang.reflect.Field;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_7_R4.map.CraftMapCanvas;
import org.bukkit.craftbukkit.v1_7_R4.map.CraftMapView;

public class FakeMapCanvas extends CraftMapCanvas {
	
	public FakeMapCanvas() {
		super(null);
	}	
	
	@Override
	public byte[] getBuffer() {
		return super.getBuffer();
	}
	
	@Override
	public void setBase(byte[] base) {
		super.setBase(base);
	}
	
	@Override
	public CraftMapView getMapView() {
		return (CraftMapView) ((Bukkit.getMap((short)0) == null) ? Bukkit.createMap(Bukkit.getWorlds().get((short)0)) : Bukkit.getMap((short)0));
	}
	
	@Override
	public void setPixel(int x, int y, byte color) {
		Field field;
		byte[] buffer;
		try {
			field = CraftMapCanvas.class.getDeclaredField("buffer");
			field.setAccessible(true);
			buffer = (byte[]) field.get(this);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		
		if ((x < 0) || (y < 0) || (x >= 128) || (y >= 128)) return;
		if (buffer[(y * 128 + x)] != color) {
			buffer[(y * 128 + x)] = color;
			//this.mapView.worldMap.flagDirty(x, y, y); -->> Not required
		}
		
		try {
			field.set(this, buffer);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
