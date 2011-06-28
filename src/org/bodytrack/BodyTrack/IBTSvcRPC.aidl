package org.bodytrack.BodyTrack;

/*This AIDL file defines the RPc interface for the GPS service.
*/

interface IBTSvcRPC {
	void startLogging(int id);
	void stopLogging(int id);
	boolean isLogging(int id);
	boolean canLog(int id);
	void setGPSDelay(int index);
	int getGPSDelayIndex();
}