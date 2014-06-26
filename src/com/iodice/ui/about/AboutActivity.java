package com.iodice.ui.about;

import android.util.Log;

import com.iodice.rssreader.R;
import com.iodice.ui.base.NavigationDrawerBaseActivity;
import com.iodice.utilities.Text;

public class AboutActivity extends NavigationDrawerBaseActivity {

	@Override
	public int getViewLayoutId() {
		String myString;
		myString = Text.readRawTextResource(R.string.license_jsoup, this);
		Log.i("", myString);
		
		myString = Text.readRawTextResource(R.string.license_android_swipe_list_view, this);
		Log.i("", myString);
		
		myString = Text.readRawTextResource(R.string.license_roboto_slab, this);
		Log.i("", myString);
		
		myString = Text.readRawTextResource(R.string.license_rome_jdom, this);
		Log.i("", myString);
		
		
		return R.layout.about_activity;
	}

	@Override
	public int[] getViewsToHidewOnDrawerOpen() {
		return null;
	}

	@Override
	protected boolean isActionBarDrawerIndicatorVisible() {
		return false;
	}

}
