package com.iodice.utilities;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.iodice.rssreader.R;

public class ConfirmationDialog {
	/**
	 * A dialog w/ custom message
	 * @param context
	 * @param callbackInterface
	 * @param callbackFunctionIdentifer
	 * @param callbackData
	 * @param message
	 * @return
	 */
	public static AlertDialog getCustomDialog(Context context, 
			final Callback callbackInterface, 
			final int callbackFunctionIdentifer,
			final Object callbackData,
			final String message) {
		
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
		alertDialogBuilder
		.setTitle(message)
		.setCancelable(false)
		.setPositiveButton(context.getString(R.string.yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int id) {
				callbackInterface.handleCallbackEvent(callbackFunctionIdentifer, callbackData);
			}
		  })
		.setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int id) {
				// if this button is clicked, just close the dialog box and do nothing
				dialog.cancel();
			}
		});
    	AlertDialog alertDialog = alertDialogBuilder.create();
		return alertDialog;
	}
	
	/**
	 * A simple delete dialog
	 * @param context
	 * @param callbackInterface
	 * @param callbackFunctionIdentifer
	 * @param callbackData
	 * @return
	 */
	public static AlertDialog getDeleteDialog(Context context, 
			final Callback callbackInterface, 
			final int callbackFunctionIdentifer,
			final Object callbackData) {		
		String delete = context.getText(R.string.delete_title).toString();
		return ConfirmationDialog.getCustomDialog(context, 
				callbackInterface, 
				callbackFunctionIdentifer, 
				callbackData, 
				delete);
	}
}