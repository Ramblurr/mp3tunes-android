/***************************************************************************
 *   Copyright (C) 2009  Casey Link <unnamedrambler@gmail.com>             *
 *   Copyright (C) 2007-2008 sibyl project http://code.google.com/p/sibyl/ *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 3 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.         *
 ***************************************************************************/
package com.mp3tunes.android.activity;

import java.util.ArrayList;
import java.util.Stack;

import com.binaryelysium.mp3tunes.api.Locker;
import com.binaryelysium.mp3tunes.api.Token;
import com.mp3tunes.android.AlphabetIndexer;
import com.mp3tunes.android.ListAdapter;
import com.mp3tunes.android.ListEntry;
import com.mp3tunes.android.LockerDb;
import com.mp3tunes.android.MP3tunesApplication;
import com.mp3tunes.android.Music;
import com.mp3tunes.android.R;
import com.mp3tunes.android.LockerDb.DbSearchQuery;
import com.mp3tunes.android.activity.QueueBrowser.TrackListAdapter;
import com.mp3tunes.android.service.Mp3tunesService;
import com.mp3tunes.android.util.UserTask;

import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

/**
 * Primary activity that encapsulates browsing the locker
 * 
 * @author ramblurr
 * 
 */
public class LockerList extends ListActivity implements ServiceConnection
{
    private EditText mSearchField;
    private Button mSearchButton;
    private TextView mHeaderText;
    private LinearLayout mSearchBar;
    private LinearLayout mMainHeader;
    private ViewFlipper mViewFlipper;

    // the database cursor
    private Cursor mCursor = null;

    private Locker mLocker;

    // the cached database
    private LockerDb mDb;

    // id List for main menu
    private static final int[] mMainOpts = { R.string.artists, R.string.albums, R.string.tracks,
            R.string.playlists, R.string.search };

    // icon list for the main menu
    private static final int[] mMainOptsIcon = { R.drawable.artist_icon, R.drawable.album_icon,
            R.drawable.song_icon, R.drawable.playlist_icon, R.drawable.search_icon };

    // number of main menu options
    private static final int mMainOptsNum = 5;

    // position of the id column in the dbase cursor (see LockerDb)
    private static final int ID = 2;

    // C++ style enum of the possible states of the LockerList
    private static final class STATE
    {

        public static final int MAIN = 0;
        public static final int ARTIST = 1;
        public static final int ALBUM = 2;
        public static final int TRACK = 3;
        public static final int PLAYLISTS = 4;
        public static final int SEARCH = 5;
    };

    // tracks the current position in the menu
    private int mPositionMenu = STATE.MAIN;

    // tracks the current row position in main menu
    private int mPositionRow = 0;

    // sense of the animation when changing menu
    private static final int TRANSLATION_LEFT = 0;
    private static final int TRANSLATION_RIGHT = 1;
    
    private static final int DIALOG_REFRESH = 0;

    private static final String TAG = "LockerList";

    private Animation mLTRanim;
    private Animation mRTLanim;

    // Stores browsing history within the activity
    private Stack<HistoryUnit> mHistory;
    private IntentFilter mIntentFilter;
    
    private String[] mAlphabet;

    /**
     * Encapsulates the required fields to store a browsing state.
     */
    private class HistoryUnit
    {

        public int state; // See LockerList.STATE
        public ListAdapter adapter;
        public HistoryUnit( int s, ListAdapter a )
        {
            state = s;
            adapter = a;
        }
    }

