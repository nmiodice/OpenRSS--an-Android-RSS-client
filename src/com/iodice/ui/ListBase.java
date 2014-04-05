package com.iodice.ui;

import java.util.ArrayList;

import android.app.ListFragment;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public abstract class ListBase extends ListFragment {
	private final String TAG = "List_Base";

	protected ArrayList<Integer> selectedListItems = new ArrayList<Integer>();
	protected boolean isInActionMode = false;
	
    abstract public void onSingleItemClick(View view);
    abstract public void onMultipleItemClick();
    abstract public void setUpAdapter();
    abstract public MultiChoiceModeListener getChoiceListener();
    abstract public View onListElementRedraw(int position, View convertView, ViewGroup parent);

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	// want to retain instance variables when the fragment needs to be redrawn
    	this.setRetainInstance(true);
    	return super.onCreateView(inflater, container, savedInstanceState);
    }
    
    
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		Log.i(TAG, "Item " + position + "clicked");
		onSingleItemClick(v);
	}
 
    
	@Override
	// set up the contextual action bar
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		this.setUpAdapter();
		
		ListView listView = getListView();
		listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
		listView.setMultiChoiceModeListener(getChoiceListener());
	}
	
	
	// uses a custom adapter that allows the user to provide re-draw logic
	public void setAdapter(Cursor cursor, String[] columns, int[] to, int layout) {
		setListAdapter(
				new MySimpleCursorAdapter(
				    getActivity().getApplicationContext(), 
				    layout, 
				    cursor, 
				    columns, 
				    to));
	}
	
	// redraw a list, presumably because the underlying data set has changed and views need to be
	// updtated
	public void redrawListView() {
		// the call to replaceCurrentData is made with the *current set of data* so that the
		// net result is that the list is redrawn. 
		MySimpleCursorAdapter adapt = (MySimpleCursorAdapter)this.getListAdapter();
		Cursor c = adapt.getCursor();
		replaceCurrentData(c);
	}
	
	public void replaceCurrentData(Cursor c) {
		MySimpleCursorAdapter adapt = (MySimpleCursorAdapter)this.getListAdapter();
		int layout = adapt.getLayout();
		String[] columns = adapt.getColumns();
		int[] layoutMapping = adapt.getLayoutMapping();
		
		this.setAdapter(c, columns, layoutMapping, layout);
		adapt = (MySimpleCursorAdapter) this.getListAdapter();
		adapt.notifyDataSetChanged();	
	}

	// a SimpleCursorAdapter that allows for customization whenever a row layout needs to be re-drawn. 
	// Customization done through the onListElementRedraw() abstract method
	private class MySimpleCursorAdapter extends SimpleCursorAdapter {
		
		int layout;
		String[] columns;
		int[] layoutMapping;
		Cursor cursor;

		public MySimpleCursorAdapter(Context context, int layout, Cursor c,
				String[] from, int[] to) {
			super(context, layout, c, from, to, 0);
			this.layout = layout;
			this.columns = from;
			this.layoutMapping = to;
			this.cursor = c;
		}
		
		public int getLayout() {
			return this.layout;
		}
		
		public String[] getColumns() {
			return this.columns;
		}
		
		public int[] getLayoutMapping() {
			return this.layoutMapping;
		}
		@Override
		public Cursor getCursor() {
			return this.cursor;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View v = super.getView(position, convertView, parent);
	    	return onListElementRedraw(position, v, parent);
		}		
	}
}
