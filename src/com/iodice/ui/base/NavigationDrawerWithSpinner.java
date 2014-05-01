package com.iodice.ui.base;

import java.util.List;
import java.util.Locale;

import android.app.ActionBar;
import android.app.ActionBar.OnNavigationListener;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.iodice.rssreader.R;

public abstract class NavigationDrawerWithSpinner extends NavigationDrawerBaseActivity
implements ActionBar.OnNavigationListener {
	private static final String TAG = "NavigationDrawerWithSpinner";
	private static final String LAST_SELECTED_LIST_NAV_ITEM = "LAST_SELECTED_LIST_NAV_ITEM";
	protected List<String> spinnerListItemPrimaryKeys = null;
	
	public abstract void setupCategorySpinner();
	public abstract void setupCategorySpinnerWithSelection(String selection);
	public abstract boolean onSpinnerItemClick(int position, long id);
	// must return a pair with an adapter & the primary keys for the spinner list. These primary keys
	// will be used for automated item selection, as well as other things. The spinnerListItemPrimaryKeys
	// property will be set to the value of this list
	public abstract List<String> backgroundSpinnerQuery();
	public abstract String getSpinnerTitleText();

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		String oldSelectedListNavigationItem = null;
		
		if (savedInstanceState != null)
			oldSelectedListNavigationItem = savedInstanceState.getString(LAST_SELECTED_LIST_NAV_ITEM);
	
		if (oldSelectedListNavigationItem == null)
			setupCategorySpinner();
		else
			setupCategorySpinnerWithSelection(oldSelectedListNavigationItem);

		// call this after the other calls because it controls whether or not the spinner
		// is visible
		super.onCreate(savedInstanceState);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		if (this.spinnerListItemPrimaryKeys != null) {
			int selListItemIdx = getSelectedNavigationIndex();
			if (selListItemIdx >= 0 && selListItemIdx < spinnerListItemPrimaryKeys.size())
				outState.putString(LAST_SELECTED_LIST_NAV_ITEM, this.spinnerListItemPrimaryKeys.get(selListItemIdx));
			else 
				Log.e(TAG, "spinnerListItem index out of bounds!");
		}
		super.onSaveInstanceState(outState);
	}
	
    private int getSelectedNavigationIndex() {
    	ActionBar ab = getActionBar();
    	int oldNavMode = ab.getNavigationMode();
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		int i = ab.getSelectedNavigationIndex();
		ab.setNavigationMode(oldNavMode);
		return i;
    }
    private void setSelectedNavigationIndex(int i) {
    	ActionBar ab = getActionBar();
    	int oldNavMode = this.getActionBar().getNavigationMode();
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		ab.setSelectedNavigationItem(i);
		ab.setNavigationMode(oldNavMode);
    }
   
	
	
	@Override
	// when a spinner item is selected, this method is called. returns true if the
	// event was handled, false otherwise
	public boolean onNavigationItemSelected(int position, long id) {

		if (this.spinnerListItemPrimaryKeys == null) {
			Log.e(TAG, "spinner items are null! This should never happen");
			return false;
		} else if (position < 0 || position >= this.spinnerListItemPrimaryKeys.size()) {
			Log.e(TAG, "item position in onNavigationItemSelected is out of range");
			return false;
		}
		return this.onSpinnerItemClick(position, id);
	}
	
	// used to pass data from inherited classes to built in methods. Primary callers
	// are the backgroundSpinnerQuery from the PopulateActionBarSpinner class
	protected class AdapterListPair {
		private SpinnerAdapter aAdapt = null;
		private List<String> comparisonKeyList = null;

		public AdapterListPair() {
		}
		
		public SpinnerAdapter getAdapter() {
			return this.aAdapt;
		}
		public void setAdapter(SpinnerAdapter aAdapt) {
			this.aAdapt = aAdapt;
		}
		public List<String> getComparisonKeyList() {
			return this.comparisonKeyList;
		}
		public void setComparisonKeyList(List<String> comparisonKeyList) {
			this.comparisonKeyList = comparisonKeyList;
		}
	}

	
	// handles query to load the action bar details
	public class PopulateActionBarSpinner extends AsyncTask<Void, Void, SpinnerAdapter> {

		private OnNavigationListener listener = null;
		private String requestedSelection = null;
		
		public void setOnNavigationListener(OnNavigationListener listener) {
			this.listener = listener;
		}
		public void setSelectionIfPossible(String s) {
			this.requestedSelection = s;
		}
		protected void onPreExecute() {
			// Set up the action bar to show a dropdown list.
			final ActionBar actionBar = getActionBar();
			actionBar.setDisplayShowTitleEnabled(false);
			actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		}
		
		// process db request in background thread
		protected SpinnerAdapter doInBackground(Void... arg0) {
			List<String> items = backgroundSpinnerQuery();
			spinnerListItemPrimaryKeys = items;
			// note: actionBar.getThemedContext() ensures the correct colors based on the action bar theme.
			// note: use text2 because it is the bottom text element. text1 holds descriptive text
			MyArrayAdapter aAdpt = new MyArrayAdapter(getActionBar().getThemedContext(),
					android.R.layout.simple_list_item_1,
					android.R.id.text1,
					items);
			aAdpt.setTitleText(getSpinnerTitleText());
			return aAdpt;
		}
		
	    // Call the callback function to refresh UI
		protected void onPostExecute(SpinnerAdapter aAdpt) {
			if (listener == null)
				throw new NullPointerException();

			if (aAdpt == null)
				throw new NullPointerException();
			
			getActionBar().setListNavigationCallbacks(aAdpt, listener);
			
			// select the proper list selection
			if (this.requestedSelection != null) {
				int listItemCnt = spinnerListItemPrimaryKeys.size();
				for (int i = 0; i < listItemCnt; i++) {
					if (spinnerListItemPrimaryKeys.get(i).equals(requestedSelection) == true) {
						setSelectedNavigationIndex(i);
						break;
					}
						
				}
			}
			Log.i(TAG, "post xecute");
		}
	
	}
	
	
	
	private class MyArrayAdapter extends ArrayAdapter<String> {
		private String titleText = null;
		private Context context;
		private List<String> items = null;
		
		public MyArrayAdapter(Context context, 
				int listItemLayout,
				int contentLayoutId,
				List<String> items) {
			super(context, listItemLayout, contentLayoutId, items);
			
			this.context = context;
			this.items = items;
		}
		
		public void setTitleText(String t) {
			if (t != null)
				this.titleText = t;
			else
				throw new NullPointerException();
		}
	
		// the selected item has a subtitle
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater mInflater = (LayoutInflater)context.getSystemService(Activity.LAYOUT_INFLATER_SERVICE);
            View v = mInflater.inflate(R.layout.two_line_spinner_list_item, null);
            
            TextView title = (TextView)v.findViewById(R.id.text_top);
            title.setText(titleText);
            
            TextView userContent = (TextView)v.findViewById(R.id.text_bottom);
            String contentSelection = items.get(position);
            userContent.setText(contentSelection.toUpperCase(Locale.US));
            return v;
		 }
		 
		 // the drop down view has no subtitle
		 @Override
		 public View getDropDownView(int position, View convertView, ViewGroup parent) {
			 View v = super.getView(position, null, parent);
			 TextView content = (TextView)v.findViewById(android.R.id.text1);
			 content.setTextAppearance(context, R.style.Spinner_Large_Text);
			 content.setSelected(false);
			 content.setPressed(false);
			 return v;
		 }
	
	}
	
}
