package com.iodice.utilities;

public interface Callback {
	// A callback event to be called with an integer 'n', which can be used however the caller
	// 	would like. This provides the user the ability to implement this as a switch statement
	//	that calls out to many different methods. The 'obj' parameter can contain user-defined
	// 	data
	public void handleCallbackEvent(int n, Object obj) throws UnsupportedOperationException;
}