    @Override
    public void onCreate( Bundle icicle )
    {
        super.onCreate( icicle );
        requestWindowFeature( Window.FEATURE_NO_TITLE );
        setContentView( R.layout.lockerlist );

        mLocker = ( Locker ) MP3tunesApplication.getInstance().map.get( "mp3tunes_locker" );
        if ( mLocker == null )
            logout();

        mLTRanim = AnimationUtils.loadAnimation( this, R.anim.ltrtranslation );
        mRTLanim = AnimationUtils.loadAnimation( this, R.anim.rtltranslation );

        // this prevents the background image from flickering when the
        // animations run
        getListView().setAnimationCacheEnabled( false );

        try
        {
            // establish a connection with the database
            mDb = new LockerDb( this, mLocker );

        }
        catch ( Exception ex )
        {
            // database connection failed.
            // Show an error and exit gracefully.
            System.out.println( ex.getMessage() );
            ex.printStackTrace();
            MP3tunesApplication.getInstance().presentError( getApplicationContext(), "a", "" );
            logout();
        }

        mHistory = new Stack<HistoryUnit>();

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction( Mp3tunesService.PLAYBACK_ERROR );
        mIntentFilter.addAction( Mp3tunesService.META_CHANGED );
        
        mSearchField = ( EditText ) findViewById( R.id.search_editbox );
        mSearchButton = ( Button ) findViewById( R.id.search_button );
        mSearchButton.setOnClickListener( mSearchListener );
        mHeaderText = ( TextView ) findViewById( R.id.header_text );
        mSearchBar = ( LinearLayout ) findViewById( R.id.SearchBar );
        mMainHeader = ( LinearLayout ) findViewById( R.id.main_header );
        mViewFlipper = ( ViewFlipper ) findViewById( R.id.ViewFlipper );
        
        getAlphabet(LockerList.this);
        Music.bindToService(this, this);
        displayMainMenu( TRANSLATION_LEFT );
    }
    
    @Override
    protected void onSaveInstanceState( Bundle outState )
    {
//        outState.putParcelable( "cursor", mCursor );
//        outState.putSerializable( "cursor", mCursor );
        //TODO save state
    }
    
    @Override
    protected void onRestoreInstanceState( Bundle state )
    {
        //TODO restore state
    }

    @Override
    protected void onDestroy()
    {
        if ( mCursor != null )
            mCursor.close();
        mDb.close();

        super.onDestroy();
    }
    
    @Override
    protected void onPause() {
        unregisterReceiver(mStatusListener);
        super.onPause();
    }
    
    @Override
    public void onResume() {
        registerReceiver( mStatusListener, mIntentFilter );
        //We need to bind the player so we can see whether it's playing or not
        //in order to properly display the Now Playing indicator if we've been
        //relaunched after being killed.
    
        Music.bindToService(this, this);
        
        super.onResume();
    }
    
    public void onServiceConnected(ComponentName name, IBinder service)
    {
    }
    
    public void onServiceDisconnected(ComponentName name) {
//        finish();
    }
    
    private BroadcastReceiver mStatusListener = new BroadcastReceiver()
    {
        @Override
        public void onReceive( Context context, Intent intent )
        {

            String action = intent.getAction();
            if ( action.equals( Mp3tunesService.PLAYBACK_ERROR ) )
            {

            }
            else if( action.equals( Mp3tunesService.META_CHANGED))
            {
                //Update now playing buttons after the service is re-bound
                
            }
        }

    };

    /** Creates the menu items */
    public boolean onCreateOptionsMenu( Menu menu )
    {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main, menu);


