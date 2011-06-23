package org.bodytrack.BodyTrack.Activities;

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
			out.close();
		} catch (FileNotFoundException e) {
		}
		
		oldHandler.uncaughtException(thread, ex);
	}

}
