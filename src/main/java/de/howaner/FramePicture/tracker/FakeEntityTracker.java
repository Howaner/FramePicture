package de.howaner.FramePicture.tracker;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Set;
import net.minecraft.server.v1_7_R1.CrashReport;
import net.minecraft.server.v1_7_R1.CrashReportSystemDetails;
import net.minecraft.server.v1_7_R1.Entity;
import net.minecraft.server.v1_7_R1.EntityItemFrame;
import net.minecraft.server.v1_7_R1.EntityTracker;
import net.minecraft.server.v1_7_R1.EntityTrackerEntry;
import net.minecraft.server.v1_7_R1.ReportedException;
import net.minecraft.server.v1_7_R1.WorldServer;
import org.apache.logging.log4j.Logger;

public class FakeEntityTracker extends EntityTracker {

	public FakeEntityTracker(WorldServer worldserver) {
		super(worldserver);
	}
	
	@Override
	public void addEntity(Entity entity, int i, int j, boolean flag) {
		if (entity instanceof EntityItemFrame) {
			try {
				i = Math.min(i, this.getPrivateValue("e", int.class));
				
				if (this.trackedEntities.b(entity.getId())) {
					throw new IllegalStateException("Entity is already tracked!");
				}
				EntityTrackerEntry fakeEntry = new FakeEntityTrackerEntry(entity, i, j, flag);
				
				Set list = (Set) this.getPrivateValue("c", Set.class);
				list.add(fakeEntry);
				this.setPrivateValue("c", list);
				
				this.trackedEntities.a(entity.getId(), fakeEntry);
				fakeEntry.scanPlayers(this.getPrivateValue("world", WorldServer.class).players);
			} catch (Throwable throwable) {
				CrashReport crashreport = CrashReport.a(throwable, "Adding entity to track");
				CrashReportSystemDetails crashreportsystemdetails = crashreport.a("Entity To Track");
				
				crashreportsystemdetails.a("Tracking range", i + " blocks");
				crashreportsystemdetails.a("Update interval", this.createCrashReportEntityTrackerUpdateInterval(this, j));
				entity.a(crashreportsystemdetails);
				CrashReportSystemDetails crashreportsystemdetails1 = crashreport.a("Entity That Is Already Tracked");
				
				((EntityTrackerEntry)this.trackedEntities.get(entity.getId())).tracker.a(crashreportsystemdetails1);
				try {
					throw new ReportedException(crashreport);
				} catch (ReportedException reportedexception) {
					this.getPrivateValue("a", Logger.class).error("\"Silently\" catching entity tracking error.", reportedexception);
				}
			}
		} else {
			super.addEntity(entity, i, j, flag);
		}
	}
	
	private Object createCrashReportEntityTrackerUpdateInterval(EntityTracker tracker, int id) {
		try {
			Class clazz = Class.forName("net.minecraft.server.v1_7_R1.CrashReportEntityTrackerUpdateInterval");
			Constructor con = clazz.getDeclaredConstructor(EntityTracker.class, int.class);
			con.setAccessible(true);
			
			Object newClass = con.newInstance(tracker, id);
			return newClass;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public <T> T getPrivateValue(String name, Class<T> type) {
		try {
			Field field = EntityTracker.class.getDeclaredField(name);
			field.setAccessible(true);
			return (T) field.get(this);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public void setPrivateValue(String name, Object value) throws Exception {
		Field field = EntityTracker.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(this, value);
	}
	
}
