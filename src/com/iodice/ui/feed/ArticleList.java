package com.iodice.ui.feed;

import java.util.List;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.iodice.database.ArticleOrm;
import com.iodice.rssreader.R;
import com.iodice.ui.ListBase;

public class ArticleList extends ListBase {
	
	private List<String> articleURLList;
	private static final String TAG = "Fragment_List";

	// this listview uses a tile layout, so the default divider isnt necessary
	public void onViewCreated(View view, Bundle savedInstanceState) {
		getListView().setDivider(null);
		getListView().setDividerHeight(0);
	}
	
	@Override
	public void onSingleItemClick(View view) {
		Intent browserIntent;
		TextView txtview = (TextView) view.findViewById(R.id.rss_url);
		String feedURL = txtview.getText().toString();

		Log.i(TAG, "Opening feed: " + feedURL);
		
		try {
			browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(feedURL));
			startActivity(browserIntent);
		} catch (ActivityNotFoundException e) {
			Toast.makeText(getActivity().getApplicationContext(), R.string.no_browser,  Toast.LENGTH_LONG).show();
			e.printStackTrace();
		} catch (Exception e) {
			e.getMessage();
			e.printStackTrace();
		}
	}

	@Override
	public void onMultipleItemClick() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setUpAdapter() {
		String[] columns;
		int[] to;
		Cursor cursor = null;		
		
		columns = new String[] {
			ArticleOrm.COLUMN_TITLE,
			ArticleOrm.COLUMN_URL,
			ArticleOrm.COLUMN_PARENT_URL,
			ArticleOrm.COLUMN_AUTHOR,
			ArticleOrm.COLUMN_DESCRIPTION,
			ArticleOrm.COLUMN_PUBLISHED_DATE
		};
		
		to = new int[] { 
		    R.id.rss_title,
		    R.id.rss_url,
		    R.id.rss_base_url,
		    R.id.rss_author,
		    R.id.rss_description,
		    R.id.rss_published_date
		};
		cursor = ArticleOrm.selectWhereParentLinkIs(getActivity().getApplicationContext(), this.articleURLList);
		Log.i(TAG, "" + cursor.getCount() + " articles loaded");
		// create the adapter using the cursor pointing to the desired data 
		// as well as the layout information
		setAdapter(cursor, columns, to, R.layout.article_list_row);
	}

	@Override
	public View onListElementRedraw(int position, View convertView,
			ViewGroup parent) {
		// important to always call the parent, as it takes care of redrawing the checkboxes accurately
		// when in action mode
		View v = super.onListElementRedraw(position, convertView, parent);;
		
		// not all articles supply an author
		TextView tmp = (TextView) v.findViewById(R.id.rss_author);
		if (tmp.getText().equals(""))
			tmp.setVisibility(View.GONE);

        // query local parameters to see whether or not we have data to 
        // populate the fragment with
    	tmp = (TextView) v.findViewById(R.id.rss_author);
		if (tmp.getText().equals(""))
			tmp.setVisibility(View.GONE);

    	tmp = (TextView) v.findViewById(R.id.rss_description);
    	String tmpStr = tmp.getText().toString();
    	String originalStr = tmpStr;
        if (!tmpStr.equals("")) {
        	// display only the first 200 characters of the description. add an elipsis at the end
        	// of the last word in the first 200 characters
        	tmpStr = tmpStr.substring(0, Math.min(tmpStr.length(), 200));
        	if (tmpStr.length() < originalStr.length()) {
        		do {
        			tmpStr = tmpStr.substring(0, tmpStr.length() - 1);
        			if (tmpStr.equals("") == true)
        				break;
        		} while (tmpStr.endsWith(" ") == false);
        		
        		if (tmpStr.length() > 0)
        			tmpStr = tmpStr.substring(0, tmpStr.length() - 1);
        		
        		tmpStr = tmpStr + "...";
        	}
        	tmp.setText(tmpStr);
        }
        else
        	tmp.setVisibility(View.GONE);
        
    	tmp = (TextView) v.findViewById(R.id.rss_title);
		if (tmp.getText().equals(""))
			tmp.setVisibility(View.GONE);
        
        tmp = (TextView) v.findViewById(R.id.rss_base_url);
		if (tmp.getText().equals(""))
			tmp.setVisibility(View.GONE);  
		
		// the url never needs to be shown, it only holds data to launch the article
		// in a browser
        tmp = (TextView) v.findViewById(R.id.rss_url);
		tmp.setVisibility(View.GONE);   
        
    	tmp = (TextView) v.findViewById(R.id.rss_published_date);
		if (tmp.getText().equals(""))
			tmp.setVisibility(View.GONE);

		
		return v;
	}
	
	public void setFeeds(List<String> urlList) {
		if (urlList != null)
			this.articleURLList = urlList;
	}

	@Override
	public boolean respondToContextualActionBarMenuItemClick(ActionMode mode,
			MenuItem item) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public int getContextualMenuViewId() {
		return R.menu.main_name_context;
	}

}
