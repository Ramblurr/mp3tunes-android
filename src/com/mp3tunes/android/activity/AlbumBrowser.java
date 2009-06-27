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

import com.binaryelysium.mp3tunes.api.Token;
import com.mp3tunes.android.LockerDb;
import com.mp3tunes.android.Music;
import com.mp3tunes.android.MusicAlphabetIndexer;
import com.mp3tunes.android.R;
import com.mp3tunes.android.service.Mp3tunesService;
import com.mp3tunes.android.util.ImageCache;
import com.mp3tunes.android.util.ImageDownloader;
import com.mp3tunes.android.util.ImageDownloaderListener;

import java.text.Collator;

public class AlbumBrowser extends ListActivity
    implements View.OnCreateContextMenuListener, Music.Defs
{
    private String mCurrentAlbumId;
    private String mCurrentAlbumName;
    private String mCurrentArtistNameForAlbum;
    private Cursor mAlbumCursor;
    private String mArtistId;
    private AlbumListAdapter mAdapter;
    private AsyncTask mArtFetcher;
    private boolean mAdapterSent;
    private final static int SEARCH = CHILD_MENU_BASE;
    private final static int PROGRESS = CHILD_MENU_BASE + 1;
    
    private AlertDialog mProgDialog;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle)
    {
        if (icicle != null) {
            mCurrentAlbumId = icicle.getString("selectedalbum");
            mArtistId = icicle.getString("artist");
        } else {
            mArtistId = getIntent().getStringExtra("artist");
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
        text.setText(R.string.loading_albums);

        builder = new AlertDialog.Builder(this);
        builder.setView(layout);
        mProgDialog = builder.create();

        setContentView(R.layout.media_picker_activity);
        ListView lv = getListView();
        lv.setFastScrollEnabled(true);
        lv.setOnCreateContextMenuListener(this);
        lv.setTextFilterEnabled(true);

        mAdapter = (AlbumListAdapter) getLastNonConfigurationInstance();
        if (mAdapter == null) {
            //Log.i("@@@", "starting query");
            mAdapter = new AlbumListAdapter(
                    getApplication(),
                    this,
                    R.layout.track_list_item,
                    mAlbumCursor,
                    new String[] {},
                    new int[] {});
            setListAdapter(mAdapter);
            setTitle(R.string.title_working_albums);
            new FetchAlbumsTask().execute();
        } else {
            mAdapter.setActivity(this);
            setListAdapter(mAdapter);
            mAlbumCursor = mAdapter.getCursor();
            if (mAlbumCursor != null) {
                init(mAlbumCursor);
            } else {
                new FetchAlbumsTask().execute();
            }
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        mAdapterSent = true;
        return mAdapter;
    }
    
    @Override
    public void onSaveInstanceState(Bundle outcicle) {
        if( mArtFetcher != null && mArtFetcher.getStatus() == AsyncTask.Status.RUNNING)
        {
            mArtFetcher.cancel(true);
            outcicle.putBoolean( "artfetch_in_progress", true );
            mArtFetcher = null;
        }
        // need to store the selected item so we don't lose it in case
        // of an orientation switch. Otherwise we could lose it while
        // in the middle of specifying a playlist to add the item to.
        outcicle.putString("selectedalbum", mCurrentAlbumId);
        outcicle.putString("artist", mArtistId);
        super.onSaveInstanceState(outcicle);
    }

    @Override
    public void onDestroy() {
        if( mArtFetcher != null && mArtFetcher.getStatus() == AsyncTask.Status.RUNNING)
        {
            mArtFetcher.cancel(true);
            mArtFetcher = null;
        }
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

    public void init(Cursor c) {

        mAdapter.changeCursor(c); // also sets mAlbumCursor

        if (mAlbumCursor == null) {
//            Music.displayDatabaseError(this); //TODO display error
            closeContextMenu();
            return;
        }
        
//        Music.hideDatabaseError(this);//TODO display error
        setTitle();
    }

    private void setTitle() {
        CharSequence fancyName = "";
        if (mAlbumCursor != null && mAlbumCursor.getCount() > 0) {
            mAlbumCursor.moveToFirst();
            fancyName = mAlbumCursor.getString(Music.ALBUM_MAPPING.ARTIST_NAME);
            if (fancyName == null || fancyName.equals(LockerDb.UNKNOWN_STRING))
                fancyName = getText(R.string.unknown_artist_name);
        }

        if (mArtistId != null && fancyName != null)
            setTitle(fancyName);
        else
            setTitle(R.string.title_albums);
    }
    
    @Override
    public void onCreateContextMenu(ContextMenu menu, View view, ContextMenuInfo menuInfoIn) {
        menu.add(0, QUEUE, 0, R.string.menu_play_selection);
        menu.add(0, PLAY_SELECTION, 0, R.string.menu_play_selection);
        
//        SubMenu sub = menu.addSubMenu(0, ADD_TO_PLAYLIST, 0, R.string.menu_add_to_playlist);
//        Music.makePlaylistMenu(this, sub); // TODO make menu
//        menu.add(0, DELETE_ITEM, 0, R.string.delete_item);
        menu.add(0, SEARCH, 0, R.string.search);

        AdapterContextMenuInfo mi = (AdapterContextMenuInfo) menuInfoIn;
        mAlbumCursor.moveToPosition(mi.position);
        mCurrentAlbumId = mAlbumCursor.getString(mAlbumCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID));
        mCurrentAlbumName = mAlbumCursor.getString(mAlbumCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM));
//        mCurrentArtistNameForAlbum = mAlbumCursor.getString(
//                mAlbumCursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST));
        menu.setHeaderTitle(mCurrentAlbumName);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case PLAY_SELECTION: {
                // play the selected album
                new FetchTracksTask().execute( Integer.valueOf( mCurrentAlbumId ), PLAY_SELECTION );
                return true;
            }

            case QUEUE: {
                new FetchTracksTask().execute( Integer.valueOf( mCurrentAlbumId ), QUEUE );
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
//                int [] list = Music.getSongListForAlbum(this, Integer.parseInt(mCurrentAlbumId));
                int playlist = item.getIntent().getIntExtra("playlist", 0);
//                Music.addToPlaylist(this, list, playlist);
                return true;
            }
            case DELETE_ITEM: {
                // TODO Delete
//                int [] list = Music.getSongListForAlbum(this, Integer.parseInt(mCurrentAlbumId));
//                String f = getString(R.string.delete_album_desc); 
//                String desc = String.format(f, mCurrentAlbumName);
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
        
        title = mCurrentAlbumName;
//        query = mCurrentArtistNameForAlbum + " " + mCurrentAlbumName;
//        i.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, mCurrentArtistNameForAlbum);
        i.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, mCurrentAlbumName);
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
//                        int [] list = Music.getSongListForAlbum(this, Integer.parseInt(mCurrentAlbumId));
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
        String artist = c.getString(Music.ALBUM_MAPPING.ARTIST_ID);
        String album = c.getString(Music.ARTIST_MAPPING.ID);
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(Uri.EMPTY, "vnd.mp3tunes.android.dir/track");
        intent.putExtra("album", album);
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

    private Cursor getArtistCursor(AsyncQueryHandler async, String filter) {
        StringBuilder where = new StringBuilder();
        where.append(MediaStore.Audio.Albums.ALBUM + " != ''");
        
        // Add in the filtering constraints
        String [] keywords = null;
        if (filter != null) {
            String [] searchWords = filter.split(" ");
            keywords = new String[searchWords.length];
            Collator col = Collator.getInstance();
            col.setStrength(Collator.PRIMARY);
            for (int i = 0; i < searchWords.length; i++) {
                keywords[i] = '%' + MediaStore.Audio.keyFor(searchWords[i]) + '%';
            }
            for (int i = 0; i < searchWords.length; i++) {
                where.append(" AND ");
                where.append(MediaStore.Audio.Media.ARTIST_KEY + "||");
                where.append(MediaStore.Audio.Media.ALBUM_KEY + " LIKE ?");
            }
        }

        String whereclause = where.toString();  
            
        String[] cols = new String[] {
                MediaStore.Audio.Albums._ID,
                MediaStore.Audio.Albums.ALBUM,
                MediaStore.Audio.Albums.ALBUM_KEY,
                MediaStore.Audio.Albums.ARTIST,
                MediaStore.Audio.Albums.NUMBER_OF_SONGS,
                MediaStore.Audio.Albums.ALBUM_ART
        };
        Cursor ret = null;
        if (mArtistId != null) {
            if (async != null) {
                async.startQuery(0, null,
                        MediaStore.Audio.Artists.Albums.getContentUri("external",
                                Long.valueOf(mArtistId)),
                        cols, whereclause, keywords, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
            } else {
//                ret = Music.query(this,
//                        MediaStore.Audio.Artists.Albums.getContentUri("external",
//                                Long.valueOf(mArtistId)),
//                        cols, whereclause, keywords, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
            }
        } else {
            if (async != null) {
                async.startQuery(0, null,
                        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                        cols, whereclause, keywords, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
            } else {
//                ret = Music.query(this, MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
//                        cols, whereclause, keywords, MediaStore.Audio.Albums.DEFAULT_SORT_ORDER);
            }
        }
        return ret;
    }
    
    static class AlbumListAdapter extends SimpleCursorAdapter implements SectionIndexer, ImageDownloaderListener{
        
        private final Drawable mNowPlayingOverlay;
        private final BitmapDrawable mDefaultAlbumIcon;
        private int mAlbumNameIdx;
        private int mAlbumIdIdx;
        private int mArtistNameIdx;
        private int mArtistIdIdx;
        private int mNumAlbumsIdx;
        private int mNumSongsIdx;
//        private int mAlbumArtIndex;
        private final Resources mResources;
        private final StringBuilder mStringBuilder = new StringBuilder();
        private final String mUnknownArtist;
        private final String mUnknownAlbum;
//        private final String mAlbumSongSeparator;
        private final Object[] mFormatArgs = new Object[1];
        private AlphabetIndexer mIndexer;
        private AlbumBrowser mActivity;
        private AsyncQueryHandler mQueryHandler;
        private String mConstraint = null;
        private boolean mConstraintIsValid = false;
        
        protected transient ImageCache mImageCache;
        protected transient ImageDownloader mImageDownloader;
        
        class ViewHolder {
            TextView line1;
            TextView line2;
            TextView duration;
            ImageView play_indicator;
            ImageView icon;
        }

        class QueryHandler extends AsyncQueryHandler {
            QueryHandler(ContentResolver res) {
                super(res);
            }
            
            @Override
            protected void onQueryComplete(int token, Object cookie, Cursor cursor) {
                //Log.i("@@@", "query complete");
                mActivity.init(cursor);
            }
        }

        AlbumListAdapter(Context context, AlbumBrowser currentactivity,
                int layout, Cursor cursor, String[] from, int[] to) {
            super(context, layout, cursor, from, to);

            mActivity = currentactivity;
            mQueryHandler = new QueryHandler(context.getContentResolver());
            
            mUnknownAlbum = context.getString(R.string.unknown_album_name);
            mUnknownArtist = context.getString(R.string.unknown_artist_name);
//            mAlbumSongSeparator = context.getString(R.string.albumsongseparator);

            Resources r = context.getResources();
            mNowPlayingOverlay = r.getDrawable(R.drawable.indicator_ic_mp_playing_list);

            Bitmap b = BitmapFactory.decodeResource(r, R.drawable.albumart_mp_unknown_list);
            mDefaultAlbumIcon = new BitmapDrawable(b);
            // no filter or dither, it's a lot faster and we can't tell the difference
            mDefaultAlbumIcon.setFilterBitmap(false);
            mDefaultAlbumIcon.setDither(false);
            getColumnIndices(cursor);
            mResources = context.getResources();
        }

        private void getColumnIndices(Cursor cursor) {
            if (cursor != null) {
                mAlbumNameIdx = Music.ALBUM_MAPPING.ALBUM_NAME;
                mAlbumIdIdx = Music.ALBUM_MAPPING.ID;
                mArtistNameIdx = Music.ALBUM_MAPPING.ARTIST_NAME;
                mArtistIdIdx  = Music.ALBUM_MAPPING.ID;
                mNumSongsIdx = Music.ALBUM_MAPPING.TRACK_COUNT;
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
        
        public void setActivity(AlbumBrowser newactivity) {
            mActivity = newactivity;
        }
        
        public AsyncQueryHandler getQueryHandler() {
            return mQueryHandler;
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
           vh.icon.setBackgroundDrawable(mDefaultAlbumIcon);
           vh.icon.setPadding(0, 0, 1, 0);
           v.setTag(vh);
           return v;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            
            ViewHolder vh = (ViewHolder) view.getTag();

            String name = cursor.getString(mAlbumNameIdx);
            String displayname = name;
            boolean unknown = name == null || name.equals(LockerDb.UNKNOWN_STRING); 
            if (unknown) {
                displayname = mUnknownAlbum;
            }
            vh.line1.setText(displayname);
            
            name = cursor.getString(mArtistNameIdx);
            displayname = name;
            if (name == null || name.equals(LockerDb.UNKNOWN_STRING)) {
                displayname = mUnknownArtist;
            }
            vh.line2.setText(displayname);

            ImageView iv = vh.icon;
            iv.setImageDrawable( null );
            name = cursor.getString(mAlbumIdIdx);
            Drawable d = Music.getCachedArtwork(context, Integer.valueOf( name ), mDefaultAlbumIcon);
            iv.setImageDrawable( d );
            
            int currentartistid = Music.getCurrentAlbumId();
            int aid = cursor.getInt(mAlbumIdIdx);
            iv = vh.play_indicator;
            if (currentartistid == aid) {
                iv.setImageDrawable(mNowPlayingOverlay);
            } else {
                iv.setImageDrawable(null);
            }
        }
        
        @Override
        public void changeCursor(Cursor cursor) {
            if (cursor != mActivity.mAlbumCursor) {
                mActivity.mAlbumCursor = cursor;
                getColumnIndices(cursor);
                super.changeCursor(cursor);
            }
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

        public void imageDownloadProgress( int imageDownloaded, int imageCount )
        {
            System.out.println("fetched " + imageDownloaded + " / " + imageCount );
            notifyDataSetChanged();
        }

        public void asynOperationEnded()
        {
            notifyDataSetChanged();
        }

        public void asynOperationStarted()
        {
            System.out.println("image fetch started");
        }
        
        public void setImageCache( ImageCache imageCache ) {
            mImageDownloader = new ImageDownloader(imageCache);
            mImageDownloader.setListener(this);
            mImageCache = imageCache;
    }
    }

    /**
     * Fetches albums async
     * @author ramblurr
     *
     */
    private class FetchAlbumsTask extends AsyncTask<Void, Void, Boolean>
    {
        Cursor cursor;
        @Override
        public void onPreExecute()
        {
            AlbumBrowser.this.showDialog( PROGRESS );
            Music.setSpinnerState(AlbumBrowser.this, true);
        }

        @Override
        public Boolean doInBackground( Void... params )
        {
            try
            {
                if(mArtistId != null)
                    cursor = Music.sDb.getAlbumsForArtist( Integer.valueOf( mArtistId ) );
                else
                    cursor = Music.sDb.getTableList( Music.Meta.ALBUM );
                
                Token[] t = Music.sDb.getTokens( Music.Meta.ALBUM );
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
            Music.setSpinnerState(AlbumBrowser.this, false);
            mArtFetcher = new FetchArtTask().execute();
            if( cursor != null)
                AlbumBrowser.this.init(cursor);
            else
                System.out.println("CURSOR NULL");
            mArtFetcher = new FetchArtTask().execute();
        }
    }
    
    private class FetchArtTask extends AsyncTask<Void, Integer, Boolean>
    {
        Cursor cursor;
        int[] list;
        @Override
        public void onPreExecute()
        {
            mAdapter.asynOperationStarted();
            System.out.println("fetch art");
            Music.setSpinnerState(AlbumBrowser.this, true);
            if( mAlbumCursor != null )
            {
                list = new int[mAlbumCursor.getCount()];
                mAlbumCursor.moveToFirst();
                for( int i=0; i < list.length; i++ ) {
                    list[i] = mAlbumCursor.getInt( Music.ALBUM_MAPPING.ID );
                    mAlbumCursor.moveToNext();
                }
                mAlbumCursor.moveToFirst();
                System.out.println("gonna fetch " + list.length);
            }else
                System.out.println("album cursor == null");
                
        }

        @Override
        public Boolean doInBackground( Void... params )
        {
            try
            {
                int lim = list.length;
                for(int i = 0; i < lim; i++) {
                    Music.sDb.fetchArt( list[i] );
                    publishProgress( i, lim );
                }
            }
            catch ( Exception e )
            {
                return false;
            }
            return true;
        }
        
        @Override
        protected void onProgressUpdate( Integer... values )
        {
            mAdapter.imageDownloadProgress( values[0], values[1] );
        }

        @Override
        public void onPostExecute( Boolean result )
        {
            Music.setSpinnerState(AlbumBrowser.this, false);
            mAdapter.asynOperationEnded();
        }
    }
    
    private class FetchTracksTask extends AsyncTask<Integer, Void, Boolean>
    {
        Cursor cursor;
        int action = -1;
        @Override
        public void onPreExecute()
        {
            Music.setSpinnerState(AlbumBrowser.this, true);
        }

        @Override
        public Boolean doInBackground( Integer... params )
        {
            if(params.length <= 1)
                return false;
            int album_id = params[0];
            action = params[1];
            try
            {
                if( Music.sDb!= null)
                    cursor = Music.sDb.getTracksForAlbum( album_id );
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
            Music.setSpinnerState(AlbumBrowser.this, false);
            if( cursor != null && result) {
                switch ( action )
                {
                case PLAY_SELECTION:
                    Music.playAll(AlbumBrowser.this, cursor, 0);
                    break;
                case QUEUE:
                    int[] ids = Music.getSongListForCursor( cursor );
                    Music.sDb.insertQueueItems( ids );
                    break;
                }
            } else
                System.out.println("CURSOR NULL");
        }
    }
   
}
