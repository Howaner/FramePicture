package de.howaner.FramePicture.util;

import de.howaner.FramePicture.FramePicturePlugin;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.Vector;
import javax.imageio.ImageIO;

public class PictureDatabase {
	private final Vector<String> whileDownload = new Vector<String>();
	private final Vector<Thread> threads = new Vector<Thread>();
	private File outputFolder = new File("plugins/FramePicture/images/");
	
	public PictureDatabase() {
		if (!outputFolder.exists()) outputFolder.mkdirs();
	}
	
	public File getOutputFolder() {
		return this.outputFolder;
	}
	
	public BufferedImage loadImage(String name) {
		if (name == null || name.isEmpty()) return null;
		if (!outputFolder.exists()) outputFolder.mkdirs();
		File file = new File(outputFolder, name);
		if (!file.exists() || file.isDirectory()) return null;
		
		try {
			return ImageIO.read(file);
		} catch (Exception ex) {
			return null;
		}
	}
	
	public File writeImage(BufferedImage img, String name) {
		if (img == null || name == null || name.isEmpty()) return null;
		if (!outputFolder.exists()) outputFolder.mkdirs();
		
		File file = new File(outputFolder, String.format("%s.png", name));
		int i = 1;
		while (file.exists()) {
			file = new File(outputFolder, String.format("%s_%s.png", name, String.valueOf(i)));
			i++;
		}
		
		try {
			ImageIO.write(img, "png", file);
			return file;
		} catch (Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}
	
	public boolean deleteImage(String name) {
		File file = new File(outputFolder, name);
		if (!file.exists()) return false;
		
		try {
			file.delete();
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public void downloadImage(final String path, final FinishDownloadSignal signal) {
		if (whileDownload.contains(path.toLowerCase())) return;
		whileDownload.add(path);
		
		FramePicturePlugin.log.info("Download " + path + " ...");
		Thread thread = new Thread() {
			@Override
			public void run() {
				try {
					//Generate Streams
					URL url = new URL(path);
					BufferedInputStream in = new BufferedInputStream(url.openStream());
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					
					//Get File Informations
					String fileName;
					String fileEnding;
					
					if (path.contains("/")) {
						String[] split = path.split("/");
						fileName = split[split.length-1];
					} else {
						fileName = "unknown.png";
					}
					if (fileName.contains(".")) {
						String[] split = fileName.split("\\.");
						
						fileName = "";
						for (int i = 0; i < split.length-1; i++) {
							if (i != 0) fileName += '.';
							fileName += split[i];
						}
						
						fileEnding = split[split.length-1];
					} else {
						fileEnding = "png";
					}
					
					//Download Picture
					byte[] buf = new byte[1024];
					int count;
					while ((count = in.read(buf, 0, 1024)) != -1) {
						out.write(buf, 0, count);
					}
					out.flush();
					out.close();
					in.close();
					
					//Get Output File
					if (!outputFolder.exists()) outputFolder.mkdirs();
					File outputFile = new File(outputFolder, String.format("%s.%s", fileName, fileEnding));
					int i = 1;
					while (outputFile.exists()) {
						outputFile = new File(outputFolder, String.format("%s_%s.%s", fileName, String.valueOf(i), fileEnding));
						i++;
					}
					
					//Save Picture into File
					FileOutputStream output = new FileOutputStream(outputFile);
					output.write(out.toByteArray());
					output.flush();
					output.close();
					
					//Call Signal
					FramePicturePlugin.log.info("Image " + outputFile + " was downloaded!");
					synchronized(signal) {
						signal.downloadSuccess(outputFile);
					}
				} catch (Exception ex) {
					FramePicturePlugin.log.warning("Cant download Image! Error: " + ex.getMessage());
					synchronized(signal) {
						signal.downloadError(ex);
					}
					ex.printStackTrace();
				}
				synchronized(whileDownload) {
					whileDownload.remove(path);
				}
				synchronized(threads) {
					threads.remove(this);
				}
			}
		};
		threads.add(thread);
		thread.start();
	}
	
	public void clear() {
		for (Thread thread : this.threads)
			thread.interrupt();
		
		this.threads.clear();
		this.whileDownload.clear();
	}
	
	public interface FinishDownloadSignal {
		
		public void downloadSuccess(File file);
		
		public void downloadError(Exception e);
	}
	
}
