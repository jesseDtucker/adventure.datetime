/*
 *	Copyright (c) 2013 Andrew Fontaine, James Finlay, Jesse Tucker, Jacob Viau, and
 * 	Evan DeGraff
 *
 * 	Permission is hereby granted, free of charge, to any person obtaining a copy of
 * 	this software and associated documentation files (the "Software"), to deal in
 * 	the Software without restriction, including without limitation the rights to
 * 	use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * 	the Software, and to permit persons to whom the Software is furnished to do so,
 * 	subject to the following conditions:
 *
 * 	The above copyright notice and this permission notice shall be included in all
 * 	copies or substantial portions of the Software.
 *
 * 	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * 	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * 	FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * 	COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * 	IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * 	CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package ca.cmput301f13t03.adventure_datetime.view;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;
import ca.cmput301f13t03.adventure_datetime.R;
import ca.cmput301f13t03.adventure_datetime.model.AccountService;
import ca.cmput301f13t03.adventure_datetime.model.Bookmark;
import ca.cmput301f13t03.adventure_datetime.model.Interfaces.IBookmarkListListener;
import ca.cmput301f13t03.adventure_datetime.model.Interfaces.ICurrentStoryListener;
import ca.cmput301f13t03.adventure_datetime.model.Story;
import ca.cmput301f13t03.adventure_datetime.serviceLocator.Locator;

import java.util.Map;
import java.util.UUID;

/**
 * Show synopsis & more details about selected story. User can then play the selected story.
 * Utilizes fragments to allow swiping through available stories.
 * 
 * @author James Finlay
 */
public class StoryDescription extends Activity implements ICurrentStoryListener, IBookmarkListListener {
	private static final String TAG = "StoryDescription";
	public static final String SERVER = "doge.such.server";

	private ViewPager _viewPager;
	private Map<UUID, Bookmark> _bookmarks;
	private Map<UUID, Story> _stories;
	private Story _story;
	private int source;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.story_descript);

		source = getIntent().getIntExtra(SERVER, -1);
	}
	@Override
	public void OnBookmarkListChange(Map<UUID, Bookmark> newBookmarks) {
		_bookmarks = newBookmarks;
		setUpView();
	}
	@Override
	public void OnCurrentStoryChange(Story story) {
		_story = story;
		setUpView();
	}
	@Override
	public void onSaveInstanceState(Bundle outState) {}

	private void setUpView() {
		if (_story == null) return;
		if (_bookmarks == null) return;

		/** Layout items **/
		getActionBar().setTitle(_story.getTitle());

		/** Layout items **/
		Button play = (Button) findViewById(R.id.play); 
		Button restart = (Button) findViewById(R.id.restart);
        ImageView thumbnail = (ImageView) findViewById(R.id.thumbnail);
		TextView title  = (TextView) findViewById(R.id.title);
		TextView author  = (TextView) findViewById(R.id.author);
		TextView datetime = (TextView) findViewById(R.id.datetime);
		TextView fragments = (TextView) findViewById(R.id.fragments);
		TextView content = (TextView) findViewById(R.id.content);

		title.setText(_story.getTitle());
		author.setText("Author: " + _story.getAuthor());
		datetime.setText("Last Modified: " + _story.getFormattedTimestamp());
		fragments.setText("Fragments: " + _story.getFragmentIds().size());
		content.setText(_story.getSynopsis());

        thumbnail.setImageBitmap(_story.getThumbnail().decodeBitmap());

		if (_bookmarks.containsKey(_story.getId())) {
			play.setText("Continue Story");
			restart.setText("Start from the Beginning");
		} else {
			play.setVisibility(View.GONE);
			restart.setText("Play Story");
		}

		play.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Launch Story
				Locator.getUserController().ResumeStory(_story.getId());
				Intent intent = new Intent(StoryDescription.this, FragmentViewActivity.class);
				intent.putExtra(FragmentViewActivity.FOR_SERVER, source==BrowseFragment.SOURCE_ONLINE);
				startActivity(intent);
			}
		});

		restart.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				// Restart & Launch Story
				Locator.getUserController().StartStory(_story.getId());
				Intent intent = new Intent(StoryDescription.this, FragmentViewActivity.class);
				intent.putExtra(FragmentViewActivity.FOR_SERVER, source==BrowseFragment.SOURCE_ONLINE);
				startActivity(intent);
			}
		});
	}

	@Override
	public void onResume() {
		Locator.getPresenter().Subscribe((ICurrentStoryListener)this);
		Locator.getPresenter().Subscribe((IBookmarkListListener)this);
		super.onResume();
	}

	@Override
	public void onPause() {
		Locator.getPresenter().Unsubscribe((ICurrentStoryListener)this);
		Locator.getPresenter().Unsubscribe((IBookmarkListListener)this);
		super.onPause();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		switch (source) {
		case BrowseFragment.SOURCE_ONLINE:
			getMenuInflater().inflate(R.menu.story_online, menu);
			break;
		case BrowseFragment.SOURCE_CACHE:
			getMenuInflater().inflate(R.menu.story_cache, menu);
			break;
		case BrowseFragment.SOURCE_AUTHOR:
			getMenuInflater().inflate(R.menu.story_author, menu);
			break;
		default:
			Log.e(TAG, "Something fked up");
			return false;
		}
		return true;
	}
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_comment:
			/**  Launch **/
			Locator.getUserController().StartStory(_story.getId());
			Intent intent = new Intent(this, CommentsView.class);
			intent.putExtra(CommentsView.COMMENT_TYPE, true);
			startActivity(intent);
			break;
		case R.id.action_download:
			/* Download to cache */
			Locator.getUserController().download();
			Toast.makeText(getApplicationContext(), "Downloaded!", Toast.LENGTH_LONG).show();
			break;
		case R.id.action_edit:
			/* Move from cache to authorship */
			Locator.getAuthorController().setStoryToAuthor(_story.getId(), AccountService.getUserName(getContentResolver()));
			intent = new Intent(StoryDescription.this, AuthorStoryDescription.class);
			startActivity(intent);	
			break;
		}
		return super.onOptionsItemSelected(item);
	}
}
