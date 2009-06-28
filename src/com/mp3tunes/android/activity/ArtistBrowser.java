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
import android.content.BroadcastReceiver;
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
import android.provider.MediaStore;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AlphabetIndexer;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SectionIndexer;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.AdapterView.AdapterContextMenuInfo;

import com.mp3tunes.android.LockerDb;
import com.mp3tunes.android.Music;
import com.mp3tunes.android.MusicAlphabetIndexer;
import com.mp3tunes.android.R;
import com.mp3tunes.android.service.Mp3tunesService;

public class ArtistBrowser extends ListActivity
    implements View.OnCreateContextMenuListener, Music.Defs
{
    private String mCurrentArtistId;
    private String mCurrentArtistName;
//    private String mCurrentArtistNameForAlbum;
    private ArtistListAdapter mAdapter;
    private boolean mAdapterSent;
    private final static int SEARCH = CHILD_MENU_BASE;
    private final static int PROGRESS = CHILD_MENU_BASE + 1;
    private AlertDialog mProgDialog;
    private AsyncTask mArtistTask;
    private AsyncTask mTracksTask;

    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle)
    {
        if (icicle != null) {
            mCurrentArtistId = icicle.getString("selectedalbum");
            mArtistId = icicle.getString("artist");
        } else {
//            mArtistId = getIntent().getStringExtra("artist");
        }
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        Music.bindToService(this);
        if(! Music.connectToDb( this ) )
            finish(); //TODO show error
        
        AlertDialog.Builder builder;
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.progress_dialog,
                                       (ViewGroup) findViewById(R.id.layout_root));

        TextView text = (TextView) layout.findViewById(R.id.progress_text);
        text.setText(R.string.loading_artists);

        builder = new AlertDialog.Builder(this);
        builder.setView(layout);
        mProgDialog = builder.create();

        setContentView(R.layout.media_picker_activity);
        ListView lv = getListView();
        lv.setFastScrollEnabled(true);
        lv.setOnCreateContextMenuListener(this);
        lv.setTextFilterEnabled(true);

        mAdapter = (ArtistListAdapter) getLastNonConfigurationInstance();
        if (mAdapter == null) {
            //Log.i("@@@", "starting query");
            mAdapter = new ArtistListAdapter(
                    getApplication(),
                    this,
                    R.layout.track_list_item,
                    mArtistCursor,
                    new String[] {},
                    new int[] {});
            setListAdapter(mAdapter);
            setTitle(R.string.title_working_artists);
            mArtistTask = new FetchArtistsTask().execute();
        } else {
            mAdapter.setActivity(this);
            setListAdapter(mAdapter);
            mArtistCursor = mAdapter.getCursor();
            if (mArtistCursor != null) {
                init(mArtistCursor);
            } else {
                mArtistTask = new FetchArtistsTask().execute();
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
        System.out.println("On save instance state");
        if( mArtistTask != null && mArtistTask.getStatus() == AsyncTask.Status.RUNNING)
            mArtistTask.cancel( true );
        if( mTracksTask != null && mTracksTask.getStatus() == AsyncTask.Status.RUNNING)
            mTracksTask.cancel( true );
        // need to store the selected item so we don't lose it in case
        // of an orientation switch. Otherwise we could lose it while
        // in the middle of specifying a playlist to add the item to.
        outcicle.putString("selectedalbum", mCurrentArtistId);
        outcicle.putString("artist", mArtistId);
        super.onSaveInstanceState(outcicle);
    }

    @Override
    public void onDestroy() 
    {
        if( mArtistTask != null && mArtistTask.getStatus() == AsyncTask.Status.RUNNING)
            mArtistTask.cancel( true );
        if( mTracksTask != null && mTracksTask.getStatus() == AsyncTask.Status.RUNNING)
            mTracksTask.cancel( true );
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

        mAdapter.changeCursor(c); // also sets mArtistCursor

        if (mArtistCursor == null) {
//            Music.displayDatabaseError(this); //TODO display error
            closeContextMenu();
            return;
        }
        
//        Music.hideDatabaseError(this);//TODO display error
        setTitle();
    }

    private void setTitle() {
            setTitle(R.string.title_artists);
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
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfoIn) {
        menu.add(0, QUEUE, 0, R.string.menu_queue);
        menu.add(0, PLAY_SELECTION, 0, R.string.menu_play_selection);
//        SubMenu sub = menu.addSubMenu(0, ADD_TO_PLAYLIST, 0, R.string.menu_add_to_playlist);
//        Music.makePlaylistMenu(this, sub); // TODO make menu
//        menu.add(0, DELETE_ITEM, 0, R.string.delete_item);
        menu.add(0, SEARCH, 0, R.string.search);

        AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfoIn;
        mArtistCursor.moveToPosition(mi.position);
        mCurrentArtistId = mArtistCursor.getString(Music.ARTIST_MAPPING.ID);
        mCurrentArtistName = mArtistCursor.getString(Music.ARTIST_MAPPING.ARTIST_NAME);
//        mCurrentArtistNameForAlbum = mArtistCursor.getString(
//                mArtistCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST));
        menu.setHeaderTitle(mCurrentArtistName);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case PLAY_SELECTION: {
                // play the selected artist
                mTracksTask = new FetchTracksTask().execute( Integer.valueOf( mCurrentArtistId ), PLAY_SELECTION );
                return true;
            }

            case QUEUE: {
                mTracksTask = new FetchTracksTask().execute( Integer.valueOf( mCurrentArtistId ), QUEUE );
                return true;
            }

            case NEW_PLAYLIST: {
             // TODO  new playlist?
                Intent intent = new Intent();
//                intent.setClass(this, CreatePlaylist.class);
                startActivityForResult(intent, NEW_PLAYLIST);
                return true;
            }

            case PLAYLIST_SELECTED: {
             // TODO playlist selected
//                int [] list = Music.getSongListForAlbum(this, Integer.parseInt(mCurrentArtistId));
                int playlist = item.getIntent().getIntExtra("playlist", 0);
//                Music.addToPlaylist(this, list, playlist);
                return true;
            }
            case DELETE_ITEM: {
                // TODO Delete
//                int [] list = Music.getSongListForAlbum(this, Integer.parseInt(mCurrentArtistId));
//                String f = getString(R.string.delete_album_desc); 
//                String desc = String.format(f, mCurrentArtistName);
//                Bundle b = new Bundle();
//                b.putString("description", desc);
////                b.putIntArray("items", list);
//                Intent intent = new Intent();
////                intent.setClass(this, DeleteItems.class);
//                intent.putExtras(b);
//                startActivityForResult(intent, -1);
                return true;
            }
            case SEARCH:
             // TODO search
//                doSearch();
                return true;

        }
        return super.onContextItemSelected(item);
    }

    void doSearch() {
        CharSequence title = null;
        String query = null;
        
        Intent i = new Intent();
        i.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
        
        title = mCurrentArtistName;
//        query = mCurrentArtistNameForAlbum + " " + mCurrentArtistName;
//        i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, mCurrentArtistNameForAlbum);
        i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, mCurrentArtistName);
        i.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE);
