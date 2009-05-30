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

import com.mp3tunes.android.R;

/**
 * Simple static types and enums.
 *
 */
public class Music
{

    public static enum Meta
    {
        TRACK, ARTIST, ALBUM, CURRENT_PLAYLIST;
    }
    
    
    public static String[] ALBUM  = { "_id","album_name", "artist_id", "artist_name", "track_count", "cover_url" };

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
}
