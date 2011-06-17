package org.bodytrack.BodyTrack;

/*This AIDL file defines the RPc interface for the Accelerometer service.
*/

interface IAccSvcRPC {
	void startLogging();
	void stopLogging();
	boolean isLogging();
}