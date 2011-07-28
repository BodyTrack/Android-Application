package org.bodytrack.BodyTrack;

import java.util.LinkedList;
import java.util.List;

import android.net.wifi.ScanResult;

public class WifiWatcher {
	private List<Object[]> apList = new LinkedList<Object[]>();
	private long oldestAP = Long.MAX_VALUE;
	
	private BTStatisticTracker btStats = BTStatisticTracker.getInstance();
	
	public void addScanResults(List<ScanResult> results){
		boolean[] inScan = new boolean[apList.size()];
		long scanTime = System.currentTimeMillis();
		for (int i = 0; i < inScan.length; i++)
			inScan[i] = false;
		for (ScanResult result : results){
			boolean found = false;
			int index = 0;
			for (Object[] ap : apList){
				if (((String) ap[1]).equals(result.SSID) && ((String) ap[2]).equals(result.BSSID)){
					inScan[index] = true;
					found = true;
					break;
				}
				index++;
			}
			if (!found){
				apList.add(new Object[]{scanTime,result.SSID, result.BSSID});
			}
		}
		int numRemoved = 0;
		for (int i = 0; i < inScan.length; i++){
			if (!inScan[i]){
				apList.remove(i - numRemoved++);
			}
		}		
		oldestAP = Long.MAX_VALUE;
		for (Object[] ap : apList){
			if (((Long) ap[0]) < oldestAP){
				oldestAP = (Long) ap[0];
				break;
			}
		}
		return;
	}
	
	public void clear(){
		apList = new LinkedList<Object[]>();
		oldestAP = Long.MAX_VALUE;
	}
	
	public boolean isIdlingNearAP(){
		return System.currentTimeMillis() - oldestAP > 1000 * 60 * 5;
	}
}
