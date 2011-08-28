package org.bodytrack.BodyTrack;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;

import android.os.Environment;

public class BodyTrackExceptionHandler implements UncaughtExceptionHandler {

	private UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
	
	private static final String folder = "/BodyTrack/Crashes";
	
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		try {
			long timestamp = System.currentTimeMillis();
			File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + folder);
			f.mkdirs();
			f = new File(f.getAbsolutePath() + "/log_" + timestamp + ".txt");
			FileOutputStream fos = new FileOutputStream(f);
			PrintStream out = new PrintStream(fos);
			out.println("Thread source: " + thread);
			out.println("Timestamp: " + timestamp);
			out.println("Stack trace:");
			ex.printStackTrace(out);
			BTStatisticTracker btStats = BTStatisticTracker.getInstance();
			out.println("Time spent converting data to JSON: " + (btStats.getTimeSpentConvertingToJSON() / 1000.0));
			out.println("Time spent writing to DB: " + (btStats.getTimeSpentPushingIntoDB() / 1000.0));
			out.println("Time spent fetching and joining data: " + (btStats.getTimeSpentGatheringAndJoining() / 1000.0));
			out.println("Time spent uploading data: " + (btStats.getTimeSpentUploadingData() / 1000.0));
			out.println("Time spent deleting data: " + (btStats.getTimeSpentDeletingData() / 1000.0));
			out.println("Total uptime: " + (btStats.getTotalUptime() / 1000.0));
			out.println("Database Writes: " + btStats.getDbWrites());
			out.println("Data uploaded: " + btStats.getTotalDataUploadBytes());
			out.println("Overhead uploaded: " + btStats.getTotalOverheadBytes());
			out.println("Recent Console Output:");
			out.println(btStats.getConsoleText());
			out.close();
		} catch (FileNotFoundException e) {
		}
		
		oldHandler.uncaughtException(thread, ex);
	}

}
