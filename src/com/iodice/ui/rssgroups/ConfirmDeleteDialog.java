package com.iodice.ui.rssgroups;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.iodice.rssreader.R;
import com.iodice.utilities.Callback;

public class ConfirmDeleteDialog {
	public static AlertDialog getDeleteDialog(Context context, 
			final Callback callbackInterface, 
			final int callbackFunctionIdentifer,
			final List<String> selectedUrlList) {		

		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
		alertDialogBuilder
		.setTitle(context.getText(R.string.delete_title))
		.setCancelable(false)
		.setPositiveButton(context.getString(R.string.yes), new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog,int id) {
				callbackInterface.handleCallbackEvent(callbackFunctionIdentifer, selectedUrlList);
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
}