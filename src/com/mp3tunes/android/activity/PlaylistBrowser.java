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

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.AsyncQueryHandler;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Adapter;
import android.widget.AlphabetIndexer;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.binaryelysium.mp3tunes.api.Locker;
import com.binaryelysium.mp3tunes.api.Token;
import com.mp3tunes.android.LockerDb;
import com.mp3tunes.android.MP3tunesApplication;
import com.mp3tunes.android.Music;
import com.mp3tunes.android.MusicAlphabetIndexer;
import com.mp3tunes.android.R;
import com.mp3tunes.android.service.Mp3tunesService;

import java.text.Collator;

public class PlaylistBrowser extends ListActivity
    implements View.OnCreateContextMenuListener, Music.Defs
{
    private String mCurrentPlaylistId;
    private String mCurrentPlaylistName;
//    private String mCurrentArtistNameForAlbum;
    private PlaylistListAdapter mAdapter;
    private boolean mAdapterSent;
    private static final int DELETE_PLAYLIST = CHILD_MENU_BASE + 1;
    private static final int EDIT_PLAYLIST = CHILD_MENU_BASE + 2;
    private static final int RENAME_PLAYLIST = CHILD_MENU_BASE + 3;
    private static final int CHANGE_WEEKS = CHILD_MENU_BASE + 4;
    private static final long RECENTLY_ADDED_PLAYLIST = -1;
    private static final long ALL_SONGS_PLAYLIST = -2;
    private static final long PODCASTS_PLAYLIST = -3;
    private AsyncTask mPlaylistTask;
    private AsyncTask mTracksTask;

    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle)
    {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        Music.bindToService(this);
        if(! Music.connectToDb( this ) )
            finish(); //TODO show error


        setContentView(R.layout.media_picker_activity);
        ListView lv = getListView();
        lv.setFastScrollEnabled(true);
        lv.setOnCreateContextMenuListener(this);
        lv.setTextFilterEnabled(true);

        mAdapter = (PlaylistListAdapter) getLastNonConfigurationInstance();
        if (mAdapter == null) {
            //Log.i("@@@", "starting query");
            mAdapter = new PlaylistListAdapter(
                    getApplication(),
                    this,
                    R.layout.track_list_item,
                    mPlaylistCursor,
                    new String[] {},
                    new int[] {});
            setListAdapter(mAdapter);
            setTitle(R.string.title_working_playlists);
            mPlaylistTask = new FetchPlaylistsTask().execute();
        } else {
            mAdapter.setActivity(this);
            setListAdapter(mAdapter);
            mPlaylistCursor = mAdapter.getCursor();
            if (mPlaylistCursor != null) {
                init(mPlaylistCursor);
            } else {
                setTitle(R.string.title_working_playlists);
                mPlaylistTask = new FetchPlaylistsTask().execute();
            }
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        mAdapterSent = true;
        return mAdapter;
    }
    
    @Override
    public void onSaveInstanceState(Bundle outcicle) 
    {
        if( mTracksTask != null && mTracksTask.getStatus() == AsyncTask.Status.RUNNING)
            mTracksTask.cancel( true );
        if( mPlaylistTask != null && mPlaylistTask.getStatus() == AsyncTask.Status.RUNNING)
            mPlaylistTask.cancel( true );
        
        // need to store the selected item so we don't lose it in case
        // of an orientation switch. Otherwise we could lose it while
        // in the middle of specifying a playlist to add the item to.
        outcicle.putString("selectedalbum", mCurrentPlaylistId);
        outcicle.putString("artist", mArtistId);
        super.onSaveInstanceState(outcicle);
    }

    @Override
    public void onDestroy() 
    {
        if( mTracksTask != null && mTracksTask.getStatus() == AsyncTask.Status.RUNNING)
            mTracksTask.cancel( true );
        if( mPlaylistTask != null && mPlaylistTask.getStatus() == AsyncTask.Status.RUNNING)
            mPlaylistTask.cancel( true );
        Music.unbindFromService(this);
        Music.unconnectFromDb( this );
        if (!mAdapterSent) {
            Cursor c = mAdapter.getCursor();
            if (c != null) {
                c.close();
            }
        }
        super.onDestroy();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        IntentFilter f = new IntentFilter();
        f.addAction(Mp3tunesService.META_CHANGED);
        f.addAction(Mp3tunesService.QUEUE_CHANGED);
        registerReceiver(mTrackListListener, f);
        mTrackListListener.onReceive(null, null);
    }

    private BroadcastReceiver mTrackListListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            getListView().invalidateViews();
        }
    };

    @Override
    public void onPause() {
        unregisterReceiver(mTrackListListener);
        super.onPause();
    }

    public void init(Cursor c) {

        mAdapter.changeCursor(c); // also sets mPlaylistCursor

        if (mPlaylistCursor == null) {
//            Music.displayDatabaseError(this); //TODO display error
            closeContextMenu();
            return;
        }
        
//        Music.hideDatabaseError(this);//TODO display error
        setTitle();
    }

    private void setTitle() {
        setTitle(R.string.title_playlists);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfoIn) {        
        AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfoIn;

        menu.add(0, PLAY_SELECTION, 0, R.string.menu_play_selection);

        if (mi.id >= 0  ) {
            menu.add(0, DELETE_PLAYLIST, 0, R.string.menu_delete);
        }

        if (mi.id == RECENTLY_ADDED_PLAYLIST) {
            menu.add(0, EDIT_PLAYLIST, 0, R.string.menu_edit_playlist);
        }

        if (mi.id >= 0) {
            menu.add(0, RENAME_PLAYLIST, 0, R.string.menu_rename_playlist);
        }

        mPlaylistCursor.moveToPosition(mi.position);
        mCurrentPlaylistName = mPlaylistCursor.getString(Music.PLAYLIST_MAPPING.PLAYLIST_NAME);
        mCurrentPlaylistId = mPlaylistCursor.getString(Music.PLAYLIST_MAPPING.ID);
        menu.setHeaderTitle(mCurrentPlaylistName);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case PLAY_SELECTION: {
                // play the selected playlist
                mTracksTask = new FetchTracksTask().execute( Integer.valueOf( mCurrentPlaylistId ), PLAY_SELECTION );
                return true;
            }

            case DELETE_PLAYLIST: {
                return true;
            }

            case EDIT_PLAYLIST: {
                return true;
            }

            case RENAME_PLAYLIST: {
                return true;
            }
        }
        return super.onContextItemSelected(item);
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        
        Cursor c = (Cursor) getListAdapter().getItem( position );
        String playlist = c.getString(Music.PLAYLIST_MAPPING.ID);
        String playlist_name = c.getString(Music.PLAYLIST_MAPPING.PLAYLIST_NAME);
        Intent intent = new Intent(Intent.ACTION_EDIT);
        intent.setDataAndType(Uri.EMPTY, "vnd.mp3tunes.android.dir/track");
        intent.putExtra("playlist", Long.valueOf(playlist).toString());
        intent.putExtra("playlist_name", playlist_name);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        menu.add(0, GOTO_START, 0, R.string.menu_home).setIcon(R.drawable.ic_mp_song_playback);
        menu.add(0, GOTO_PLAYBACK, 0, R.string.menu_player).setIcon(R.drawable.ic_mp_song_playback);
        menu.add(0, SHUFFLE_ALL, 0, R.string.menu_shuffle_all).setIcon(R.drawable.ic_mp_song_playback);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(GOTO_PLAYBACK).setVisible(Music.isMusicPlaying());
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        Cursor cursor;
        switch (item.getItemId()) {
            case GOTO_START:
                intent = new Intent();
                intent.setClass(this, LockerList.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;

            case GOTO_PLAYBACK:
                intent = new Intent("com.mp3tunes.android.PLAYER");
                startActivity(intent);
                return true;

            case SHUFFLE_ALL:
                //TODO shuffle all
//                cursor = Music.query(this, MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
//                        new String [] { MediaStore.Audio.Media._ID},
//                        MediaStore.Audio.Media.IS_MUSIC + "=1", null,
//                        MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
//                if (cursor != null) {
//                    Music.shuffleAll(this, cursor);
//                    cursor.close();
//                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    static class PlaylistListAdapter extends SimpleCursorAdapter implements SectionIndexer {
        
        private final Drawable mNowPlayingOverlay;
        private final BitmapDrawable mDefaultPlaylistIcon;
        private int mPlaylistNameIdx;
        private int mPlaylistIdIdx;
        private int mNumSongsIdx;
        private final Resources mResources;
        private final String mUnknownPlaylist;
        private AlphabetIndexer mIndexer;
        private PlaylistBrowser mActivity;
        private String mConstraint = null;
        private boolean mConstraintIsValid = false;
        
        class ViewHolder {
            TextView line1;
            TextView line2;
            TextView duration;
            ImageView play_indicator;
            ImageView icon;
        }

        PlaylistListAdapter(Context context, PlaylistBrowser currentactivity,
                int layout, Cursor cursor, String[] from, int[] to) {
            super(context, layout, cursor, from, to);

            mActivity = currentactivity;
            
//            mUnknownAlbum = context.getString(R.string.unknown_album_name);
            mUnknownPlaylist = context.getString(R.string.unknown_playlist_name);
//            mAlbumSongSeparator = context.getString(R.string.albumsongseparator);

            Resources r = context.getResources();
            mNowPlayingOverlay = r.getDrawable(R.drawable.indicator_ic_mp_playing_list);

            Bitmap b = BitmapFactory.decodeResource(r, R.drawable.playlist_icon);
            mDefaultPlaylistIcon = new BitmapDrawable(b);
            // no filter or dither, it's a lot faster and we can't tell the difference
            mDefaultPlaylistIcon.setFilterBitmap(false);
            mDefaultPlaylistIcon.setDither(false);
            getColumnIndices(cursor);
            mResources = context.getResources();
        }

        private void getColumnIndices(Cursor cursor) {
            if (cursor != null) {
//                mAlbumIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM);
                mPlaylistNameIdx = Music.PLAYLIST_MAPPING.PLAYLIST_NAME;
                mPlaylistIdIdx  = Music.PLAYLIST_MAPPING.ID;
                mNumSongsIdx = Music.PLAYLIST_MAPPING.FILE_COUNT;
                
                if (mIndexer != null) {
                    mIndexer.setCursor(cursor);
                } else {
                    mIndexer = new MusicAlphabetIndexer(cursor, mPlaylistNameIdx, mResources.getString(
                            R.string.alphabet));
                }
            }
        }
        
        public void setActivity(PlaylistBrowser newactivity) {
            mActivity = newactivity;
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
           View v = super.newView(context, cursor, parent);
           ViewHolder vh = new ViewHolder();
           vh.line1 = (TextView) v.findViewById(R.id.line1);
           vh.line2 = (TextView) v.findViewById(R.id.line2);
           vh.duration = (TextView) v.findViewById(R.id.duration);
           vh.play_indicator = (ImageView) v.findViewById(R.id.play_indicator);
           vh.icon = (ImageView) v.findViewById(R.id.icon);
           vh.icon.setBackgroundDrawable(mDefaultPlaylistIcon);
           vh.icon.setPadding(0, 0, 1, 0);
           v.setTag(vh);
           return v;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            
            ViewHolder vh = (ViewHolder) view.getTag();

            String name = cursor.getString(mPlaylistNameIdx);
            String displayname = name;
            boolean unknown = name == null || name.equals(LockerDb.UNKNOWN_STRING); 
            if (unknown) {
                displayname = mUnknownPlaylist;
            }
            vh.line1.setText(displayname);
            
            int numalbums = cursor.getInt(mNumSongsIdx);
            int numsongs = cursor.getInt(mNumSongsIdx);
            displayname = Music.makeAlbumsLabel( context, numalbums, numsongs, true );
            vh.line2.setText(displayname);
        }
        
        @Override
        public void changeCursor(Cursor cursor) {
            if (cursor != mActivity.mPlaylistCursor) {
                mActivity.mPlaylistCursor = cursor;
                getColumnIndices(cursor);
                super.changeCursor(cursor);
            }
        }
        
        @Override
        public Cursor runQueryOnBackgroundThread(CharSequence constraint) {
//            String s = constraint.toString();
//            if (mConstraintIsValid && (
//                    (s == null && mConstraint == null) ||
//                    (s != null && s.equals(mConstraint)))) {
//                return getCursor();
//            }
//            Cursor c = mActivity.getArtistCursor(null, s);
//            mConstraint = s;
//            mConstraintIsValid = true;
//            return c;
            return null;
        }
        
        public Object[] getSections() {
            return mIndexer.getSections();
        }
        
        public int getPositionForSection(int section) {
            return mIndexer.getPositionForSection(section);
        }
        
        public int getSectionForPosition(int position) {
            return 0;
        }
    }

    private Cursor mPlaylistCursor;
    private String mArtistId;
    
    private class FetchPlaylistsTask extends AsyncTask<Void, Void, Boolean>
    {
        String[] tokens= null;
        Cursor cursor;
        @Override
        public void onPreExecute()
        {
            Music.setSpinnerState(PlaylistBrowser.this, true);
            System.out.println("Fetching playlists tasks");
        }

        @Override
        public Boolean doInBackground( Void... params )
        {
            try
            {
                if( Music.sDb!= null)
                cursor = Music.sDb.getTableList( Music.Meta.PLAYLIST );
                else
                    System.out.println("database null");
//                Token[] t = Music.sDb.getTokens( Music.Meta.ARTIST );
//                tokens = LockerDb.tokensToString( t );
            }
            catch ( Exception e )
            {
                System.out.println("Fetching playlists failed");
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        public void onPostExecute( Boolean result )
        {
            Music.setSpinnerState(PlaylistBrowser.this, false);
            if( cursor != null)
                PlaylistBrowser.this.init(cursor);
            else
                System.out.println("CURSOR NULL");
        }
    }
    
    private class FetchTracksTask extends AsyncTask<Integer, Void, Boolean>
    {
        String[] tokens= null;
        Cursor cursor;
        int action = -1;
        @Override
        public void onPreExecute()
        {
            Music.setSpinnerState(PlaylistBrowser.this, true);
        }

        @Override
        public Boolean doInBackground( Integer... params )
        {
            if(params.length <= 1)
                return false;
            int playlist_id = params[0];
            action = params[1];
            try
            {
                if( Music.sDb!= null)
                    cursor = Music.sDb.getTracksForPlaylist( playlist_id );
                else
                    System.out.println("database null");
            }
            catch ( Exception e )
            {
                System.out.println("Fetching tracks failed");
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        public void onPostExecute( Boolean result )
        {
            Music.setSpinnerState(PlaylistBrowser.this, false);
            if( cursor != null && result) {
                switch ( action )
                {
                case PLAY_SELECTION:
                    Music.playAll(PlaylistBrowser.this, cursor, 0);
                    break;
                }
            } else
                System.out.println("CURSOR NULL");
        }
    }
}

