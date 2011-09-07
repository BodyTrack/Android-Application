package org.bodytrack.BodyTrack;

/*This AIDL file defines the RPC interface for the GPS service.
*/

interface IBTSvcRPC {
	void startLogging(int id);
	void stopLogging(int id);
	boolean isLogging(int id);
	boolean canLog(int id);
	boolean canSplitAcc();
	void setGPSDelay(int index);
	int getGPSDelayIndex();
	void setAccSplitting(boolean enabled);
	boolean isSplittingAcc();
	void setSensorDelay(int index);
	int getSensorDelayIndex();
}