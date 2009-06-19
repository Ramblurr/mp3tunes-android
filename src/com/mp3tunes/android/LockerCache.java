/***************************************************************************
*   Copyright (C) 2009  Casey Link <unnamedrambler@gmail.com>             *
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

import android.content.Context;
import android.content.SharedPreferences;

public class LockerCache
{

    long mArtistsLastUpdate = -1; // time of the last locker update
    long mAlbumsLastUpdate = -1; 
    long mTracksLastUpdate = -1; 
    long mPlaylistLastUpdate = -1; // time of the last playlist update
    long mPrefsLastUpdate = -1; // time of the last preferences update
    long mArtistsTokensLastUpdate = -1; // time of the last locker update
    long mAlbumsTokensLastUpdate = -1; 
    long mTracksTokensLastUpdate = -1;
    
    
    // the length of time the cache should last before being 
    // considered expired expires
    long mCacheLifetime; 
    

    /* LOCKER CACHE TYPES */
    static final int ARTIST = 0;
    static final int ALBUM = 1;
    static final int TRACK = 2;
    static final int PLAYLIST = 3;
    static final int PREFS = 4;
    static final int ARTIST_TOKENS = 5;
    static final int ALBUM_TOKENS = 6;
    static final int TRACK_TOKENS = 7;
    

    private LockerCache()
    {}
    public LockerCache( long lastUpdate, long lifetime  )
    {

        this( lastUpdate, lastUpdate, lastUpdate, lastUpdate, lastUpdate, lifetime  );
    }

    public LockerCache( long artistUpdate, long albumUpdate, long trackUpdate, long playlistUpdate, long prefUpdate, long lifetime )
    {

        mArtistsLastUpdate = artistUpdate;
        mAlbumsLastUpdate = albumUpdate;
        mTracksLastUpdate = trackUpdate;
        mPlaylistLastUpdate = playlistUpdate;
        mPrefsLastUpdate = prefUpdate;
        mCacheLifetime = lifetime;
        mArtistsTokensLastUpdate =  artistUpdate;
        mAlbumsTokensLastUpdate =  albumUpdate;
        mTracksTokensLastUpdate =  trackUpdate;
    }

    /**
     * Determines if a particular part of the Locker has expired.
     * 
     * @param type
     *            the locker cache type, ARTIST, ALBUM, TRACK, PLAYLIST, PREFS
     * @return true if the supplied type is still valid, false if it has expired
     */
    public boolean isCacheValid( int type )
    {
        long now = System.currentTimeMillis();
        long then = mArtistsLastUpdate + mCacheLifetime;
        
        switch ( type )
        {
        case ARTIST:
            return mArtistsLastUpdate > 0 && ( ( mArtistsLastUpdate + mCacheLifetime ) > now );
        case ALBUM:
            return mAlbumsLastUpdate > 0 && ( ( mAlbumsLastUpdate + mCacheLifetime ) > now );
        case TRACK:
            return mTracksLastUpdate > 0 && ( ( mTracksLastUpdate + mCacheLifetime ) > now );
        case ARTIST_TOKENS:
            return mArtistsTokensLastUpdate > 0 && ( ( mArtistsTokensLastUpdate + mCacheLifetime ) > now );
        case ALBUM_TOKENS:
            return mAlbumsTokensLastUpdate > 0 && ( ( mAlbumsTokensLastUpdate + mCacheLifetime ) > now );
        case TRACK_TOKENS:
            return mTracksTokensLastUpdate > 0 && ( ( mTracksTokensLastUpdate + mCacheLifetime ) > now );
        case PLAYLIST:
            return mPlaylistLastUpdate > 0 && ( ( mPlaylistLastUpdate + mCacheLifetime ) > now );
        case PREFS:
            return mPrefsLastUpdate > 0 && ( ( mPrefsLastUpdate + mCacheLifetime ) > now );

        }
        return false;
    }

    public void setUpdate( long timestamp, int type )
    {
        switch ( type )
        {
        case ARTIST:
            mArtistsLastUpdate = timestamp;
            break;
        case ALBUM:
            mAlbumsLastUpdate = timestamp;
            break;
        case TRACK:
            mTracksLastUpdate = timestamp;
            break;
        case ARTIST_TOKENS:
            mArtistsTokensLastUpdate = timestamp;
            break;
        case ALBUM_TOKENS:
            mAlbumsTokensLastUpdate = timestamp;
            break;
        case TRACK_TOKENS:
            mTracksTokensLastUpdate = timestamp;
            break;
        case PLAYLIST:
            mPlaylistLastUpdate = timestamp;
            break;
        case PREFS:
            mPrefsLastUpdate = timestamp;
            break;

        }
    }

    public void setCacheLifetime( long length )
    {

        mCacheLifetime = length;
    }
    
    public void clearCache()
    {
        mArtistsLastUpdate = -1;
        mAlbumsLastUpdate = -1;
        mTracksLastUpdate = -1;
        mPlaylistLastUpdate = -1;
        mPrefsLastUpdate = -1;
        mArtistsTokensLastUpdate = -1;
        mAlbumsTokensLastUpdate = -1;
        mTracksTokensLastUpdate = -1;
    }
    
    public void saveCache(Context context)
    {
        SharedPreferences prefs = context.getSharedPreferences( MP3tunesApplication.LAST_UPDATE, 0 );
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong( "ArtistsLastUpdate", mArtistsLastUpdate );
        editor.putLong( "AlbumsLastUpdate", mAlbumsLastUpdate);
        editor.putLong( "TracksLastUpdate", mTracksLastUpdate);
        editor.putLong( "PlaylistLastUpdate", mPlaylistLastUpdate);
        editor.putLong( "PrefsLastUpdate", mPrefsLastUpdate);
        editor.putLong( "ArtistsTokensLastUpdate", mArtistsTokensLastUpdate);
        editor.putLong( "AlbumsTokensLastUpdate", mAlbumsTokensLastUpdate);
        editor.putLong( "TracksTokensLastUpdate", mTracksTokensLastUpdate);
        editor.commit();
    }
    
    public static LockerCache loadCache( Context context, long lifetime )
    {
        LockerCache cache = new LockerCache();
        SharedPreferences prefs = context.getSharedPreferences( MP3tunesApplication.LAST_UPDATE, 0 );
        
        cache.mArtistsLastUpdate = prefs.getLong( "ArtistsLastUpdate", -1 );
        cache.mAlbumsLastUpdate = prefs.getLong( "AlbumsLastUpdate", -1 );
        cache.mTracksLastUpdate = prefs.getLong( "TracksLastUpdate", -1 );
        cache.mPlaylistLastUpdate = prefs.getLong( "PlaylistLastUpdate", -1 );
        cache.mPrefsLastUpdate = prefs.getLong( "PrefsLastUpdate", -1 );
        cache.mArtistsTokensLastUpdate = prefs.getLong( "ArtistsTokensLastUpdate", -1 );
        cache.mAlbumsTokensLastUpdate = prefs.getLong( "AlbumsTokensLastUpdate", -1 );
        cache.mTracksTokensLastUpdate = prefs.getLong( "TracksTokensLastUpdate", -1 );
        cache.mCacheLifetime = lifetime;
        return cache;
    }
}
