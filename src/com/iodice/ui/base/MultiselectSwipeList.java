package com.iodice.ui.base;

import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.iodice.rssreader.R;
import com.iodice.utilities.SwipeToggle;
import com.nhaarman.listviewanimations.itemmanipulation.OnDismissCallback;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.SwipeDismissAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.contextualundo.ContextualUndoAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.contextualundo.ContextualUndoAdapter.DeleteItemCallback;

abstract class MultiselectSwipeList 
extends CabMultiselectList 
implements OnDismissCallback, DeleteItemCallback {
	
	protected boolean enableSwipeUndo = false;
	BaseAdapter mAdapter;

	/**
	 * Invoked after an item is swiped using the 
	 * SwipeDismissAdapter
	 * @param removed
	 */
	abstract protected void onItemSwiped(List<Integer> removed);
	
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
		View v = super.onCreateView(inflater, container, savedInstanceState);
    	return v;
	}
	
	@Override
	/**
	 * Disable swipe when CAB is active
	 */
	protected boolean onPreCreateActionMode(ActionMode mode, Menu menu) {
		super.onPreCreateActionMode(mode, menu);
    	ListView listView = getListView();
        SwipeToggle adapt = (SwipeToggle)listView.getAdapter();
        adapt.untoggleSwipe();
		return true;
	}
	@Override
	/**
	 * Re-enable swipe when CAB is destroyed
	 */
	protected void onPreDestroyActionMode(ActionMode mode) {
		super.onPreDestroyActionMode(mode);
        ListView listView = getListView();
        SwipeToggle adapt = (SwipeToggle)listView.getAdapter();
        adapt.toggleSwipe();
	}
	
	/**
	 * Sets up a simple adapter that allows for swipe to dismiss
	 */
    protected void setSwipeDismissAdapter() {
        SwipeDismissAdapter adapter = new SwipeDismissAdapter(mAdapter, this);
        adapter.setAbsListView(getListView());
        getListView().setAdapter(adapter);
    }
    
    /**
     * Sets up a more complex adapter that allows for an undo notification
     */
    private void setContextualUndoWithTimedDeleteAdapter() {
        ContextualUndoAdapter adapter = new ContextualUndoAdapter(mAdapter, R.layout.undo_row, R.id.undo_row_undobutton, 2000, this);
        adapter.setAbsListView(getListView());
        getListView().setAdapter(adapter);
    }
    
    @Override
	public void setAdapter(Cursor cursor, String[] columns, int[] to, int layout) {
		super.setAdapter(cursor, columns, to, layout);
    	mAdapter = (BaseAdapter) this.getListAdapter();
    	if (enableSwipeUndo)
    		setContextualUndoWithTimedDeleteAdapter();
    	else
    		setSwipeDismissAdapter();
		
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
    
    @Override
    public void deleteItem(final int position) {
    	/* this should never happen */
        if (enableSwipeUndo == false)
        	throw new UnsupportedOperationException();
        
        ArrayList<Integer> removed = new ArrayList<Integer>();
        removed.add(position);
        onItemSwiped(removed);
    }
}
