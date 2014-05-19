package com.iodice.ui.base;

/* *
 * A simple extension of of an exsting helper class that adds a nice animation 
 * for views as they enter the screen from either the top or bottom edge. 
 * 
 * Parts of this functionality (specifically the animations) were taken directly from 
 * 	Kyle W. Banks and is avaliable here:
 * 		http://kylewbanks.com/blog/Implementing-Google-Plus-Style-ListView-Animations-on-Android
 */

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.BaseAdapter;

import com.iodice.application.SharedPrefsHelper;
import com.iodice.rssreader.R;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.SwipeDismissAdapter;
import com.nhaarman.listviewanimations.itemmanipulation.swipedismiss.SwipeOnScrollListener;


public abstract class AnimatedEntryList 
extends CabMultiselectList {
	private int lastPosition = -1;
	private boolean isScrolling = false;
	/* controlled by shared preferences */
	private static boolean animate = true;
	
	/**
	 * Called in order to detect if the view is currently scrolling. This prevents unwanted animations
	 * when the listview needs to be redrawn for other reasons -- the swipe up/down animations are only
	 * fun to watch if the list is being scrolled through. Otherwise, its pointless.
	 */
	@Override
	public void onViewCreated (View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		boolean animate = SharedPrefsHelper.getEnableAnimations(getActivity());
		setAnimationEnabled(animate);
	}
	
	private SwipeOnScrollListener getScrollListener() {
    	return new SwipeOnScrollListener() {
		    public void onScrollStateChanged(AbsListView view, int scrollState) {
		    	super.onScrollStateChanged(view, scrollState);
		    	if(scrollState == OnScrollListener.SCROLL_STATE_IDLE)
		        	isScrolling = false;
		    	else
		        	isScrolling = true;
		    }
		};
	}
	
	@Override
	/**
	 * Sets up a swipe dismiss adapter & a suitable scroll listener to be
	 * used for animating entry/exit to the screen
	 */
    protected void setSwipeDismissAdapter() {
    	BaseAdapter mAdapter = (BaseAdapter) this.getListAdapter();
    	SwipeOnScrollListener scrollListener = getScrollListener();
        SwipeDismissAdapter adapter = new SwipeDismissAdapter(mAdapter, this, scrollListener);
        adapter.setAbsListView(getListView());
        getListView().setAdapter(adapter);
    }

	/**
	 * Handles animation from top/bottom of the screen
	 */
    @Override
    public View onListElementRedraw(int position, View convertView, ViewGroup parent) {
		View v = super.onListElementRedraw(position, convertView, parent);
		if (v != null 
				&& isScrolling == true 
				&& animate == true
				&& convertView.getVisibility() == View.VISIBLE) {
			Animation animation = AnimationUtils.loadAnimation(getActivity(), 
					(position > lastPosition) ? R.animator.up_from_bottom : R.animator.down_from_top);
			animation.setInterpolator(new DecelerateInterpolator());
		    v.startAnimation(animation);
		    lastPosition = position;
		}
		
    	return v;
    }
    
    public static void setAnimationEnabled(boolean b) {
    	AnimatedEntryList.animate = b;
    }

}