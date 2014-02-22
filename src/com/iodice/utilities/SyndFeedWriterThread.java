package com.iodice.utilities;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.Writer;

import com.google.code.rome.android.repackaged.com.sun.syndication.feed.synd.SyndFeed;
import com.google.code.rome.android.repackaged.com.sun.syndication.io.SyndFeedOutput;

// writes a syndfeed to disk as a background thread
public class SyndFeedWriterThread implements Runnable {

	private String dir;
	private String name;
	private SyndFeed feed;
	
	public SyndFeedWriterThread(String dir, String name, SyndFeed feed) {
		this.dir = dir;
		this.name = Text.makeFilesystemSafe(name);
		this.feed = feed;
	}

	@Override
	public void run() {
		FileOutputStream fop = null;
		byte[] contentsInBytes;
		
		try{
			// 1. create file on disk
			File file = new File(this.dir, this.name);
			
			// 2. set up to write xml to disk
			Writer writer = new FileWriter(this.dir + this.name);
	        SyndFeedOutput output = new SyndFeedOutput();
			
	        
	        fop = new FileOutputStream(file);
			// if file doesnt exists, then create it
			if (!file.exists()) {
				file.createNewFile();
			}
			contentsInBytes = output.outputString(this.feed).getBytes();
			fop.write(contentsInBytes);
			fop.flush();
			fop.close();
	        // 3. write to disk
	        output.output(this.feed, writer);
			System.out.println("Writing " + this.name + " to directory " + this.dir + ". File size = " + (float)file.length()/1024/1024 + "MB");
	        writer.close();
		} catch(Exception e){
			System.out.println(e.getMessage());
			e.printStackTrace();
		}		
	}

}
