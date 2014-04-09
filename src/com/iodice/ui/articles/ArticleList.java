package com.iodice.ui.articles;

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
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.iodice.database.ArticleOrm;
import com.iodice.rssreader.R;
import com.iodice.ui.ListBase;
import com.iodice.utilities.Text;

public class ArticleList extends ListBase {
	
	private List<String> articleURLList;
	private static final String TAG = "ArticleList";

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
	public void cabOnMultipleItemClick() {
		Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
		sharingIntent.setType("text/plain");
		
        ListView v = getListView();
        Cursor c;
        String shareBody = "";
        int size = selectedListItems.size();
        
        // do nothing with an empty selection
        if (size == 0) {
			Toast.makeText(getActivity().getApplicationContext(), getResources().getText(R.string.no_selections).toString(),  Toast.LENGTH_SHORT).show();
    		return;
    	// format the subject = title
        } else if (size == 1) {
        	c = (Cursor)v.getItemAtPosition(0);
     		shareBody = c.getString(c.getColumnIndex(ArticleOrm.COLUMN_DESCRIPTION));
     		shareBody += "\n" + c.getString(c.getColumnIndex(ArticleOrm.COLUMN_URL));
     		
    		sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, c.getString(c.getColumnIndex(ArticleOrm.COLUMN_TITLE)));
    		sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
    	// list the details of each together with a unique subject line
        } else {
            for (int i = 0; i < size; i++) {
            	c = (Cursor)v.getItemAtPosition(i);
            	if (c == null) {
            		Log.e(TAG, "Error: Cursor element is null: should never happen!");
            		continue;
            	}
        		shareBody += c.getString(c.getColumnIndex(ArticleOrm.COLUMN_TITLE));
        		shareBody += "\n" + c.getString(c.getColumnIndex(ArticleOrm.COLUMN_DESCRIPTION));
        		shareBody += "\n" + c.getString(c.getColumnIndex(ArticleOrm.COLUMN_URL));
        		shareBody += "\n\n";
            }
    		sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, this.getText(R.string.article_share_subject));
    		sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
        }		
		startActivity(Intent.createChooser(sharingIntent, "Share via"));
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
    	String desc = tmp.getText().toString();
        if (!desc.equals("")) {
        	int maxLen = getResources().getInteger(R.integer.article_description_max_length);
        	if (desc.length() > maxLen) {
        		desc = Text.limitTextCharacters(desc, maxLen);
        		desc += "...";
        	}
        	tmp.setText(desc);
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
	public boolean cabRespondToMenuItemClick(ActionMode mode,
			MenuItem item) {
		switch (item.getItemId()) {
		
		    case R.id.action_select_all:
		    	selectAll();
		    	return true;
		    	
		    case R.id.action_deselect_all:
		    	deselectAll();
		    	return true;
		    	
		    case R.id.action_share_selected:
		    	this.cabOnMultipleItemClick();
	            mode.finish();
		    	return true;
		    	
		    default:
		        return false;
		}
	}

	@Override
	public int cabGetMenuLayoutId() {
		return R.menu.articles_cab;
	}

}
