package com.iodice.ui.feed;

import java.util.List;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.TextView;

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
		// TODO Auto-generated method stub

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
		setAdapter(cursor, columns, to, R.layout.fragment_rss);
	}

	@Override
	public MultiChoiceModeListener getChoiceListener() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public View onListElementRedraw(int position, View convertView,
			ViewGroup parent) {
		View v = convertView;
		
		// not all articles supply an author
		TextView tmpView = (TextView) v.findViewById(R.id.rss_author);
		if (tmpView.getText().equals(""))
			tmpView.setVisibility(View.GONE);
		
		return v;
	}
	
	public void setFeeds(List<String> urlList) {
		if (urlList != null)
			this.articleURLList = urlList;
	}

}
