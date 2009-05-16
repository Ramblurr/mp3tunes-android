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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.util.Log;

import com.binaryelysium.mp3tunes.api.Album;
import com.binaryelysium.mp3tunes.api.Artist;
import com.binaryelysium.mp3tunes.api.Locker;
import com.binaryelysium.mp3tunes.api.LockerException;
import com.binaryelysium.mp3tunes.api.Track;

/**
 * This class is essentially a wrapper for storing
 * MP3tunes locker data in an sqlite databse.
 * It acts as a local cache of the metadata in a user's locker.
 * 
 * It is also used to handle the current playlist.
 *
 */
public class LockerDb
{

    private LockerCache mCache;
    private Context mContext;
    private Locker mLocker;
    private SQLiteDatabase mDb;
    
    private static final String DB_NAME = "locker.dat";
    private static final int DB_VERSION =  1;

    public LockerDb( Context context, Locker locker )
    {
        // Open the database
        mDb = (new LockerDbHelper(context, DB_NAME, null, DB_VERSION)).getWritableDatabase();
        if( mDb == null ){
            throw new SQLiteDiskIOException("Error creating database");
        }

        mLocker = locker;
        mContext = context;
        long lastupdate = MP3tunesApplication.getInstance().getLastUpdate();
        mCache = new LockerCache( lastupdate, 8640000, false ); // TODO handle cache timeout properly
    }

    public void close()
    {
        if ( mDb != null )
            mDb.close();
    }
    
    public void clearDB()
    {
        mDb.delete("track", null, null);
        mDb.delete("album", null, null);
        mDb.delete("artist", null, null);
//        mDb.execSQL("DELETE FROM current_playlist");
    }
    
    public void insert(Track track) throws IOException, SQLiteException {

        if(track == null)
        {
            System.out.println("OMG TRACK NULL");
            return;
        }
        int artist = 0, album = 0; // = 0 -> last value, !=0 -> null or select
        //, album = false, genre = false;
        mDb.execSQL("BEGIN TRANSACTION");
        try{
            if(track.getArtistName().length() > 0) {
                ContentValues cv = new ContentValues(2);
                cv.put("_id", track.getArtistId());
                cv.put("artist_name", track.getArtistName());
                
                Cursor c = mDb.query("artist", new String[]{"_id"}, "_id='"+track.getArtistId()+"'", null, null, null, null);
                if(c.moveToNext())
                    artist = c.getInt(0);
                else
                    mDb.insert( "artist", "Unknown", cv );
                c.close();
            }else{
                artist = 1;
            }
            /* TODO determine whether the fancy ContentValues means of performing queries 
             * is faster than the regular rawQuery + string a concatentation method.            
             */
            if(track.getAlbumTitle().length() > 0) {
                ContentValues cv = new ContentValues(2);
                cv.put( "_id", track.getAlbumId() );
                cv.put( "album_name", track.getAlbumTitle() );
                cv.put( "artist_id", track.getArtistId() );
                Cursor c = mDb.query("album", new String[]{"_id"}, "_id='"+track.getAlbumId()+"'", null, null, null, null);
//                Cursor c = mDb.rawQuery("SELECT _id FROM album WHERE _id='"+track.getAlbumId()+"'" ,null);
                if(c.moveToNext())
                    artist = c.getInt(0);
                else
                    mDb.insert( "album", "Unknown", cv );
//                    mDb.execSQL("INSERT INTO album(_id, album_name) VALUES("+track.getAlbumId()+", '"+track.getAlbumTitle()+"')");
                c.close();
            }else{
                album = 1;
            }
            
            Cursor c = mDb.query("track", new String[]{"_id"}, "_id='"+track.getId()+"'", null, null, null, null);
            if(!c.moveToNext())
            {
                ContentValues cv = new ContentValues(7);
                cv.put("_id", track.getId());
                cv.put("play_url", track.getPlayUrl());
                cv.put("download_url", track.getDownloadUrl());
                cv.put("title", track.getTitle());
                cv.put("track", track.getNumber());
                cv.put("artist_id", track.getArtistId());
                cv.put("album_id", track.getAlbumId());
                cv.put("cover_url", track.getAlbumArt());
                mDb.insert( "track", "Unknown", cv );
            }
            c.close();
//            mDb.execSQL("INSERT INTO track(_id, play_url, download_url, title, track, artist_id, album_id, cover_url) VALUES('"+
//                    track.getId()+"','"+
//                    track.getPlayUrl().replace("'", "''")+"','"+
//                    track.getDownloadUrl().replace("'", "''")+"','"+
//                    track.getTitle()+"',"+
//                    track.getNumber()+","+
//                    track.getArtistId()+","+
//                    track.getAlbumId()+",'"+
//                    (track.getAlbumArt() != null ? track.getAlbumArt().replace("'", "''") : "" ) +"')");
            
            mDb.execSQL("COMMIT TRANSACTION");

         }catch(SQLiteException e){
            mDb.execSQL("ROLLBACK");
            throw e;
        }
    }
    
    public Cursor getTableList(Music.Meta type) {
        
        if ( mCache.isCacheValid( LockerCache.ARTIST ) )
        {
            System.out.println("Cache valid");
            switch(type){
                case TRACK :
                    return mDb.query( "track", new String[]{"title", "_id", "track" }, null, null, null, null, "title" );
                case ALBUM :
                    return mDb.query( "album", new String[]{"album_name", "_id", "track_count" }, null, null, null, null, "album_name" );
                case ARTIST :
                    return mDb.query( "artist", new String[]{"artist_name","_id" }, null, null, null, null, "artist_name" );
                default : return null;
            }
        }
        System.out.println("Cache invalid");
        return null;
    }
    
