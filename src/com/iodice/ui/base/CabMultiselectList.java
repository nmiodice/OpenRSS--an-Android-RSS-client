package com.iodice.ui.base;

import java.util.ArrayList;
import java.util.List;

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
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

import com.iodice.rssreader.R;
import com.nhaarman.listviewanimations.itemmanipulation.OnDismissCallback;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.SwipeDismissAdapter;

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
 * 
 * @author Nicholas M. Iodice
 *
 */
public abstract class CabMultiselectList 
extends ListFragment
implements OnDismissCallback {
	private final String TAG = "List_Base";

	protected ArrayList<Integer> selectedListItems = new ArrayList<Integer>();
	protected ArrayList<Integer> hiddenListItems = new ArrayList<Integer>();
	protected boolean isInActionMode = false;
	
	abstract protected void onItemSwiped(List<Integer> removed);
	/**
	 * The action taken out when a single list item is clicked while the
	 * contextual action bar is not visible
	 * @param view
	 */
    abstract public void onSingleItemClick(View view);
    
    /**
     * The primary action to be taken when multiple items are selected while
     * the contextual action bar is visible. This method isnt currently
     * called internally.
     */
    abstract public void cabMultiselectPrimaryAction();
    
    /**
     * Populate the list data. The proper implementation for this method will do
     * the following:
     * 	1) run asynchronously, likely as an AsyncTask
     * 	2) call setAdapter(...) instead of setListAdapter(...) 
     * 
     * (1) is suggested, (2) is required
     */
    abstract public void setUpAdapter();
    
    /**
     * Respond to a menu item click while the contextual action bar is present. This is
     * a wrapper for the android library 'onActionItemClicked' method, and is only here
     * to make the base API method calls consistent
     * 
     * @param mode Same as from the 'onActionItemClicked' android library method call 
     * @param item Same as from the 'onActionItemClicked' android library method call
     */
    
    abstract public boolean cabOnMenuItemClicked(ActionMode mode, MenuItem item);
    /**
     * @return The menu layout ID to be used while the contextual action bar is active
     */
    abstract public int cabGetMenuLayoutId();
    
    /**
     * 
     * @return The row layout ID for a list item
     */
    abstract protected int getListItemLayoutID();
    
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
			throw new UnsupportedOperationException();
		}
		onSingleItemClick(v);
	}
 
    
	@Override
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
		setSwipeDismissAdapter();
	}
	
    protected void setSwipeDismissAdapter() {
    	BaseAdapter mAdapter = (BaseAdapter) this.getListAdapter();
        SwipeDismissAdapter adapter = new SwipeDismissAdapter(mAdapter, this);
        adapter.setAbsListView(getListView());
        getListView().setAdapter(adapter);
    }
    
    /**
     * Called in response to a list item being dismissed. This call forwards
     * the dismissed positions to the subclass
     */
    public void onDismiss(AbsListView listView, int[] reverseSortedPositions) {
		ArrayList<Integer> removed = new ArrayList<Integer>();
		int length = reverseSortedPositions.length;
		for (int i = 0; i < length; i++)
			removed.add(reverseSortedPositions[i]);		
        onItemSwiped(removed);
    }
	
	/**
	 * Select all the list elements, or if all of them are already selected,
	 * deselect all list elements
	 */
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
    
    /**
     * Deselect all list elements
     */
    public void deselectAll() {
        selectedListItems.clear();
        redrawListView();
    }
    
    /**
     * Select the checkbox at the specified list position
     * @param position Absolute position in the list, not the position out of all
     * visible list items
     */
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

    
    /**
     * The base implementation takes care of redrawing checkboxes to ensure that they are selected
     * or deselected as appropriate. This method also provides flexibility for the client to
     * redraw or change any features of the list element. For more complex lists it is suggested
     * that this method be overridden (but also for it to call super.onListElementRedraw as well)
     * 
     * @param position Absolute position in the list
     * @param view View to be redrawn before modification
     * @param parent Parent view group
     * @return View to be redrawn after modification
     */
    // this logic is applied to each list row whenever the list needs to be re-drawn. It tracks whether or not
    // a row should have its checkboxes visible and/or checked
    public View onListElementRedraw(int position, View view, ViewGroup parent) {

    	CheckBox bx = (CheckBox) view.findViewById(R.id.item_checkbox);
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
		
    	return view;
    }
	
	/** 
	 * Redraw a list, presumably because the underlying data set has changed and views
	 * need to be updtated
	 */
	public void redrawListView() {
		MySimpleCursorAdapter adapt = (MySimpleCursorAdapter)this.getListAdapter();
		adapt.notifyDataSetChanged();
	}
	
	/**
	 * Replace the current dataset & redraw the list. The adapter is not recreated
	 * inside this method
	 * @param cursor The cursor containig the new set of data
	 */
	public void replaceCurrentData(Cursor cursor) {
		MySimpleCursorAdapter adapt = (MySimpleCursorAdapter)this.getListAdapter();
		// because data collection is handled in a separate thread, the adapter
		// may be null if the data is replaced prior to the thread execution
		if (cursor != null && adapt != null) {
			adapt.changeCursor(cursor);
			adapt.notifyDataSetChanged();
		} else
			Log.e(TAG, "Adapter or cursor is null! adapter = " + adapt + ", cursor = " + cursor);
	}
	
	/**
	 * @return A MultiChoiceModeListener that provides basic functionality for the list
	 * while the contextual actionbar is created. This calls into the parent class for
	 * basic functionality, allowing the user to override specific actions if desired
	 */
	private MultiChoiceModeListener getChoiceListener() {
		return new MultiChoiceModeListener() {
			
		    @Override
	        // Inflate the menu for the CAB
		    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
		    	int contextualMenuView = cabGetMenuLayoutId();
		    	MenuInflater inflater = mode.getMenuInflater();
		    	inflater.inflate(contextualMenuView, menu);

		    	ListView listView = getListView();
		        int vCnt = listView.getCount();
		        View child;
		        CheckBox bx;
		        
		        SwipeDismissAdapter adapt = (SwipeDismissAdapter)listView.getAdapter();
		        adapt.untoggleSwipe();
		        
		        isInActionMode = true;
		        
		        for (int i = 0; i < vCnt; i++) {
		        	child = listView.getChildAt(i);
		        	if (child == null)
		        		continue;
		        	listView.setSelected(true);
		        	bx = (CheckBox) child.findViewById(R.id.item_checkbox);
		        	bx.setVisibility(View.VISIBLE);
		        	bx.setChecked(false);
		        }
		        
		        return true;
		    }
		
		    @Override
	        // Respond to clicks on the actions in the CAB
		    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
		    	return cabOnMenuItemClicked(mode, item);
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
		        ListView listView = getListView();
		        SwipeDismissAdapter adapt = (SwipeDismissAdapter)listView.getAdapter();
		        adapt.toggleSwipe();
		    }
		    @Override
	        // Here you can perform updates to the CAB due to
	        // an invalidate() request
		    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
		    	return false;
		    }
		
		};
	}
	
	
	
	

	/**
	 * A SimpleCursorAdapter that allows for customization whenever a row layout
	 * needs to be re-drawn. Customization done through the onListElementRedraw()
	 * abstract method
	 * 
	 * @author Nicholas M. Iodice
	 *
	 */
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
