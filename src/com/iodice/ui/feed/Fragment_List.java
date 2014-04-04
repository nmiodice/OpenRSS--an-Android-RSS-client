package com.iodice.ui.feed;

import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;

import com.iodice.database.feedsOrm;
import com.iodice.rssreader.R;
import com.iodice.ui.List_Base;

public class Fragment_List extends List_Base {

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
		Cursor cursor;		
		
		columns = new String[] {
			feedsOrm.COLUMN_NAME,
			feedsOrm.COLUMN_URL
		};
		to = new int[] { 
		    R.id.feed_name,
		    R.id.feed_url
		};
		cursor = feedsOrm.selectAllOrderBy(getActivity().getApplicationContext(), feedsOrm.COLUMN_NAME);

		// create the adapter using the cursor pointing to the desired data 
		// as well as the layout information
		setAdapter(cursor, columns, to, R.layout.feed_list_view);
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
		// TODO Auto-generated method stub
		return v;
	}

}
