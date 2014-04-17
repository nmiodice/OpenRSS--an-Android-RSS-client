package com.iodice.ui;

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

import com.iodice.rssreader.R;


public abstract class AnimatedEntryList extends ListBase {
	private int lastPosition = -1;
	private boolean isScrolling = false;
	
	@Override
	/**
	 * Called in order to detect if the view is currently scrolling. This prevents unwanted animations
	 * when the listview needs to be redrawn for other reasons -- the swipe up/down animations are only
	 * fun to watch if the list is being scrolled through. Otherwise, its pointless.
	 */
	public void onViewCreated (View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		getListView().setOnScrollListener(new OnScrollListener() {
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		    }
			
		    public void onScrollStateChanged(AbsListView view, int scrollState) {
		    	if(scrollState == OnScrollListener.SCROLL_STATE_IDLE)
		        	isScrolling = false;
		    	else
		        	isScrolling = true;
		    }
		});
		
	}

	/**
	 * Handles animation
	 */
    @Override
    public View onListElementRedraw(int position, View convertView, ViewGroup parent) {
		View v = super.onListElementRedraw(position, convertView, parent);
		if (v != null && isScrolling == true && convertView.getVisibility() == View.VISIBLE) {
			Animation animation = AnimationUtils.loadAnimation(getActivity(), 
					(position > lastPosition) ? R.animator.up_from_bottom : R.animator.down_from_top);
			animation.setInterpolator(new DecelerateInterpolator());
		    v.startAnimation(animation);
		    lastPosition = position;
		}
		
    	return v;
    }

}