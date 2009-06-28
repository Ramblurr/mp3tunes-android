/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mp3tunes.android.activity;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;

import android.database.AbstractCursor;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

import com.mp3tunes.android.LockerDb;
import com.mp3tunes.android.Music;
import com.mp3tunes.android.R;

public class QueryBrowser extends ListActivity implements Music.Defs
{
    private final static int PLAY_NOW = 0;
    private final static int ADD_TO_QUEUE = 1;
    private final static int PLAY_NEXT = 2;
    private final static int PLAY_ARTIST = 3;
    private final static int EXPLORE_ARTIST = 4;
    private final static int PLAY_ALBUM = 5;
    private final static int EXPLORE_ALBUM = 6;
    private final static int REQUERY = 3;
    private final static int PROGRESS = 7;

    private QueryListAdapter mAdapter;
    private boolean mAdapterSent;
    private String mFilterString = "";
    private ListView mTrackList;
    private SearchCursor mQueryCursor;
    private AlertDialog mProgDialog;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        Music.bindToService(this);
        
        if (icicle == null) {
            Intent intent = getIntent();
            
            if (intent.getAction().equals(Intent.ACTION_SEARCH)) {
               System.out.println("ACTION = SEARCH!");
            }
            mFilterString = intent.getStringExtra(SearchManager.QUERY);
        }

        setContentView(R.layout.query);
        
