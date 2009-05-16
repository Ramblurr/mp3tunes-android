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

    public static String[] TRACK = { "_id", "title", "artist_name", "artist_id", "album_name", "album_id",
            "track", "play_url", "download_url", "cover_url" };
}
