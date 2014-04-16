package com.iodice.ui;

/* *
 * A simple extension of of an exsting helper class that adds a nice animation 
 * for views as they enter the screen from either the top or bottom edge. 
 * 
 * This functionality was taken directly from Kyle W. Banks and is avaliable here:
 * 	http://kylewbanks.com/blog/Implementing-Google-Plus-Style-ListView-Animations-on-Android
 */

import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;

import com.iodice.rssreader.R;


public abstract class AnimatedEntryList extends ListBase {
	private int lastPosition = -1;

    @Override
    public View onListElementRedraw(int position, View convertView, ViewGroup parent) {
		View v = super.onListElementRedraw(position, convertView, parent);
		if (v != null) {
			Animation animation = AnimationUtils.loadAnimation(getActivity(), 
					(position > lastPosition) ? R.animator.up_from_bottom : R.animator.down_from_top);
			animation.setInterpolator(new DecelerateInterpolator());
		    v.startAnimation(animation);
		    lastPosition = position;
		}
    	return v;
    }

}