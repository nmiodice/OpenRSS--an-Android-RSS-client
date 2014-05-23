package com.iodice.ui.base;

import java.util.ArrayList;
import java.util.List;

import android.database.Cursor;
import android.view.ActionMode;
import android.view.Menu;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ListView;

import com.nhaarman.listviewanimations.itemmanipulation.OnDismissCallback;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.SwipeDismissAdapter;

abstract class MultiselectSwipeList 
extends CabMultiselectList 
implements OnDismissCallback {
	
	/**
	 * User defined logic for item swipe
	 * @param removed
	 */
	abstract protected void onItemSwiped(List<Integer> removed);

	@Override
	/**
	 * Disable swipe when CAB is active
	 */
	protected boolean onPreCreateActionMode(ActionMode mode, Menu menu) {
		super.onPreCreateActionMode(mode, menu);
    	ListView listView = getListView();
        SwipeDismissAdapter adapt = (SwipeDismissAdapter)listView.getAdapter();
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
        SwipeDismissAdapter adapt = (SwipeDismissAdapter)listView.getAdapter();
        adapt.toggleSwipe();
	}
	
    protected void setSwipeDismissAdapter() {
    	BaseAdapter mAdapter = (BaseAdapter) this.getListAdapter();
        SwipeDismissAdapter adapter = new SwipeDismissAdapter(mAdapter, this);
        adapter.setAbsListView(getListView());
        getListView().setAdapter(adapter);
    }
    
    @Override
	public void setAdapter(Cursor cursor, String[] columns, int[] to, int layout) {
		super.setAdapter(cursor, columns, to, layout);
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


}
