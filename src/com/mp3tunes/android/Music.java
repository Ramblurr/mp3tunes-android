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
package com.mp3tunes.android;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Formatter;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Resources;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.view.Window;

import com.binaryelysium.mp3tunes.api.Locker;
import com.mp3tunes.android.R;
import com.mp3tunes.android.service.ITunesService;
import com.mp3tunes.android.service.Mp3tunesService;

/**
 * Simple static types and enums.
 *
 */
public class Music
{

    public static enum Meta
    {
        TRACK, ARTIST, ALBUM, PLAYLIST, CURRENT_PLAYLIST;
    }
    
    public static String[] ID = { "_id" };
    
    public static String[] TOKEN = { "type", "token", "count" };
    
    public static String[] ARTIST = { "_id", "artist_name" };
    
    public static String[] PLAYLIST = { "_id", "playlist_name", "file_count", "file_name" };
    
    public static String[] ALBUM  = { "_id","album_name", "artist_id", "artist_name", "track_count", "cover_url" };

  //This mapping corresponds to the field indexes above
    public static final class ARTIST_MAPPING 
    {
        public static final int ID = 0;
        public static final int ARTIST_NAME = 1;
    }
    
    //This mapping corresponds to the field indexes above
    public static final class ALBUM_MAPPING 
    {
        public static final int ID = 0;
        public static final int ALBUM_NAME = 1;
        public static final int ARTIST_ID = 2;
        public static final int ARTIST_NAME = 3;
        public static final int TRACK_COUNT = 4;
        public static final int COVER_URL = 5;
    }
    
    public static String[] TRACK = { "_id", "title", "artist_name", "artist_id", "album_name", "album_id",
            "track", "play_url", "download_url", "cover_url" };

    //This mapping corresponds to the field indexes above
    public static final class TRACK_MAPPING 
    {
        public static final int ID = 0;
        public static final int TITLE = 1;
        public static final int ARTIST_NAME = 2;
        public static final int ARTIST_ID = 3;
        public static final int ALBUM_NAME = 4;
        public static final int ALBUM_ID = 5;
        public static final int TRACKNUM = 6;
        public static final int PLAY_URL = 7;
        public static final int DOWNLOAD_URL = 8;
        public static final int COVER_URL = 9;
    }
    
    
    
    public static final class RepeatMode {
        // each song will be played once
        public static final int NO_REPEAT = 0; 
        // the current song will be repeated while repeatmode is REPEAT_SONG
        public static final int REPEAT_SONG = 1;  
        // the current playlist will be repeated when finished
        public static final int REPEAT_PLAYLIST = 2;   
    }
    
    public static final class ShuffleMode {
        // songs are played in the order of the playlist
        public static final int NORMAL = 0;
        // tracks are played randomly
        public static final int TRACKS = 1;
        // artists are played randomly
        public static final int ARTISTS = 2;
        // albums are played randomly
        public static final int ALBUMS = 3;
        public static int getText(int mode){
            switch(mode){
                case TRACKS: return R.string.shuffle_tracks;
                case ARTISTS: return R.string.shuffle_artists;
                case ALBUMS: return R.string.shuffle_albums;
                default : return R.string.shuffle_none;
            }
        }
    }
    
    public static LockerDb sDb = null;
    private static ArrayList<Context> sDbConnectionMap = new ArrayList<Context>();
    public static boolean connectToDb( Context context )
    {
        if( sDb == null ) {
            Locker locker = ( Locker ) MP3tunesApplication.getInstance().map.get( "mp3tunes_locker" );
            if(locker == null)
                return false;
            try
            // establish a connection with the database
            {
                System.out.println("on create getting db");
                sDb = new LockerDb( context.getApplicationContext(), locker );
            }
            catch ( Exception ex )
            {
                // database connection failed.
                // Show an error and exit gracefully.
                System.out.println( ex.getMessage() );
                ex.printStackTrace();
                return false;
            }
        }
        sDbConnectionMap.add( context );
        return true;
    }
    
    public static boolean unconnectFromDb( Context context )
    {
        boolean res = sDbConnectionMap.remove( context );
        if( sDbConnectionMap.isEmpty() )
            sDb.close();
        return res;
    }
    
    public static ITunesService sService = null;
    private static HashMap<Context, ServiceBinder> sConnectionMap = new HashMap<Context, ServiceBinder>();
    
    public static boolean bindToService(Context context) {
        return bindToService(context, null);
    }

    public static boolean bindToService(Context context, ServiceConnection callback) {
        context.startService(new Intent(context, Mp3tunesService.class));
        ServiceBinder sb = new ServiceBinder(callback);
        sConnectionMap.put(context, sb);
        return context.bindService((new Intent()).setClass(context,
                Mp3tunesService.class), sb, 0);
    }
    
    public static void unbindFromService(Context context) {
        ServiceBinder sb = (ServiceBinder) sConnectionMap.remove(context);
        if (sb == null) {
            Log.e("MusicUtils", "Trying to unbind for unknown Context");
            return;
        }
        context.unbindService(sb);
        if (sConnectionMap.isEmpty()) {
            // presumably there is nobody interested in the service at this point,
            // so don't hang on to the ServiceConnection
            sService = null;
        }
    }

    private static class ServiceBinder implements ServiceConnection {
        ServiceConnection mCallback;
        ServiceBinder(ServiceConnection callback) {
            mCallback = callback;
        }
        
        public void onServiceConnected(ComponentName className, android.os.IBinder service) {
            sService = ITunesService.Stub.asInterface(service);
//            initAlbumArtCache();
            if (mCallback != null) {
                mCallback.onServiceConnected(className, service);
            }
        }
        
        public void onServiceDisconnected(ComponentName className) {
            if (mCallback != null) {
                mCallback.onServiceDisconnected(className);
            }
            sService = null;
        }
    }
    
    /*  Try to use String.format() as little as possible, because it creates a
     *  new Formatter every time you call it, which is very inefficient.
     *  Reusing an existing Formatter more than tripled the speed of
     *  makeTimeString().
     *  This Formatter/StringBuilder are also used by makeAlbumSongsLabel()
     */
    private static StringBuilder sFormatBuilder = new StringBuilder();
    private static Formatter sFormatter = new Formatter(sFormatBuilder, Locale.getDefault());
    private static final Object[] sTimeArgs = new Object[5];

    public static String makeTimeString(Context context, long secs) {
        String durationformat = context.getString(R.string.durationformat);
        
        /* Provide multiple arguments so the format can be changed easily
         * by modifying the xml.
         */
        sFormatBuilder.setLength(0);

        final Object[] timeArgs = sTimeArgs;
        timeArgs[0] = secs / 3600;
        timeArgs[1] = secs / 60;
        timeArgs[2] = (secs / 60) % 60;
        timeArgs[3] = secs;
        timeArgs[4] = secs % 60;

        return sFormatter.format(durationformat, timeArgs).toString();
    }
}
