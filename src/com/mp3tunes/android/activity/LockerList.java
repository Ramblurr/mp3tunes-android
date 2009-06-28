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
import android.net.Uri;
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
import android.widget.ImageButton;
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

    // the database cursor
    private Cursor mCursor = null;

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

    private IntentFilter mIntentFilter;

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

        mLTRanim = AnimationUtils.loadAnimation( this, R.anim.ltrtranslation );
        mRTLanim = AnimationUtils.loadAnimation( this, R.anim.rtltranslation );

        // this prevents the background image from flickering when the
        // animations run
        getListView().setAnimationCacheEnabled( false );

        if(! Music.connectToDb( this ) )
            logout(); //TODO show error
        

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction( Mp3tunesService.PLAYBACK_ERROR );
        mIntentFilter.addAction( Mp3tunesService.META_CHANGED );

        
        Music.bindToService(this, this);
        displayMainMenu( TRANSLATION_LEFT );
    }
    
    @Override
    protected void onSaveInstanceState( Bundle outState )
    {
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
        Music.unconnectFromDb( this );

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
    
    
    @Override
    public boolean onPrepareOptionsMenu( Menu menu )
    {
        menu.findItem(R.id.menu_opt_player).setVisible( Music.isMusicPlaying() );
        return super.onPrepareOptionsMenu( menu );
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
    
    OnClickListener mSearchListener = new OnClickListener()
    {
    
        public void onClick( View arg0 )
        {
//            mHeaderText.setVisibility( View.INVISIBLE );
//            new SearchTask().execute( mSearchField.getText().toString() );
            
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
        // Show the requested option
        refreshMenu( position );
    }

    /**
     * Updates the screen with fresh menu options according to the current STATE
     * and which item was clicked.
     */
    private void refreshMenu( int pos )
    {
        if ( mPositionMenu == STATE.MAIN )
        {
            mPositionRow = pos;
            showSubMenu( TRANSLATION_LEFT );
        }
    }

    private void showSubMenu( int sense )
    {
        // which menu option has been selected
        Intent intent;
        switch ( mMainOpts[mPositionRow] )
        {
        case R.string.artists:
            intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(Uri.EMPTY, "vnd.mp3tunes.android.dir/artist");
            startActivity(intent);
            return; 

        case R.string.albums:
            intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(Uri.EMPTY, "vnd.mp3tunes.android.dir/album");
            startActivity(intent);
            return;

        case R.string.tracks:
            intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(Uri.EMPTY, "vnd.mp3tunes.android.dir/track");
            startActivity(intent);
            return;

        case R.string.search:
            onSearchRequested();
//            mPositionMenu = STATE.SEARCH;
//            (( ListAdapter ) getListAdapter() ).disableLoadBar();
//            toggleHeader();
            
            break;

        case R.string.playlists:
            intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(Uri.EMPTY, "vnd.mp3tunes.android.dir/playlist");
            startActivity(intent);
            break;
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
        Music.sDb.clearDB();
        Music.unconnectFromDb( this );
    }

    protected Dialog onCreateDialog( int id )
    {
        switch ( id )
        {
        case DIALOG_REFRESH: {
            ProgressDialog dialog = new ProgressDialog( this );
            dialog.setMessage( getString( R.string.loading_albums ) );
            dialog.setIndeterminate( true );
            dialog.setCancelable( false );
            return dialog;
        }

        }
        return null;

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
      Music.sDb.clearQueue();
      Music.sDb.appendQueueItem( track_id );
      System.out.println("playlist size: " + Music.sDb.getQueueSize());
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