        AlertDialog.Builder builder;
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.progress_dialog,
                                       (ViewGroup) findViewById(R.id.layout_root));

        TextView text = (TextView) layout.findViewById(R.id.progress_text);
        text.setText(R.string.loading_albums);

        builder = new AlertDialog.Builder(this);
        builder.setView(layout);
        mProgDialog = builder.create();
        
        mTrackList = getListView();
        mTrackList.setTextFilterEnabled(true);
        mAdapter = (QueryListAdapter) getLastNonConfigurationInstance();
        if (mAdapter == null) {
            System.out.println("adapter == null");
            mAdapter = new QueryListAdapter(
                    getApplication(),
                    this,
                    R.layout.track_list_item,
                    null, // cursor
                    new String[] {},
                    new int[] {});
            setListAdapter(mAdapter);
            if (!TextUtils.isEmpty(mFilterString)) {
                
//            } else {
                new SearchTask().execute( mFilterString );
                mTrackList.setFilterText(mFilterString);
                mFilterString = null;
            }
        } else {
            System.out.println("adapter != null");
            mAdapter.setActivity(this);
            setListAdapter(mAdapter);
            mQueryCursor = (SearchCursor) mAdapter.getCursor();
            if (mQueryCursor != null) {
                init(mQueryCursor);
            } else {
                new SearchTask().execute( mFilterString );
            }
        }
    }
    
    @Override
    public Object onRetainNonConfigurationInstance() {
        mAdapterSent = true;
        return mAdapter;
    }
    
    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        Music.unbindFromService(this);
        super.onDestroy();
        if (!mAdapterSent && mAdapter != null) {
            Cursor c = mAdapter.getCursor();
            if (c != null) {
                c.close();
            }
        }
    }
    
    @Override
    protected Dialog onCreateDialog( int id )
    {
        switch ( id )
        {
        case PROGRESS:
            return mProgDialog;
        default:
            return null;
        }
    }
    
    public void init(SearchCursor c) {
        
        mAdapter.changeCursor(c);

        if (mQueryCursor == null) {
//            Music.displayDatabaseError(this);
            setListAdapter(null);
            return;
        }
//        Music.hideDatabaseError(this);
    }
    
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        // Dialog doesn't allow us to wait for a result, so we need to store
        // the info we need for when the dialog posts its result
        mQueryCursor.moveToPosition(position);
        if (mQueryCursor.isBeforeFirst() || mQueryCursor.isAfterLast()) {
            return;
        }
        String selectedType = mQueryCursor.getMimetype();
        
        if ("artist".equals(selectedType)) 
        {
            String artist = mQueryCursor.getString(Music.ARTIST_MAPPING.ID);
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(Uri.EMPTY, "vnd.mp3tunes.android.dir/album");
            intent.putExtra("artist", artist);
            startActivity(intent);
        } else if ("album".equals(selectedType)) {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
            intent.putExtra("album", Long.valueOf(id).toString());
            startActivity(intent);
        } else if (position >= 0 && id >= 0){
            int track = mQueryCursor.getInt(Music.TRACK_MAPPING.ID);
            int [] list = new int[] { track };
            Music.playAll(this, list, 0);
        } else {
            Log.e("QueryBrowser", "invalid position/id: " + position + "/" + id);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case USE_AS_RINGTONE: {
//                // Set the system setting to make this the current ringtone
//                Music.setRingtone(this, mTrackList.getSelectedItemId());
//                return true;
            }

        }
        return super.onOptionsItemSelected(item);
    }
    
    private class SearchCursor extends AbstractCursor
    {
        Cursor mArtists;
        Cursor mTracks;
        
        
        public SearchCursor(Cursor artists, Cursor tracks)
        {
            mArtists = artists;
            mTracks = tracks;
        }

        
        @Override
        public boolean onMove( int oldPosition, int newPosition )
        {
            if (oldPosition == newPosition)
                return true;
            
            int artists = mArtists.getCount();
            int total = getCount();
            
            if( newPosition < artists )
                mArtists.moveToPosition( newPosition );
            else
                mTracks.moveToPosition( total - newPosition );    
            return true;
        }
        @Override
        public String[] getColumnNames()
        {
            return Music.TRACK;
        }

        @Override
        public int getCount()
        {
            int count = -1;
            if( mArtists != null)
                count += mArtists.getCount();
            if( mTracks != null )
                count += mTracks.getCount();
            return count;
        }
        
        public String getMimetype()
        {
            int pos = getPosition();
            int artists = mArtists.getCount();
            if( pos < artists )
                return "artist";
            else
                return "track";
        }

        @Override
        public int getInt( int column )
        {
            int pos = getPosition();
            int artists = mArtists.getCount();
            try {
                if( pos < artists )
                    return mArtists.getInt( column );
                else
                    return mTracks.getInt( column );
            } catch (Exception ex) {
                onChange(true);
                return 0;
            }
        }

        @Override
        public String getString( int column )
        {
            int pos = getPosition();
            int artists = mArtists.getCount();
            try {
                if( pos < artists )
                    return mArtists.getString( column );
                else
                    return mTracks.getString( column );
            } catch (Exception ex) {
                onChange(true);
                return "";
            }
        }

        @Override
        public boolean isNull( int column )
        {
            int pos = getPosition();
            int artists = mArtists.getCount();
            if( pos < artists )
                return mArtists.isNull( column );
            else
                return mTracks.isNull( column );
        }

        @Override
        public double getDouble( int column )
        {
            return 0;
        }

        @Override
        public float getFloat( int column )
        {
            return 0;
        }

        @Override
        public long getLong( int column )
        {
            return 0;
        }

        @Override
        public short getShort( int column )
        {
            return 0;
        }
        
        @Override
        public void deactivate()
        {
            if (mTracks != null)
                mTracks.deactivate();
            if (mArtists != null)
                mArtists.deactivate();
        }
        
    }
    
    static class QueryListAdapter extends SimpleCursorAdapter {
        private QueryBrowser mActivity = null;
        private String mConstraint = null;
        private boolean mConstraintIsValid = false;

        QueryListAdapter(Context context, QueryBrowser currentactivity,
                int layout, SearchCursor cursor, String[] from, int[] to) {
            super(context, layout, cursor, from, to);
            mActivity = currentactivity;
        }

        public void setActivity(QueryBrowser newactivity) {
            mActivity = newactivity;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) 
        {
            if( !(cursor instanceof SearchCursor) )
                return;
            
            SearchCursor c = (SearchCursor) cursor;
            TextView tv1 = (TextView) view.findViewById(R.id.line1);
            TextView tv2 = (TextView) view.findViewById(R.id.line2);
            ImageView iv = (ImageView) view.findViewById(R.id.icon);
            ViewGroup.LayoutParams p = iv.getLayoutParams();
            if (p == null) {
                // seen this happen, not sure why
                DatabaseUtils.dumpCursor(cursor);
                return;
            }
            p.width = ViewGroup.LayoutParams.WRAP_CONTENT;
            p.height = ViewGroup.LayoutParams.WRAP_CONTENT;
            
            String mimetype = c.getMimetype();
            
            if (mimetype == null) {
                mimetype = "audio/";
            }
            if (mimetype.equals("artist")) {
                iv.setImageResource(R.drawable.artist_icon);
                String name = c.getString(Music.ARTIST_MAPPING.ARTIST_NAME);
                String displayname = name;
                boolean isunknown = false;
                if (name == null || name.equals(LockerDb.UNKNOWN_STRING)) {
                    displayname = context.getString(R.string.unknown_artist_name);
                    isunknown = true;
                }
                tv1.setText(displayname);

                int numalbums = c.getInt(Music.ARTIST_MAPPING.ALBUM_COUNT);
                int numsongs = c.getInt(Music.ARTIST_MAPPING.TRACK_COUNT);
                
                String songs_albums = Music.makeAlbumsLabel(context,
                        numalbums, numsongs, isunknown);
                
                tv2.setText(songs_albums);
            
            } else if (mimetype.equals("album")) {
//                iv.setImageResource(R.drawable.albumart_mp_unknown_list);
//                String name = cursor.getString(cursor.getColumnIndexOrThrow(
//                        SearchManager.SUGGEST_COLUMN_TEXT_1));
//                String displayname = name;
//                if (name == null || name.equals(LockerDb.UNKNOWN_STRING)) {
//                    displayname = context.getString(R.string.unknown_album_name);
//                }
//                tv1.setText(displayname);
//                
//                name = cursor.getString(cursor.getColumnIndexOrThrow("data1"));
//                displayname = name;
//                if (name == null || name.equals(LockerDb.UNKNOWN_STRING)) {
//                    displayname = context.getString(R.string.unknown_artist_name);
//                }
//                tv2.setText(displayname);
            } else if (mimetype.equals("track")) {
                iv.setImageResource(R.drawable.song_icon);
                String name = c.getString(Music.TRACK_MAPPING.TITLE);
                tv1.setText(name);
                
                String displayname = cursor.getString( Music.TRACK_MAPPING.ARTIST_NAME );
                if (name == null || name.equals(LockerDb.UNKNOWN_STRING)) {
                    displayname = context.getString(R.string.unknown_artist_name);
                }
                name = cursor.getString( Music.TRACK_MAPPING.ALBUM_NAME );
                if (name == null || name.equals(LockerDb.UNKNOWN_STRING)) {
                    name = context.getString(R.string.unknown_album_name);
                }
                tv2.setText(displayname + " - " + name);
                
            }
        }
        @Override
        public void changeCursor(Cursor cursor) {
            if (cursor != mActivity.mQueryCursor) {
                mActivity.mQueryCursor = (SearchCursor) cursor;
                super.changeCursor(cursor);
            }
        }
    }
    
    private class SearchTask extends AsyncTask<String, Void, Boolean>
    {
        LockerDb.DbSearchResult res;
        @Override
        public void onPreExecute()
        {
            QueryBrowser.this.showDialog( PROGRESS );
        }

        @Override
        public Boolean doInBackground( String... params )
        {
            System.out.println("Searching for " + params[0]);
            try
            {
                res = Music.sDb.search( Music.sDb.new DbSearchQuery( params[0], true, false, true ) );
                
            }
            catch ( Exception e )
            {
                return false;
            }
            return true;
        }

        @Override
        public void onPostExecute( Boolean result )
        {
            dismissDialog( PROGRESS );
            if(res == null)
            {
                System.out.println("No results");
                return;
            }

            
            SearchCursor c = new SearchCursor( res.mArtists, res.mTracks );  
            if( c != null)
                QueryBrowser.this.init(c);
            else
                System.out.println("CURSOR NULL");
        }
    }
}

