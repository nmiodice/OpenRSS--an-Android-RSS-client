package com.iodice.ui.rssgroups;

import java.util.ArrayList;
import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

import com.iodice.database.FeedOrm;
import com.iodice.rssreader.R;
import com.iodice.utilities.MultiSpinner;
import com.iodice.utilities.Sys;
import com.iodice.utilities.Text;

public class AddNewGroupingDialog {
	
	public static AlertDialog.Builder getAddDialog(List<String> selectedUrls, Context context) {		

		View view = getCustomContentView(context);
		AlertDialog.Builder builder = getAlertDialog(context);
		builder = setupCustomView(builder, view);
		builder = setupButtons(builder, view, selectedUrls);
		
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
		View v = inflater.inflate(R.layout.feed_add_new_grouping, null);
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
		builder.setTitle(builder.getContext().getText(R.string.add_feed_grouping_title));
		builder.setMessage(builder.getContext().getText(R.string.add_feed_grouping_message));
		return builder;
	}
	
	private static AlertDialog.Builder setupButtons(AlertDialog.Builder builder, 
			final View v, 
			final List<String> selectedUrls) {
		
		final Context context = builder.getContext();
		
		// upon OK, add selected feeds to the categories selected
		builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
				EditText editText;
				MultiSpinner spinner;
				ArrayList<String> categories = new ArrayList<String>();
				Sys.disableKeyboard(context);
				
				// step 1: find any user input categories
				editText = (EditText) v.findViewById(R.id.category_csv_list);
				String userInput = editText.getText().toString();
				if (userInput != null && !userInput.equals("")) {
					List<String> userInputList = Text.getCleanStringList(userInput);
					categories.addAll(userInputList);
				}
				
				// step 2: loop through spinner items and get selected categories
				spinner = (MultiSpinner) v.findViewById(R.id.add_feed_categories);
				boolean[] selected = spinner.getSelected();
				List<String> items = spinner.getItems();
				int cnt = items.size();
				for (int i = 0; i < cnt; i++) {
					if (selected[i])
						categories.add(items.get(i));
				}				
		        List<String> selectedUrlList = selectedUrls;
		    	FeedOrm.saveExistingFeedsInGroup(selectedUrlList, categories, context);
			}
		});

		builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});
		
		return builder;
	}

}
