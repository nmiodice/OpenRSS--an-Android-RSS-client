package com.iodice.ui.feeds;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.iodice.database.FeedData;
import com.iodice.database.FeedOrm;
import com.iodice.rssreader.R;
import com.iodice.utilities.Callback;
import com.iodice.utilities.MultiSpinner;
import com.iodice.utilities.Sys;

public class AddNewFeedDialog {
	
	public static AlertDialog.Builder getAddDialog(Context context, 
													Callback callbackInterface,
													int callbackMethodNumber) {		
		
		View view = getCustomContentView(context);
		AlertDialog.Builder builder = getAlertDialog(context);
		builder = setupCustomView(builder, view);
		
		// the context class must implement the 'callback' interface in order to
		// populate the UI with response data
		builder = setupButtons(builder, view, callbackInterface, callbackMethodNumber);
		
		return builder;
	}
	

	// http://developer.android.com/guide/topics/ui/controls/spinner.html
	private static View setupCategoryContent(View v, Context context) {
		
		MultiSpinner spinner = (MultiSpinner) v.findViewById(R.id.add_feed_categories);
		
		Cursor c = FeedOrm.selectAllCategories(context);
		ArrayList<String> items = new ArrayList<String>();
		c.moveToFirst();
		while(!c.isAfterLast()) {
		     items.add(c.getString(c.getColumnIndex(FeedOrm.getCategoryTableCategoryKey())));
		     c.moveToNext();
		}
		
		spinner.setItems(items, context.getString(R.string.groups));

		return v; 
	}
	
	private static View getCustomContentView(Context context) {
		LayoutInflater inflater = LayoutInflater.from(context);
		View v = inflater.inflate(R.layout.feed_add_new_feed, null);
		v = setupCategoryContent(v, context);
		return v;
	}
	
	private static AlertDialog.Builder getAlertDialog(Context context) {
		AlertDialog.Builder builder = new AlertDialog.Builder(context);
		return builder;
	}
	
	private static AlertDialog.Builder setupCustomView(AlertDialog.Builder builder, 
			View view) {
		builder.setView(view);
		builder.setTitle(builder.getContext().getText(R.string.add_feed_title));
		builder.setMessage(builder.getContext().getText(R.string.add_feed_message));
		return builder;
	}
	
	private static AlertDialog.Builder setupButtons(AlertDialog.Builder builder, 
			final View v, 
			final Callback callbackInterface,
			final int callbackMethodNumber) {
		
		final Context context = builder.getContext();
		
		// upon OK, setup the new feed item and send the data to the defined callback. Also, 
		// shut off the keyboard!
		builder.setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				
				FeedData newFeed = new FeedData();
				EditText txt;
				MultiSpinner spinner;
				
				Sys.disableKeyboard(context);
				
				txt = (EditText) v.findViewById(R.id.add_feed_name);
				newFeed.setName(txt.getText().toString());
				
				txt = (EditText) v.findViewById(R.id.add_feed_url);
				newFeed.setURL(txt.getText().toString());
				
				// check for new category entered via text
				ArrayList<String> categories = new ArrayList<String>();
				txt = (EditText) v.findViewById(R.id.add_feed_new_category);
				String newCategory = txt.getText().toString();
				if (newCategory != null && !newCategory.equals(""))
					categories.add(newCategory);
				
				// loop through spinner items and get selected categories
				spinner = (MultiSpinner) v.findViewById(R.id.add_feed_categories);
				boolean[] selected = spinner.getSelected();
				List<String> items = spinner.getItems();
				int cnt = items.size();
				for (int i = 0; i < cnt; i++) {
					if (selected[i])
						categories.add(items.get(i));
				}
				newFeed.setGroups(categories);
				
				// this needs to be defined by the context class via the callback interface!
				callbackInterface.handleCallbackEvent(callbackMethodNumber,  newFeed);
			}
		});

		builder.setNegativeButton(context.getString(R.string.cancel), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				//Sys.disableKeyboard(context);
			}
		});
		
		return builder;
	}

}
