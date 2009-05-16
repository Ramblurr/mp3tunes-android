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

public class LockerCache
{

    long mArtistsLastUpdate = -1; // time of the last locker update
    long mAlbumsLastUpdate = -1; 
    long mTracksLastUpdate = -1; 
    long mPlaylistLastUpdate = -1; // time of the last playlist update
    long mPrefsLastUpdate = -1; // time of the last preferences update
    long mCacheLifetime; // the length of time the cache should last before
    // being considered expired
    boolean mAutoFetch = false; // should the db auto update content with the
                                // cache
    // expires

    /* LOCKER CACHE TYPES */
    static final int ARTIST = 0;
    static final int ALBUM = 1;
    static final int TRACK = 2;
    static final int PLAYLIST = 3;
    static final int PREFS = 4;
    

    public LockerCache( long lastUpdate, long lifetime, boolean autoFetch )
    {

        this( lastUpdate, lastUpdate, lastUpdate, lastUpdate, lastUpdate, lifetime, autoFetch );
    }

    public LockerCache( long artistUpdate, long albumUpdate, long trackUpdate, long playlistUpdate, long prefUpdate, long lifetime,
            boolean autoFetch )
    {

        mArtistsLastUpdate = artistUpdate;
        mAlbumsLastUpdate = albumUpdate;
        mTracksLastUpdate = trackUpdate;
        mPlaylistLastUpdate = playlistUpdate;
        mPrefsLastUpdate = prefUpdate;
        mCacheLifetime = lifetime;
        mAutoFetch = autoFetch;
    }

    /**
     * Convenience method that determines if the entire locker (data, playlists,
     * and preferences) are valid.
     */
    public boolean isCacheValid()
    {

        return isCacheValid( ARTIST )&& isCacheValid( ALBUM ) && isCacheValid( TRACK ) && isCacheValid( PLAYLIST ) && isCacheValid( PREFS );
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
        System.out.println(" mArtistsLastUpdate: " + mArtistsLastUpdate);
        System.out.println(" Now: " + now);
        long then = mArtistsLastUpdate + mCacheLifetime;
        System.out.println(" mArtistsLastUpdate + mCacheLifetime: " + then);
        
        switch ( type )
        {
        case ARTIST:
            return mArtistsLastUpdate > 0 && ( ( mArtistsLastUpdate + mCacheLifetime ) > now );
        case ALBUM:
            return mAlbumsLastUpdate > 0 && ( ( mAlbumsLastUpdate + mCacheLifetime ) > now );
        case TRACK:
            return mTracksLastUpdate > 0 && ( ( mTracksLastUpdate + mCacheLifetime ) > now );
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
}
