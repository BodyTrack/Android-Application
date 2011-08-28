package org.bodytrack.BodyTrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Date;

import android.os.Environment;

public class BTStatisticTracker {
	private static final String folder = "/BodyTrack/Logs";
	
	public static int MAX_LOG_SIZE = 1024 * 10; //10 KB in ram at any given time
												 //too much makes it laggy and don't feel like making obtaining it async
	
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
	private long totalDataStorageBytes = 0;
	private long totalDataUploadBytes = 0;
	private long totalOverheadBytes = 0;
	private long dbWrites = 0;
	private long start = 0;
	
	private long uploadRateTime = 0;
	private long uploadRateBytes = 0;
	private long storeRateTime = 0;
	private long storeRateBytes = 0;
	
	private StringBuffer consoleText = new StringBuffer();
	
	private PrintStream fOut = new LogPrintStream();
	
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
		
	}){
		private boolean needStamp = true;
		
		private String getTimeStamp(){
			Date curDate = new Date();
			return "[" + tFormat.format(curDate.getHours()) + ":" + tFormat.format(curDate.getMinutes()) + ":" + tFormat.format(curDate.getSeconds()) + "]";
		}
		
		public void print(String text){
			if (needStamp){
				text = getTimeStamp() + text;
				needStamp = false;
			}
			fOut.print(text);
			super.print(text);
		}
		
		public void println(String text){
			print(text);
			println();
		}
		
		public void println(){
			super.println();
			needStamp = true;
		}
	};
	
	private BTStatisticTracker(){
		start = System.currentTimeMillis();
		out.println("BodyTrack Application Started");
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
	
	public void addOverheadBytes(long bytes){
		totalOverheadBytes += bytes;
	}
	
	public void addDataUploadBytes(long bytes){
		totalDataUploadBytes += bytes;
	}
	
	public void logBytesUploaded(long data, long overhead, int statusCode){
		addDataUploadBytes(data);
		addOverheadBytes(overhead);
		/*long bytes = data + overhead;
		String prefix = "";
		
		if (bytes >= 1048576){
			bytes /= 1048576;
			prefix = "M";
		}
		else if (bytes >= 1024){
			bytes /= 1024;
			prefix = "K";
		}
		
		out.println("Uploaded " + bytes + " " + prefix + "B with response: " + statusCode);*/
	}
	
	public void addDataStorageBytes(long bytes){
		totalDataStorageBytes += bytes;
	}

	public void logBytesStored(long data) {
		addDataStorageBytes(data);
	}

	public void addDbWrite(long data){
		addDataStorageBytes(data);
		dbWrites++;
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
	
	public long getTotalDataStorageBytes(){
		return totalDataStorageBytes;
	}
	
	public long getTotalDataUploadBytes(){
		return totalDataUploadBytes;
	}
	
	public long getTotalOverheadBytes(){
		return totalOverheadBytes;
	}
	
	public long getDbWrites(){
		return dbWrites;
	}
	
	public String getConsoleText(){
		return consoleText.toString();
	}
	
	public float getUploadRate() {
		long curTime = System.currentTimeMillis();
		float rate = (totalDataUploadBytes - uploadRateBytes) / (float)(curTime - uploadRateTime);

		uploadRateBytes = totalDataUploadBytes;
		uploadRateTime = curTime;
		
		return rate;	// bytes per millisecond
	}
	
	public float getStoreRate() {
		long curTime = System.currentTimeMillis();
		float rate = (totalDataStorageBytes - storeRateBytes) / (float)(curTime - storeRateTime);
		
		storeRateBytes = totalDataStorageBytes;
		storeRateTime = curTime;

		return rate;	// bytes per millisecond
	}
	
	private class LogPrintStream extends PrintStream{
		
		private Date lastDate;

		private LogPrintStream(OutputStream out) {
			super(out);
		}
		
		public LogPrintStream(){
			this(getLogOutputStream());
			lastDate = new Date();
		}
		
		private boolean checkForNeedNewFile(){
			Date curDate = new Date();
			if (checkError() || curDate.getDay() != lastDate.getDay() || curDate.getMonth() != lastDate.getMonth() ||
					curDate.getYear() != lastDate.getYear()){
				close();
				fOut = new LogPrintStream();
				return true;
			}
			else{
				return false;
			}
		}
		
		public void print(String text){
			if (checkForNeedNewFile())
				fOut.print(text);
			else
				super.print(text);
		}
	}
	
	private static DecimalFormat tFormat = new DecimalFormat("00");
	
	private static OutputStream getLogOutputStream(){
		File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + folder);
		f.mkdirs();
		Date curDate = new Date();
		f = new File(f.getAbsolutePath() + "/" + "log_" + (curDate.getYear() + 1900) + tFormat.format(curDate.getMonth() + 1) + tFormat.format(curDate.getDate()) + ".txt");
		try {
			return new FileOutputStream(f, true);
		} catch (FileNotFoundException e) {
			return new OutputStream(){

				@Override
				public void write(int oneByte) throws IOException {
					
				}
				
			};
		}
	}
}