//        title = getString(R.string.mediasearch, title);
        i.putExtra(SearchManager.QUERY, query);

        startActivity(Intent.createChooser(i, title));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case SCAN_DONE:
                if (resultCode == RESULT_CANCELED) {
                    finish();
                } else {
//                    getAlbumCursor(mAdapter.getQueryHandler(), null);
                }
                break;

            case NEW_PLAYLIST:
                if (resultCode == RESULT_OK) {
                    Uri uri = intent.getData();
                    if (uri != null) {
//                        int [] list = Music.getSongListForAlbum(this, Integer.parseInt(mCurrentArtistId));
//                        Music.addToPlaylist(this, list, Integer.parseInt(uri.getLastPathSegment()));
                    }
                }
                break;
        }
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        Cursor c = (Cursor) getListAdapter().getItem( position );
        String artist = c.getString(Music.ARTIST_MAPPING.ID);
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(Uri.EMPTY, "vnd.mp3tunes.android.dir/album");
        intent.putExtra("artist", artist);
        startActivity(intent);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.artists, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.findItem(R.id.menu_opt_player).setVisible( Music.isMusicPlaying() );
        menu.findItem(R.id.menu_opt_playall).setVisible( false );
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        Cursor cursor;
        switch (item.getItemId()) {
            case R.id.menu_opt_home:
                intent = new Intent();
                intent.setClass(this, LockerList.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                return true;

            case R.id.menu_opt_player:
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
    
    static class ArtistListAdapter extends SimpleCursorAdapter implements SectionIndexer {
        
        private final Drawable mNowPlayingOverlay;
        private final BitmapDrawable mDefaultArtistIcon;
        private int mArtistNameIdx;
        private int mArtistIdIdx;
        private int mNumAlbumsIdx;
        private final Resources mResources;
        private final String mUnknownArtist;
        private AlphabetIndexer mIndexer;
        private ArtistBrowser mActivity;
        private String mConstraint = null;
        private boolean mConstraintIsValid = false;
        
        class ViewHolder {
            TextView line1;
            TextView line2;
            TextView duration;
            ImageView play_indicator;
            ImageView icon;
        }

        ArtistListAdapter(Context context, ArtistBrowser currentactivity,
                int layout, Cursor cursor, String[] from, int[] to) {
            super(context, layout, cursor, from, to);

            mActivity = currentactivity;
            
//            mUnknownAlbum = context.getString(R.string.unknown_album_name);
            mUnknownArtist = context.getString(R.string.unknown_artist_name);
//            mAlbumSongSeparator = context.getString(R.string.albumsongseparator);

            Resources r = context.getResources();
            mNowPlayingOverlay = r.getDrawable(R.drawable.indicator_ic_mp_playing_list);

            Bitmap b = BitmapFactory.decodeResource(r, R.drawable.artist_icon);
            mDefaultArtistIcon = new BitmapDrawable(b);
            // no filter or dither, it's a lot faster and we can't tell the difference
            mDefaultArtistIcon.setFilterBitmap(false);
            mDefaultArtistIcon.setDither(false);
            getColumnIndices(cursor);
            mResources = context.getResources();
        }

        private void getColumnIndices(Cursor cursor) {
            if (cursor != null) {
//                mAlbumIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM);
                mArtistNameIdx = Music.ARTIST_MAPPING.ARTIST_NAME;
                mArtistIdIdx  = Music.ARTIST_MAPPING.ID;
                mNumAlbumsIdx = Music.ARTIST_MAPPING.ALBUM_COUNT;
//                mNumSongsIdx = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS);
//                mAlbumArtIndex = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART);
                
                if (mIndexer != null) {
                    mIndexer.setCursor(cursor);
                } else {
//                    mIndexer = new MusicAlphabetIndexer(cursor, mAlbumIdx, mResources.getString(
//                            R.string.alphabet));
                    mIndexer = new MusicAlphabetIndexer(cursor, mArtistNameIdx, mResources.getString(
                            R.string.alphabet));
                }
            }
        }
        
        public void setActivity(ArtistBrowser newactivity) {
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
           vh.icon.setBackgroundDrawable(mDefaultArtistIcon);
           vh.icon.setPadding(0, 0, 1, 0);
           v.setTag(vh);
           return v;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            
            ViewHolder vh = (ViewHolder) view.getTag();

            String name = cursor.getString(mArtistNameIdx);
            String displayname = name;
            boolean unknown = name == null || name.equals(LockerDb.UNKNOWN_STRING); 
            if (unknown) {
                displayname = mUnknownArtist;
            }
            vh.line1.setText(displayname);
            
            int numalbums = cursor.getInt(mNumAlbumsIdx);
            int numsongs = cursor.getInt(mNumAlbumsIdx);
            displayname = Music.makeAlbumsLabel( context, numalbums, numsongs, unknown );
            vh.line2.setText(displayname);

            ImageView iv = vh.icon;
            // We don't actually need the path to the thumbnail file,
            // we just use it to see if there is album art or not
//            String art = cursor.getString(mAlbumArtIndex);
//            if (unknown || art == null || art.length() == 0) {
                iv.setImageDrawable(null);
//            } else {
//                int artIndex = cursor.getInt(0);
//                Drawable d = Music.getCachedArtwork(context, artIndex, mDefaultArtistIcon);
//                iv.setImageDrawable(d);
//            }
            
            int currentartistid = Music.getCurrentArtistId();
            int aid = cursor.getInt(mArtistIdIdx);
            iv = vh.play_indicator;
            if (currentartistid == aid) {
                iv.setImageDrawable(mNowPlayingOverlay);
            } else {
                iv.setImageDrawable(null);
            }
        }
        
        @Override
        public void changeCursor(Cursor cursor) {
            if (cursor != mActivity.mArtistCursor) {
                mActivity.mArtistCursor = cursor;
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

    private Cursor mArtistCursor;
    private String mArtistId;
    
    private class FetchArtistsTask extends AsyncTask<Void, Void, Boolean>
    {
        String[] tokens= null;
        Cursor cursor;
        @Override
        public void onPreExecute()
        {
            ArtistBrowser.this.showDialog( PROGRESS );
            Music.setSpinnerState(ArtistBrowser.this, true);
            System.out.println("Fetching artists tasks");
        }

        @Override
        public Boolean doInBackground( Void... params )
        {
            try
            {
                if( Music.sDb!= null)
                cursor = Music.sDb.getTableList( Music.Meta.ARTIST );
                else
                    System.out.println("database null");
//                Token[] t = Music.sDb.getTokens( Music.Meta.ARTIST );
//                tokens = LockerDb.tokensToString( t );
            }
            catch ( Exception e )
            {
                System.out.println("Fetching artists failed");
                e.printStackTrace();
                return false;
            }
            return true;
        }

        @Override
        public void onPostExecute( Boolean result )
        {
            dismissDialog( PROGRESS );
            Music.setSpinnerState(ArtistBrowser.this, false);
            if( cursor != null)
                ArtistBrowser.this.init(cursor);
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
            Music.setSpinnerState(ArtistBrowser.this, true);
        }

        @Override
        public Boolean doInBackground( Integer... params )
        {
            if(params.length <= 1)
                return false;
            int artist_id = params[0];
            action = params[1];
            try
            {
                if( Music.sDb!= null)
                    cursor = Music.sDb.getTracksForArtist( artist_id );
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
            Music.setSpinnerState(ArtistBrowser.this, false);
            if( cursor != null && result) {
                switch ( action )
                {
                case PLAY_SELECTION:
                    Music.playAll(ArtistBrowser.this, cursor, 0);
                    break;
                case QUEUE:
                    int[] ids = Music.getSongListForCursor( cursor );
                    System.out.println("queue got " +ids.length);
                    Music.sDb.insertQueueItems( ids );
                    break;
                }
            } else
                System.out.println("CURSOR NULL");
        }
    }
}

