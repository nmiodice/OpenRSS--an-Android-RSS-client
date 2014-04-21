package com.iodice.ui.base;

/**
 * This class provides a simple abstraction of a listview that include support for a 
 * contextual actionbar with multiselection. Most of the legwork is done in this class
 * leaving only critical and customizable functions to override. This allows for visual
 * flexibility with consistent backend behavior.
 * 
 * Important notes:
 * 	1. When providing a row layout, it is important to know that the ListView class
 * 		requests the parent view's child layout. Therefore, any important style must
 * 		not be applied to the top level view, rather it needs to be applied to 
 * 		a child layout
 * 
 * 	2. The current implementation relies on a checkbox included in the layout. See the
 * 		code for details on the name. This may be changed in favor of onSelect() and
 * 		onDeselect() functions that allow the user to control this, possibly with a 
 * 		different selection behavior
 */


import java.util.ArrayList;

import android.app.ListFragment;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.iodice.rssreader.R;

public abstract class MultiselectList extends ListFragment {
	private final String TAG = "List_Base";

	protected ArrayList<Integer> selectedListItems = new ArrayList<Integer>();
	protected boolean isInActionMode = false;
	
    abstract public void onSingleItemClick(View view);
    abstract public void cabOnMultipleItemClick();
    abstract public void setUpAdapter();
    abstract public boolean cabRespondToMenuItemClick(ActionMode mode, MenuItem item);
    abstract public int cabGetMenuLayoutId();

    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	// want to retain instance variables when the fragment needs to be redrawn
    	this.setRetainInstance(true);
    	return super.onCreateView(inflater, container, savedInstanceState);
    }
    
    
	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (this.isInActionMode) {
			Log.e(TAG, "onListItemClick called in action mode! This shouldnt happen!!");
			return;
		}
		onSingleItemClick(v);
	}
 
    
	@Override
	// set up the contextual action bar
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		this.setUpAdapter();
		
		ListView listView = getListView();
		listView.setTextFilterEnabled(true);
		listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
		listView.setMultiChoiceModeListener(getChoiceListener());
	}
	
	// when the list is in load state, it is not shown, hence the negation
	public void setLoadState(boolean isInLoadState) {
		this.setListShown(!isInLoadState);
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
	
	/* if all are selected, deselect all */
    public void selectAll() {
        ListView v = getListView();
        int vCnt = v.getCount();
        
        if (this.selectedListItems.size() == vCnt) {
        	deselectAll();
        	return;
        }
        // clear first in order to avoid repeating items in the list
        this.selectedListItems.clear();
        for (int i = 0; i < vCnt; i++)
        	this.selectedListItems.add(i);
        redrawListView();
    }

    public void deselectAll() {
        selectedListItems.clear();
        redrawListView();
    }
    
    public void selectCheckBox(int position) {
    	ListView v = getListView();
        CheckBox bx; 

		// select list item. Need to account for the the position reported being the actual list position, and
        // getChildAt(position) returning the position relative to the list items visible on the screen. 
    	bx = (CheckBox) v.getChildAt(position - v.getFirstVisiblePosition()).findViewById(R.id.item_checkbox);
        
        if (selectedListItems.contains(position) == false) {
        	selectedListItems.add(position);
            bx.setChecked(true);
        } else {
        	selectedListItems.remove(Integer.valueOf(position));
        	bx.setChecked(false);
        }
    }

    // this logic is applied to each list row whenever the list needs to be re-drawn. It tracks whether or not
    // a row should have its checkboxes visible and/or checked
    public View onListElementRedraw(int position, View v, ViewGroup parent) {
    	CheckBox bx = (CheckBox) v.findViewById(R.id.item_checkbox);
		if (isInActionMode) {
	    	bx.setVisibility(View.VISIBLE);
	    	if (selectedListItems.contains(position)) {
	    		bx.setChecked(true);
	    	} else {
	    		// The view may have been converted. If so, the checkbox needs to be
	    		// manually unchecked
	    		bx.setChecked(false);
	    	}
	    } else {
	    	bx.setChecked(false);
	    	bx.setVisibility(View.GONE);
	    }
		return v;
    }
	
	// redraw a list, presumably because the underlying data set has changed and views need to be
	// updtated
	public void redrawListView() {
		MySimpleCursorAdapter adapt = (MySimpleCursorAdapter)this.getListAdapter();
		adapt.notifyDataSetChanged();
	}
	
	public void replaceCurrentData(Cursor c) {
		MySimpleCursorAdapter adapt = (MySimpleCursorAdapter)this.getListAdapter();
		// because data collection is handled in a separate thread, the adapter
		// may be null if the data is replaced prior to the thread execution
		if (c != null && adapt != null) {
			adapt.changeCursor(c);
			adapt.notifyDataSetChanged();
		} else
			Log.e(TAG, "Adapter or cursor is null! adapter = " + adapt + ", cursor = " + c);
	}
	
	// sets up contextual action bar actions
	private MultiChoiceModeListener getChoiceListener() {
		return new MultiChoiceModeListener() {
			
		    @Override
	        // Inflate the menu for the CAB
		    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		    	int contextualMenuView = cabGetMenuLayoutId();
		    	MenuInflater inflater = mode.getMenuInflater();
		    	inflater.inflate(contextualMenuView, menu);

		    	ListView v = getListView();
		        int vCnt = v.getCount();
		        View child;
		        CheckBox bx;
		        
		        isInActionMode = true;
		        
		        for (int i = 0; i < vCnt; i++) {
		        	child = v.getChildAt(i);
		        	if (child == null)
		        		continue;
		        	bx = (CheckBox) child.findViewById(R.id.item_checkbox);
		        	bx.setVisibility(View.VISIBLE);
		        	bx.setChecked(false);
		        }
		        
		        return true;
		    }
		
		    @Override
	        // Respond to clicks on the actions in the CAB
		    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		    	return cabRespondToMenuItemClick(mode, item);
		    }
			
			@Override
	        // Here you can do something when items are selected/de-selected,
	        // such as update the title in the CAB
		    public void onItemCheckedStateChanged(ActionMode mode, int position,
		                                          long id, boolean checked) {
				selectCheckBox(position);
			}
		    @Override
	        // Here you can make any necessary updates to the activity when
	        // the CAB is removed. By default, selected items are deselected/unchecked.
		    public void onDestroyActionMode(ActionMode mode) {
		        ListView v = getListView();
		        int vCnt = v.getChildCount();
		        CheckBox bx; 

		        // also deselect the checkboxes because they dont correspond to the CAB's notion
		        // of what is selected
		        for (int i = 0; i < vCnt; i++) {
		        	bx = (CheckBox) v.getChildAt(i).findViewById(R.id.item_checkbox);
		        	bx.setChecked(false);
		        	bx.setVisibility(View.GONE);
		        }
		        
		        // clear out selected items
		        selectedListItems.clear();
		        isInActionMode = false;
		    }
		    @Override
	        // Here you can perform updates to the CAB due to
	        // an invalidate() request
		    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		    	return false;
		    }
		
		};
	}
	
	
	
	

	// a SimpleCursorAdapter that allows for customization whenever a row layout needs to be re-drawn. 
	// Customization done through the onListElementRedraw() abstract method
	public class MySimpleCursorAdapter extends SimpleCursorAdapter {
		
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