    /**
     * 
     * @param albumId
     * @return 0 : album name, 1 : artist name, 2 : artist id. track count
     */
    public Cursor getAlbumInfo(int albumId){
        // because 1 is for unknown songs
        if(albumId == 1) return null;

        return mDb.rawQuery("SELECT album_name, artist_name, artist_id, track_count " +
                "FROM  album " +
                "WHERE album._id="+albumId, null);
    }
    
    /**
     * 
     * @param artist_id
     * @return 0: _id  1: album_name
     */
    public Cursor getAlbumsForArtist(int artist_id)
    {
        System.out.println("querying for albums by: " + artist_id);
        Cursor c = mDb.rawQuery( "SELECT artist_name FROM artist WHERE _id="+artist_id, null );
        if(c.moveToNext())
            System.out.println("Found: " + c.getString( 0 ));
        return mDb.query("album", new String[]{"_id", "album_name"}, "artist_id="+artist_id, null, null, null, "album_name");
    }
    
    /**
     * 
     * @param albumName
     * @return 0 : artist_name
     */
    public Cursor getArtistFromAlbum(String albumName){
        return mDb.rawQuery("SELECT DISTINCT artist_name" +
                " FROM artist, track, album" +
                " WHERE album_name='"+albumName+"'"+
                " AND album._id=track.album_id"+
                " AND track.artist_id=artist._id" +
                " ORDER BY artist_name"
                , null);
    }
    
    /**
     * 
     * @param album_id
     * @return 0: _id 1: title 2: artist_name 3:artist_id 4:album_name 5:album_id 6:track 7:play_url 8:download_url 9:cover_url
     */
    public Cursor getTracksForAlbum(int album_id)
    {
        return mDb.query("track", Music.TRACK, "album_id="+album_id, null, null, null, "track");
    }
    
    /**
     * 
     * @param trackId
     * @return 0: title 1: artist_name 2:artist_id 3:album_name 4:album_id 5:track 6:play_url 7:download_url 8:cover_url
     */
    public Cursor getTrackFromAlbum(int trackId)
    {
     return mDb.rawQuery("SELECT title, artist_name, artist_id, album_name, album_id, track, play_url, download_url, cover_url " +
               " FROM track" +
               " WHERE track._id="+trackId, null);
    }
    
    public void refreshCache() throws SQLiteException, IOException
    {
        clearDB();
        ArrayList<Track> tracks = new ArrayList<Track>( mLocker.getTracks() );
        int lim = tracks.size();
        System.out.println("beginning insertion of " + lim + " tracks");
        for ( int i = 0; i < lim; i++ )
        {
            insert( tracks.get( i ) );
        }
        System.out.println("insertion complete");
        mCache.setUpdate( System.currentTimeMillis(), LockerCache.ARTIST );
    }
    
    /**
     * Manages connecting, creating, and updating the database
     */
    
    private class LockerDbHelper extends SQLiteOpenHelper
    {
        
        private Context mC;
        public LockerDbHelper( Context context, String name, CursorFactory factory, int version )
        {
            super( context, name, factory, version );
            mC = context;
        }

        @Override
        public void onCreate( SQLiteDatabase db )
        {
            db.execSQL("CREATE TABLE track("+
                    "_id INTEGER PRIMARY KEY,"+
                    "play_url VARCHAR,"+
                    "download_url VARCHAR,"+
                    "title VARCHAR,"+
                    "track NUMBER(2) DEFAULT 0,"+
                    "artist_id INTEGER,"+
                    "artist_name VARCHAR,"+
                    "album_id INTEGER,"+
                    "album_name VARCHAR,"+
                    "cover_url VARCHAR DEFAULT NULL"+
                    ")"
            );
            db.execSQL("CREATE TABLE artist("+
                    "_id INTEGER PRIMARY KEY,"+
                    "artist_name VARCHAR,"+
                    "album_count INTEGER," +
                    "track_count INTEGER" +
                    " )"
            );
            db.execSQL("CREATE TABLE album("+
                    "_id INTEGER PRIMARY KEY,"+
                    "album_name VARCHAR, "+
                    "artist_id INTEGER,"+
                    "artist_name VARCHAR,"+
                    "track_count INTEGER,"+
                    "cover_url VARCHAR DEFAULT NULL"+
                    ")"
            );
            db.execSQL("CREATE TABLE current_playlist("+
                    "pos INTEGER PRIMARY KEY,"+
                    "id INTEGER"+
                    ")"
            );

        }

        @Override
        public void onUpgrade( SQLiteDatabase db, int oldV, int newV )
        {

            db.execSQL("DROP TABLE IF EXISTS current_playlist");
            db.execSQL("DROP TABLE IF EXISTS album");
            db.execSQL("DROP TABLE IF EXISTS artist");
            db.execSQL("DROP TABLE IF EXISTS track");
            db.execSQL("DROP TABLE IF EXISTS directory");
            onCreate(db);
        }
        
        public SQLiteDatabase getWritableDatabase() 
        {
            return super.getWritableDatabase() ;
            
        }

    }

}
