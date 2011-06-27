package org.bodytrack.BodyTrack;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class BTStatisticTracker {
	
	public static int MAX_LOG_SIZE = 5242880; //approx 5 MB
	
	private static BTStatisticTracker instance = null;
	
	public static BTStatisticTracker getInstance(){
		if (instance == null)
			instance = new BTStatisticTracker();
		return instance;
	}
	
	private long timeSpentConvertingToJSON = 0;
	private long timeSpentPushingIntoDB = 0;
	private long timeSpentGatheringAndJoining = 0;
	private long timeSpentUploadingData = 0;
	private long timeSpentDeletingDBData = 0;
	private long start = 0;
	private StringBuffer consoleText = new StringBuffer();
	
	public PrintStream out = new PrintStream(new OutputStream(){
		
		private int size = 0;

		@Override
		public void write(int oneByte) throws IOException {
			consoleText.append((char) oneByte);
			size++;
			if (size > MAX_LOG_SIZE){
				int index = consoleText.indexOf("\n") + 1;
				consoleText.delete(0, index);
				size -= index;
			}
		}
		
	});
	
	private BTStatisticTracker(){
		start = System.currentTimeMillis();
	}
	
	public void addTimeSpentConvertingToJSON(long time){
		timeSpentConvertingToJSON += time;
	}
	
	public void addTimeSpentPushingIntoDB(long time){
		timeSpentPushingIntoDB += time;
	}
	
	public void addTimeSpentGatheringAndJoining(long time){
		timeSpentGatheringAndJoining += time;
	}
	
	public void addTimeSpentUploadingData(long time){
		timeSpentUploadingData += time;
	}
	
	public void addTimeSpentDeletingData(long time){
		timeSpentDeletingDBData += time;
	}
	
	public void logBytesUploaded(int bytes, int statusCode){
		String prefix = "";
		
		if (bytes >= 1048576){
			bytes /= 1048576;
			prefix = "M";
		}
		else if (bytes >= 1024){
			bytes /= 1024;
			prefix = "K";
		}
		
		out.println("Uploaded " + bytes + " " + prefix + "B with response: " + statusCode);
	}
	
	public long getTimeSpentConvertingToJSON(){
		return timeSpentConvertingToJSON;
	}
	
	public long getTimeSpentPushingIntoDB(){
		return timeSpentPushingIntoDB;
	}
	
	public long getTimeSpentGatheringAndJoining(){
		return timeSpentGatheringAndJoining;
	}
	
	public long getTimeSpentUploadingData(){
		return timeSpentUploadingData;
	}
	
	public long getTimeSpentDeletingData(){
		return timeSpentDeletingDBData;
	}
	
	public long getTotalUptime(){
		return System.currentTimeMillis() - start;
	}
	
	
	public String getConsoleText(){
		return consoleText.toString();
	}
}