        return true;
    }

    /** Handles menu clicks */
    public boolean onOptionsItemSelected( MenuItem item )
    {
        switch ( item.getItemId() )
        {
        case R.id.menu_opt_logout:
            logout();
            return true;
        case R.id.menu_opt_settings:
            Intent intent;
            intent = new Intent( LockerList.this, Preferences.class );
            startActivity( intent );
            return true;
        }
        return false;
    }

    /**
     * We want to intercept the Back keypress so we can handle our own backwards
     * navigation.
     */
    public boolean onKeyDown( int keyCode, KeyEvent event )
    {
        if ( keyCode == KeyEvent.KEYCODE_BACK )
        {
            if ( !mHistory.isEmpty() )
            {
                if( mPositionMenu == STATE.SEARCH ) {
                    getListView().setVisibility( View.VISIBLE );
                    toggleHeader();
                }
                HistoryUnit u = mHistory.pop();
                mPositionMenu = u.state;
                getListView().startAnimation( mLTRanim );
                setListAdapter( u.adapter );
                ((ListAdapter) getListAdapter()).disableLoadBar();
                return true;
            }
        }
        return false;
    }
    
    private void toggleHeader()
    {
        if( mViewFlipper.getDisplayedChild() == 0 )
        {
//            performSlide( TRANSLATION_RIGHT );
            mViewFlipper.setDisplayedChild( 1 );
//            mViewFlipper.setInAnimation( mRTLanim );
//            mViewFlipper.setOutAnimation( mLTRanim );
            getListView().setVisibility( View.INVISIBLE );
            
        } else {
//            performSlide( TRANSLATION_LEFT );
            mViewFlipper.setDisplayedChild( 0 );
//            mViewFlipper.setInAnimation( mLTRanim );
//            mViewFlipper.setOutAnimation( mRTLanim );
            getListView().setVisibility( View.VISIBLE );
        }
            
    }
    
    OnClickListener mSearchListener = new OnClickListener()
    {
    
        public void onClick( View arg0 )
        {
//            mHeaderText.setVisibility( View.INVISIBLE );
            new SearchTask().execute( mSearchField.getText().toString() );
            
        }
    };

    /** displays the main menu */
    private void displayMainMenu( int sense )
    {
        ArrayList<ListEntry> iconifiedEntries = new ArrayList<ListEntry>();

        for ( int i = 0; i < mMainOptsNum; i++ )
        { // add all strings to the adapter
            ListEntry entry = new ListEntry( getString( mMainOpts[i] ), mMainOptsIcon[i],
                    getString( mMainOpts[i] ), R.drawable.list_arrow );
            iconifiedEntries.add( entry );
        }
        ListAdapter adapter = new ListAdapter( LockerList.this );
        adapter.setSourceIconified( iconifiedEntries );
        setListAdapter( adapter );

        getListView().setSelection( mPositionRow );
        performSlide( sense );
    }

    protected void onListItemClick( ListView l, View vu, int position, long id )
    {
        // Store the current state in the history stack
        HistoryUnit u = new HistoryUnit( mPositionMenu, ( ListAdapter ) getListAdapter() );
        mHistory.push( u );

        // Show the requested option
        refreshMenu( position );
    }

    /**
     * Updates the screen with fresh menu options according to the current STATE
     * and which item was clicked.
     */
    private void refreshMenu( int pos )
    {
        (( ListAdapter ) getListAdapter()).enableLoadBar( pos );
        if ( mPositionMenu == STATE.MAIN )
        {
            mPositionRow = pos;
            showSubMenu( TRANSLATION_LEFT );
        }
        else
        {
            if ( mCursor != null && mPositionMenu != STATE.PLAYLISTS
                    && mPositionMenu != STATE.SEARCH )
                mCursor.moveToPosition( pos );

            if ( mPositionMenu == STATE.ARTIST )
            { // Going to album list
                
                ListAdapter a = ( ListAdapter ) getListAdapter();
                int artist_id = ( Integer ) a.getItem( pos );
                new FetchAlbumsTask().execute( TRANSLATION_LEFT, artist_id );
            }
            else if ( mPositionMenu == STATE.ALBUM )
            { // Going to track list
                ListAdapter a = ( ListAdapter ) getListAdapter();
                int album_id = ( Integer ) a.getItem( pos );
                new FetchTracksTask().execute( TRANSLATION_LEFT, album_id );
            }
            else if ( mPositionMenu == STATE.TRACK )
            {
                ListAdapter a = ( ListAdapter ) getListAdapter();
                int track_id = ( Integer ) a.getItem( pos );
                playTrack( track_id );
            }
            else if ( mPositionMenu == STATE.PLAYLISTS )
            {
                ListAdapter a = ( ListAdapter ) getListAdapter();
                int playlist_id = ( Integer ) a.getItem( pos );
                new FetchPlaylistsTask().execute( TRANSLATION_LEFT, playlist_id );

            }
            else if ( mPositionMenu == STATE.SEARCH )
            {
                ListAdapter a = ( ListAdapter ) getListAdapter();
                int objid = ( Integer ) a.getItem( pos );
                Music.Meta type = ( Music.Meta ) a.getSecondValue( pos );
                
                switch ( type )
                {
                    case ARTIST:
                        new FetchAlbumsTask().execute( TRANSLATION_LEFT, objid );
                        break; 
                    case TRACK:
                        playTrack( objid );
                        break;
                }
                
            }
        }

    }

    private void showSubMenu( int sense )
    {
//        if ( sense == TRANSLATION_LEFT )
//        {
//            getListView().startAnimation( mRTLanim );
//        }
//        else if ( sense == TRANSLATION_RIGHT )
//        {
//            getListView().startAnimation( mLTRanim );
//        }

        // which manu menu option has been selected
        switch ( mMainOpts[mPositionRow] )
        {
        case R.string.artists:
            new FetchArtistsTask().execute( sense );
            return; 

        case R.string.albums:
            new FetchAlbumsTask().execute( sense );
            return;

        case R.string.tracks:
            new FetchTracksTask().execute( sense );
            return;

        case R.string.search:
            mPositionMenu = STATE.SEARCH;
            (( ListAdapter ) getListAdapter() ).disableLoadBar();
            toggleHeader();
            
            break;

        case R.string.playlists:
            new FetchPlaylistsTask().execute( sense );
            break;
        }
    }
    
    private void handleListSwitch( int id_field, int icon_id, int text_id, int second_text_id, int disclosure_id, String[] tokens )
    {
        ((ListAdapter) getListAdapter()).disableLoadBar();
        // if the cursor is empty, we adjust the text in function of the submenu
        if ( mCursor == null )
        {
            System.out.println( "ERROR CURSOR NULL" );
            return;
        }
        startManagingCursor( mCursor );
        if ( mCursor.getCount() == 0 )
        {
            System.out.println( "CURSOR EMPTY" );
            TextView emptyText = ( TextView ) findViewById( android.R.id.empty );
            switch ( mPositionMenu )
            {
            case STATE.ARTIST:
                emptyText.setText( R.string.empty_artist );
                break;
            case STATE.ALBUM:
                emptyText.setText( R.string.empty_album );
                break;
            case STATE.TRACK:
                emptyText.setText( R.string.empty_track );
                break;
            }
        }

        setListAdapter( adapterFromCursor( id_field, icon_id, text_id, second_text_id, disclosure_id, tokens ) );
    }

    /**
     * Helper function that creates a custom ListAdapter from the current
     * mCursor
     * 
     * @param id_field
     *            the column id that contains the id (track_id, artist_id, etc)
     * @param icon_id
     *            the resource id that contains the id of the icon
     * @param text_id
     *            the column id that contains the text to be displayed in the
     *            ListEntry
     * @param disclosure_id
     *            the resource id that contains the id of the disclosure icon
     * @param tokens
     *            an array of tokens to be used in the indexer
     * @return
     */
    private ListAdapter adapterFromCursor( int id_field, int icon_id, int text_id, int second_text_id, int disclosure_id, String[] tokens )
    {
        ArrayList<ListEntry> iconifiedEntries = new ArrayList<ListEntry>();
        while ( mCursor.moveToNext() )
        {
            String second = ""; 
            if( second_text_id > -1 )
                second = mCursor.getString( second_text_id );
            ListEntry entry = new ListEntry( mCursor.getInt( id_field ), icon_id == -1 ? null
                    : icon_id, mCursor.getString( text_id ), disclosure_id, second );
            iconifiedEntries.add( entry );

        }
        ListAdapter adapter = new ListAdapter( LockerList.this );
        if( tokens == null) {
            System.out.println("Using alphabet!");
            tokens = mAlphabet;
        }
        System.out.println("final tokens " + tokens.length);
        AlphabetIndexer indexer = new AlphabetIndexer(mCursor, text_id, tokens);
        adapter.setIndexer( indexer );
        adapter.setSourceIconified( iconifiedEntries );
        return adapter;
    }
    
    private void getAlphabet(Context context) {
        String alphabetString = context.getResources().getString(R.string.alphabet);
        mAlphabet = new String[alphabetString.length()];
        for (int i = 0; i < mAlphabet.length; i++) {
            mAlphabet[i] = String.valueOf(alphabetString.charAt(i));
        }
    }

    /**
     * Clears all session data (and the cache), and sends the user back to the
     * login screen.
     */
    private void logout()
    {
        clearData();
        Intent intent = new Intent( LockerList.this, Login.class );
        startActivity( intent );
        finish();
    }

    private void clearData()
    {
        SharedPreferences settings = getSharedPreferences( Login.PREFS, 0 );
        SharedPreferences.Editor editor = settings.edit();
        editor.remove( "mp3tunes_user" );
        editor.remove( "mp3tunes_pass" );
        editor.commit();
        MP3tunesApplication.getInstance().clearUpdate();
        mDb.clearDB();
        mDb.close();
    }

    protected Dialog onCreateDialog( int id )
    {
        switch ( id )
        {
        case DIALOG_REFRESH: {
            ProgressDialog dialog = new ProgressDialog( this );
            dialog.setMessage( getString( R.string.loading_msg ) );
            dialog.setIndeterminate( true );
            dialog.setCancelable( false );
            return dialog;
        }

        }
        return null;

    }

    private class RefreshCacheTask extends UserTask<Void, Void, Boolean>
    {
        @Override
        public void onPreExecute()
        {
            showDialog( DIALOG_REFRESH );
        }

        @Override
        public Boolean doInBackground( Void... params )
        {
            try
            {
//                mDb.refreshTrCache();
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
            if ( result )
            {
                dismissDialog( DIALOG_REFRESH );
            }
            else
            {
                MP3tunesApplication.getInstance().presentError( getApplicationContext(),
                        getString( R.string.ERROR_SERVER_UNAVAILABLE_TITLE ),
                        getString( R.string.ERROR_SERVER_UNAVAILABLE ) );
                logout();
            }
        }
    }
    
    private class FetchArtistsTask extends UserTask<Integer, Void, Boolean>
    {
        int sense = -1;
        String[] tokens= null;
        @Override
        public void onPreExecute()
        {
            
        }

        @Override
        public Boolean doInBackground( Integer... params )
        {
            sense = params[0];
            try
            {
                mCursor = mDb.getTableList( Music.Meta.ARTIST );
                Token[] t = mDb.getTokens( Music.Meta.ARTIST );
                tokens = LockerDb.tokensToString( t );
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
            // TODO error handling
            performSlide( sense );
            mPositionMenu = STATE.ARTIST;
            handleListSwitch( 0, R.drawable.artist_icon, 1, -1, R.drawable.arrow, tokens );
        }
    }
    
    /**
     * Fetches albums async
     * Takes a Int[]
     *  0: sense  animate TRANSLATION_RIGHT or TRANSLATION_LEFT
     *  1: artist_id or -1
     * @author ramblurr
     *
     */
    private class FetchAlbumsTask extends UserTask<Integer, Void, Boolean>
    {
        int sense = -1;
        String[] tokens= null;
        @Override
        public void onPreExecute()
        {
            
        }

        @Override
        public Boolean doInBackground( Integer... params )
        {
            sense = params[0];
            int artist_id = -1;
            if(params.length > 1)
                artist_id = params[1];
            try
            {
                if(artist_id > -1)
                    mCursor = mDb.getAlbumsForArtist( artist_id );
                else
                    mCursor = mDb.getTableList( Music.Meta.ALBUM );
                
                Token[] t = mDb.getTokens( Music.Meta.ALBUM );
                tokens = LockerDb.tokensToString( t );
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
            // TODO error handling
            performSlide( sense );
            mPositionMenu = STATE.ALBUM;
            handleListSwitch( 0, R.drawable.album_icon, 1, 3, R.drawable.arrow, tokens );
        }
    }
    
    private class FetchTracksTask extends UserTask<Integer, Void, Boolean>
    {
        int sense = -1;
        String[] tokens= null;
        @Override
        public void onPreExecute()
        {
            
        }

        @Override
        public Boolean doInBackground( Integer... params )
        {
            sense = params[0];
            int album_id = -1;
            if(params.length > 1)
                album_id = params[1];
            try
            {
                if(album_id > -1)
                    mCursor = mDb.getTracksForAlbum( album_id );
                else
                    mCursor = mDb.getTableList( Music.Meta.TRACK );
                
                Token[] t = mDb.getTokens( Music.Meta.TRACK );
                tokens = LockerDb.tokensToString( t );
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
            performSlide( sense );
            mPositionMenu = STATE.TRACK;
            handleListSwitch( 0, R.drawable.song_icon, 1, 2, R.drawable.right_play, tokens );
        }
    }
    
    private class FetchPlaylistsTask extends UserTask<Integer, Void, Boolean>
    {
        int sense = -1;
        boolean fetching_tracks = false;
        @Override
        public void onPreExecute()
        {
            
        }

        @Override
        public Boolean doInBackground( Integer... params )
        {
            sense = params[0];
            int playlist_id = -1;
            if(params.length > 1) {
                playlist_id = params[1];
                fetching_tracks = true;
            }
            try
            {
                if(fetching_tracks)
                    mCursor = mDb.getTracksForPlaylist( playlist_id );
                else
                    mCursor = mDb.getTableList( Music.Meta.PLAYLIST );
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
            // TODO error handling

            mPositionMenu = STATE.PLAYLISTS;
            if( fetching_tracks )
            {
                mDb.clearQueue();
                while( mCursor.moveToNext() ) 
                {
                    System.out.println("Got track id "+ mCursor.getInt( 0 ) + " called " + mCursor.getString( 1 )); 
                    mDb.appendQueueItem( mCursor.getInt( 0 ) );
                }
                ((ListAdapter) getListAdapter()).disableLoadBar();
                showPlayer();
//                handleListSwitch( 0, R.drawable.song_icon, 1, -1, R.drawable.right_play, null );
            } else
                handleListSwitch( 0, R.drawable.playlist_icon, 1, -1, R.drawable.right_play, null );
        }
    }
    
    private class SearchTask extends UserTask<String, Void, Boolean>
    {
        int sense = -1;
        String[] tokens= null;
        LockerDb.DbSearchResult res;
        @Override
        public void onPreExecute()
        {
            
        }

        @Override
        public Boolean doInBackground( String... params )
        {
            try
            {
                res = mDb.search( mDb.new DbSearchQuery( params[0], true, false, true ) );
                
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
            if(res == null)
            {
                System.out.println("No results");
                return;
            }
            ArrayList<ListEntry> entries = new ArrayList<ListEntry>();
            if( res.mArtists != null ) 
                entries.addAll( entriesFromCursor( 0, R.drawable.artist_icon, 1, R.drawable.arrow, res.mArtists, Music.Meta.ARTIST ) );
            if( res.mTracks != null )
                entries.addAll( entriesFromCursor( 0, R.drawable.song_icon, 1, R.drawable.right_play, res.mTracks, Music.Meta.TRACK ) );
            System.out.println("total entries: "  + entries.size());
            ListAdapter adapter = new ListAdapter( LockerList.this );
            adapter.setSourceIconified( entries );
            setListAdapter( adapter );
            getListView().setVisibility( View.VISIBLE );
            res.mArtists.close();
            res.mTracks.close();
        }
    }
    
  private ArrayList<ListEntry> entriesFromCursor( int id_field, int icon_id, int text_id, int disclosure_id, Cursor cursor, Music.Meta type )
  {
      ArrayList<ListEntry> iconifiedEntries = new ArrayList<ListEntry>();
      System.out.println("Cursor size: " + cursor.getCount());
      while ( cursor.moveToNext() )
      {
          ListEntry entry = new ListEntry( cursor.getInt( id_field ), icon_id == -1 ? null
                  : icon_id, cursor.getString( text_id ), disclosure_id );
          entry.setSecondValue( type );
          iconifiedEntries.add( entry );

      }
      System.out.println("made entries: " + iconifiedEntries.size());
      return iconifiedEntries;
  }
  
  private void performSlide( int sense )
  {
      if ( sense == TRANSLATION_LEFT )
      {
          getListView().startAnimation( mRTLanim );
      }
      else if ( sense == TRANSLATION_RIGHT )
      {
          getListView().startAnimation( mLTRanim );
      }
  }
  
  private void playTrack( int track_id )
  {
      mDb.clearQueue();
      mDb.appendQueueItem( track_id );
      System.out.println("playlist size: " + mDb.getQueueSize());
      showPlayer();
  }
  
  private void showPlayer()
  {
      try
      {
          if( Music.sService != null ) 
          {
              Music.sService.start();
              Intent i = new Intent( LockerList.this, Player.class );
//              i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
              startActivity( i );
          } else
              System.out.println("Player is null!");
          
      }
      catch ( RemoteException e )
      {
          // TODO Auto-generated catch block
          e.printStackTrace();
      }
  }

}

