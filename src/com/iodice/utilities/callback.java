package com.iodice.utilities;

public interface callback {
	// A callback event to be called with an integer 'n', which can be used however the caller
	// 	would like. This provides the user the ability to implement this as a switch statement
	//	that calls out to many different methods. The 'obj' parameter can contain user-defined
	// 	data
	public void respondToEvent(int n, Object obj);
}
