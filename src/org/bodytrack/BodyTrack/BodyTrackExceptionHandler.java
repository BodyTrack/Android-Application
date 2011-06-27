package org.bodytrack.BodyTrack;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.Thread.UncaughtExceptionHandler;

import android.content.Context;
import android.os.Environment;

public class BodyTrackExceptionHandler implements UncaughtExceptionHandler {

	private UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();
	
	@Override
	public void uncaughtException(Thread thread, Throwable ex) {
		try {
			long timestamp = System.currentTimeMillis();
			FileOutputStream fos = new FileOutputStream(Environment.getExternalStorageDirectory().getAbsolutePath() + "/bodytrack_crash_" + timestamp + ".txt");
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
			out.println("Console:");
			out.println(btStats.getConsoleText());
			out.close();
		} catch (FileNotFoundException e) {
		}
		
		oldHandler.uncaughtException(thread, ex);
	}

}
