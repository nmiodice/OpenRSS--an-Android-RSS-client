package com.iodice.rssreader;

import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;

public class Animate_Utils {

	public static void animateToOff(Animation animation, final View v) {
		
		 AnimationListener animationInListener = new AnimationListener() {

			 @Override
			 public void onAnimationEnd(Animation animation) {
				 v.setVisibility(View.GONE);
				 v.clearAnimation();
			 }
			 @Override
			 public void onAnimationRepeat(Animation animation) {		   
			 }
			 @Override
			 public void onAnimationStart(Animation animation) {
			 }};
		animation.setAnimationListener(animationInListener);
		v.startAnimation(animation);
	}
	
}
