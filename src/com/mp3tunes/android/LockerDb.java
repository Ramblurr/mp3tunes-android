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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDiskIOException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.binaryelysium.mp3tunes.api.Album;
import com.binaryelysium.mp3tunes.api.Artist;
import com.binaryelysium.mp3tunes.api.Locker;
import com.binaryelysium.mp3tunes.api.Playlist;
import com.binaryelysium.mp3tunes.api.Token;
import com.binaryelysium.mp3tunes.api.Track;
import com.binaryelysium.mp3tunes.api.results.DataResult;
import com.binaryelysium.mp3tunes.api.results.SearchResult;
import com.mp3tunes.android.LockerCache;

/**
 * This class is essentially a wrapper for storing MP3tunes locker data in an
 * sqlite databse. It acts as a local cache of the metadata in a user's locker.
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
    private static final int DB_VERSION = 2;
    
    public static final String UNKNOWN_STRING = "Unknown";

    public LockerDb( Context context, Locker locker )
    {
        // Open the database
        mDb = ( new LockerDbHelper( context, DB_NAME, null, DB_VERSION ) ).getWritableDatabase();
        if ( mDb == null )
        {
            throw new SQLiteDiskIOException( "Error creating database" );
        }

        mLocker = locker;
        mContext = context;
        mCache = LockerCache.loadCache( context, 86400000  ); // 1 day
    }

    public void close()
    {
        mCache.saveCache( mContext );
        if ( mDb != null )
            mDb.close();
    }

    public void clearDB()
    {
        mCache.clearCache();
        mDb.delete( "track", null, null );
        mDb.delete( "album", null, null );
        mDb.delete( "artist", null, null );
        mDb.delete( "playlist", null, null );
        mDb.delete( "playlist_tracks", null, null );
        mDb.delete( "token", null, null );
        mDb.delete( "current_playlist", null, null );
    }

    public Cursor getTableList( Music.Meta type )
    {
        try
        {
            switch ( type )
            {
            case TRACK:
                if ( !mCache.isCacheValid( LockerCache.TRACK ) )
                    refreshTracks();
                return queryTracks();
            case ALBUM:
                if ( !mCache.isCacheValid( LockerCache.ALBUM ) )
                    refreshAlbums();
                return queryAlbums();
            case ARTIST:
                if ( !mCache.isCacheValid( LockerCache.ARTIST ) )
                    refreshArtists();
                return queryArtists();
            case PLAYLIST:
                if ( !mCache.isCacheValid( LockerCache.PLAYLIST ) )
                    refreshPlaylists();
                return queryPlaylists();
            default:
                return null;
            }
        }
        catch ( SQLiteException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch ( IOException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return null;
    }
    
    public Token[] getTokens( Music.Meta type )
    {
        try
        {
            Cursor c;
            switch ( type )
            {
            case TRACK:
                if ( !mCache.isCacheValid( LockerCache.TRACK_TOKENS ) )
                    refreshTokens( type );
                c = queryTokens("track");
                break;
            case ALBUM:
                if ( !mCache.isCacheValid( LockerCache.ALBUM_TOKENS ) )
                    refreshTokens( type );
                c = queryTokens("album");
                break;
            case ARTIST:
                if ( !mCache.isCacheValid( LockerCache.ARTIST_TOKENS ) )
                    refreshTokens( type );
                c = queryTokens("artist");
                break;
            default:
                return null;
            }
            Token[] t = new Token[c.getCount()];
            while( c.moveToNext() )
            {
                t[c.getPosition()] = new Token( c.getString( 1 ), c.getInt( 2 ) );
            }
            c.close();
            return t;
        }
        catch ( SQLiteException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch ( IOException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * 
     * @param albumId
     * @return 0 : album name, 1 : artist name, 2 : artist id. track count
     */
    public Cursor getAlbumInfo( int albumId )
    {
        // because 1 is for unknown songs
        if ( albumId == 1 )
            return null;

        return mDb.rawQuery( "SELECT album_name, artist_name, artist_id, track_count "
                + "FROM  album " + "WHERE album._id=" + albumId, null );
    }

    /**
     * 
     * @param artist_id
     * @return 0: _id 1: album_name
     */
    public Cursor getAlbumsForArtist( int artist_id )
    {
        System.out.println( "querying for albums by: " + artist_id );
        Cursor c = mDb.rawQuery( "SELECT artist_name FROM artist WHERE _id=" + artist_id, null );
        if ( !c.moveToNext() ) {
            //TODO fetch the artist?
            Log.e( "Mp3tunes", "Error artist doesnt exist" );
            return null;
        }
        c.close();
        
        c = queryAlbums( artist_id ); 
        
        if( c.getCount() > 0 )
            return c;
        else
            c.close();
        try
        {
            refreshAlbumsForArtist( artist_id );
            return queryAlbums( artist_id );  
        }
        catch ( SQLiteException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch ( IOException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 
     * @param albumName
     * @return 0 : artist_name
     */
    public Cursor getArtistFromAlbum( String albumName )
    {
        return mDb.rawQuery( "SELECT DISTINCT artist_name" + " FROM artist, track, album"
                + " WHERE album_name='" + albumName + "'" + " AND album._id=track.album_id"
                + " AND track.artist_id=artist._id" + " ORDER BY artist_name", null );
    }

    /**
     * 
     * @param album_id
     * @return 0: _id 1: title 2: artist_name 3:artist_id 4:album_name
     *         5:album_id 6:track 7:play_url 8:download_url 9:cover_url
     */
    public Cursor getTracksForAlbum( int album_id )
    {
        System.out.println( "querying for tracks on album: " + album_id );
        Cursor c = mDb.rawQuery( "SELECT album_name FROM album WHERE _id=" + album_id, null );
        if ( !c.moveToNext() ) {
            //TODO fetch the album?
            Log.e( "Mp3tunes", "Error album doesnt exist" );
            return null;
        }
        c.close();
        
        c = queryTracksAlbum( album_id );

        if( c.getCount() > 0 )
            return c;
        else
            c.close();

        try
        {
            refreshTracksforAlbum( album_id );
            return queryTracksAlbum( album_id );  
        }
        catch ( SQLiteException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch ( IOException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 
     * @param artist_id
     * @return 0: _id 1: title 2: artist_name 3:artist_id 4:album_name
     *         5:album_id 6:track 7:play_url 8:download_url 9:cover_url
     */
    public Cursor getTracksForArtist( int artist_id )
    {
        System.out.println( "querying for tracks of artist: " + artist_id );
        Cursor c = mDb.rawQuery( "SELECT artist_name FROM artist WHERE _id=" + artist_id, null );
        if ( !c.moveToNext() ) {
            //TODO fetch the artist?
            Log.e( "Mp3tunes", "Error album doesnt exist" );
            return null;
        }
        c.close();
        
        c = queryTracksArtist( artist_id );

        if( c.getCount() > 0 )
            return c;
        else
            c.close();

        try
        {
            refreshTracksforArtist( artist_id );
            return queryTracksAlbum( artist_id );  
        }
        catch ( SQLiteException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch ( IOException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
    public Cursor getTracksForPlaylist( int playlist_id )
    {
        Cursor c = mDb.rawQuery( "SELECT playlist_name FROM playlist WHERE _id=" + playlist_id, null );
        if ( !c.moveToNext() ) {
            //TODO fetch the playlist?
            Log.e( "Mp3tunes", "Error playlist doesnt exist" );
            return null;
        }
        c.close();
        c = queryPlaylists( playlist_id );

        if( c.getCount() > 0 )
            return c;
        else
            c.close();
        try
        {
            refreshTracksforPlaylist( playlist_id );
            return queryPlaylists( playlist_id );  
        }
        catch ( SQLiteException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch ( IOException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }
    
    

    /**
     * return the song at pos in the queue
     * 
     * @param pos NOTE: Positions are 1 indexed i.e., the first song is @ pos = 1
     * @return a complete Track obj of the song
     */
    public Track getTrackQueue( int pos )
    {
        Cursor c = mDb.query( "track, current_playlist", Music.TRACK, "current_playlist.pos=" + pos+" AND track._id=current_playlist.track_id", null, null, null, null );
//        Cursor c = mDb.rawQuery("SELECT play_url FROM song, current_playlist WHERE pos="+pos+" AND song._id=current_playlist.id", null);
        if ( !c.moveToFirst() )
        {
            c.close();
            return null;
        }
        
        Track t = new Track(
                c.getInt( Music.TRACK_MAPPING.ID ),
                c.getString( Music.TRACK_MAPPING.PLAY_URL ),
                c.getString( Music.TRACK_MAPPING.DOWNLOAD_URL ),
                c.getString( Music.TRACK_MAPPING.TITLE ),
                c.getInt( Music.TRACK_MAPPING.TRACKNUM ),
                c.getInt( Music.TRACK_MAPPING.ARTIST_ID ),
                c.getString( Music.TRACK_MAPPING.ARTIST_NAME ),
                c.getInt( Music.TRACK_MAPPING.ALBUM_ID ),
                c.getString( Music.TRACK_MAPPING.ALBUM_NAME ),
                c.getString( Music.TRACK_MAPPING.COVER_URL ) );
        c.close();
        return t;
    }
    
    public boolean artCached( String album_id )
    {
        boolean cacheEnabled = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean( "cacheart", false );
        String cacheDir = Environment.getExternalStorageDirectory() + "/mp3tunes/art/";
        String ext = ".jpg";
        if( cacheEnabled )
        {
            File f = new File( cacheDir + album_id + ext);
            if( f.exists() && f.canRead() )
            {
                return true;
            }
        }
        return false;
    }

    public void fetchArt( int album_id )
    {
        boolean cacheEnabled = PreferenceManager.getDefaultSharedPreferences(mContext).getBoolean( "cacheart", false );
        Bitmap ret = null;
        try {
        String cacheDir = Environment.getExternalStorageDirectory() + "/mp3tunes/art/";
        String ext = ".jpg";
        if( cacheEnabled )
        {
            File f = new File( cacheDir + album_id + ext);
            if( !f.exists() && !f.canRead() )
            {
                Track[] tracks = null;
                synchronized(mLocker)
                {
                    tracks = mLocker.getTracksForAlbum( Integer.valueOf(  album_id ) ).getData();
                }
                String artUrl = null;
                for( Track t : tracks)
                {
                    artUrl = t.getAlbumArt();
                    if( artUrl != null )
                        break;
                }
                ret  = getArtwork( artUrl );
                if( ret != null && cacheEnabled ) 
                {
                    f = new File( cacheDir);
                    f.mkdirs();
                    f = new File( cacheDir + album_id + ext);
                    f.createNewFile();
                    
                    if( f.canWrite() )
                    {
                        FileOutputStream out = new FileOutputStream(f);
                        ret.compress( Bitmap.CompressFormat.JPEG, 70, out );
                        out.close();
                    }
                }
            }
        }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    
    public DbSearchResult search( DbSearchQuery query )
    {
        try
        {
            // First, determine which types we need to refresh.
            // this construct seems complicated (which it is..)
            // but it is faster than performing three separate http calls.
            // This way we can lump the refreshes into one http call

            boolean artist = false, track = false ,album = false;
            if(query.mTracks && !mCache.isCacheValid( LockerCache.TRACK ) )
                track = true;
            if(query.mAlbums && !mCache.isCacheValid( LockerCache.ALBUM ) )
                album = true;
            if(query.mArtists && !mCache.isCacheValid( LockerCache.ARTIST ) )
                artist = true;
            
            // Perform the single http search call
            refreshSearch( query.mQuery, artist, album, track );
            
            DbSearchResult res = new DbSearchResult();
            if( query.mTracks )
                res.mTracks = querySearch( query.mQuery, Music.Meta.TRACK );
            if( query.mAlbums )
                res.mAlbums = querySearch( query.mQuery, Music.Meta.ALBUM );
            if( query.mArtists )
                res.mArtists = querySearch( query.mQuery, Music.Meta.ARTIST );
            
            System.out.println("Got artists: " + res.mArtists.getCount());
            System.out.println("Got tracks: " + res.mTracks.getCount());
            return res;
        }
        catch ( SQLiteException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        catch ( IOException e )
        {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    
        
        
        return null;
    }
    
    /**
     * insert several tracks into the playlist queue
     * Note: the song ids are not verified!
     * @param ids the songs ids
     */
    public void insertQueueItems( int[] ids )
    {
        for( int id : ids )
        {
            appendQueueItem( id );
        }
    }
    
    /**
     * appends one track into the queue
     * Note: the song ids are not verified!
     * @param ids the song id
     */
    public void appendQueueItem( int id )
    {
        mDb.execSQL("INSERT INTO current_playlist(track_id) VALUES("+id+")");
    }
    
    /**
     * Insert an entire artist into the playlist.
     * @param id the artist id
     */
    public void insertArtistQueue( int id )
    {
        mDb.execSQL("INSERT INTO current_playlist(track_id) " +
                "SELECT track._id FROM track " +
                "WHERE track.artist_id = " + id);
    }
    
    /**
     * Insert an entire album into the playlist.
     * @param id album id
     */
    public void insertAlbumQueue( int id )
    {
        mDb.execSQL("INSERT INTO current_playlist(track_id) " +
                "SELECT track._id FROM track " +
                "WHERE track.album_id = " + id);
    }
    
    public Cursor getQueueCursor()
    {
        return mDb.query( "track, current_playlist", Music.TRACK, "track._id=current_playlist.track_id", null, null, null, "pos" );   
    }
    
    public int[] getQueue()
    {
        Cursor c = mDb.rawQuery( "select * from current_playlist ORDER BY 'pos'", null );
        
        int size = c.getCount();
        int[] queue = new int[size];

        c.moveToFirst();
        for ( int i = 0; i < size; i++ )
        {
            queue[i] = c.getInt( 1 );
            c.moveToNext();
        }
        c.close();
        return queue;
    }
    
    /**
     * Returns the size of the current playlist
     *
     * @return  size of the playlist or -1 if an error occurs
     */
    public int getQueueSize()
    {
        int size = -1;
        Cursor c = mDb.rawQuery("SELECT COUNT(track_id) FROM current_playlist" ,null);
        if(c.moveToFirst())
        {
            size = c.getInt(0);
        }
        c.close();
        return size;
    }
    
    /**
    * Removes the range of tracks specified from the queue
    * @param first The first track to be removed
    * @param last The last track to be removed
    * @return the number of tracks deleted
    */

    public int removeQueueItem(int first, int last)
    {
        if( last < first )
            return 0;
        int num_deleted = 0;
        while( first < last )
        {
            int res = mDb.delete( "current_playlist", "pos="+first++, null);
            if(res != 0)
                num_deleted++;
        }
        return num_deleted;   
    }
    
    /**
     * Moves the item at index1 to index2.
     * @param index1
     * @param index2
     */
    public void moveQueueItem( int index1, int index2 )
    {
        System.out.println("Move queue item " + index1 + " to " + index2);
        int queue_len = getQueueSize();
        
        //TODO this is incredibly inefficient, ideally this should be replaced
        // with a couple nifty sql queries.
//        Cursor c = mDb.query( "current_playlist", new String[] {"pos", "track_id"}, null, null, null, null, "pos" );
        Cursor c = mDb.rawQuery( "select * from current_playlist ORDER BY 'pos'", null );
        int[] queue = new int[queue_len];
        
        while(c.moveToNext())
        {
            int ididx = c.getColumnIndex( "track_id" );
            int posidx = c.getColumnIndex( "pos" );
            int pos = c.getInt( posidx ) - 1; // sqlite is 1 indexed
            int id =  c.getInt( ididx );
            queue[pos] = id;
        }
        
        if (index1 >= queue_len) {
            index1 = queue_len - 1;
        }
        if (index2 >= queue_len) {
            index2 = queue_len - 1;
        }
        if (index1 < index2) {
            int tmp = queue[index1];
            for (int i = index1; i < index2; i++) {
                queue[i] = queue[i+1];
            }
            queue[index2] = tmp;
        } else if (index2 < index1) {
            int tmp = queue[index1];
            for (int i = index1; i > index2; i--) {
                queue[i] = queue[i-1];
            }
            queue[index2] = tmp;
        }
        c.close();
        clearQueue();
        insertQueueItems( queue );
    }
    
    /**
     * Clear the current playlist
     */
    public void clearQueue()
    {
        mDb.execSQL("DELETE FROM current_playlist");
    }
    
    /**
     * Inserts a track into the database cache
     * @param track
     * @throws IOException
     * @throws SQLiteException
     */
    private void insertTrack( Track track ) throws IOException, SQLiteException
    {
    
        if ( track == null )
        {
            System.out.println( "OMG TRACK NULL" );
            return;
        }
        mDb.execSQL( "BEGIN TRANSACTION" );
        try
        {
            if ( track.getArtistName().length() > 0 )
            {
                ContentValues cv = new ContentValues( 2 );
                cv.put( "_id", track.getArtistId() );
                cv.put( "artist_name", track.getArtistName() );
    
                Cursor c = mDb.query( "artist", Music.ID, "_id='"
                        + track.getArtistId() + "'", null, null, null, null );
                if ( !c.moveToNext() )
                    mDb.insert( "artist", UNKNOWN_STRING, cv );
                c.close();
            }
            /*
             * TODO determine whether the fancy ContentValues means of
             * performing queries is faster than the regular rawQuery + string a
             * concatentation method.
             */
            if ( track.getAlbumTitle().length() > 0 )
            {
                ContentValues cv = new ContentValues( 2 );
                cv.put( "_id", track.getAlbumId() );
                cv.put( "album_name", track.getAlbumTitle() );
                cv.put( "artist_id", track.getArtistId() );
                Cursor c = mDb.query( "album", Music.ID, "_id='" + track.getAlbumId()
                        + "'", null, null, null, null );
                // Cursor c =
                // mDb.rawQuery("SELECT _id FROM album WHERE _id='"+track.getAlbumId()+"'"
                // ,null);
                if ( !c.moveToNext() )
                    mDb.insert( "album", UNKNOWN_STRING, cv );
                // mDb.execSQL("INSERT INTO album(_id, album_name) VALUES("+track.getAlbumId()+", '"+track.getAlbumTitle()+"')");
                c.close();
            }
    
            Cursor c = mDb.query( "track", Music.ID, "_id='" + track.getId() + "'",
                    null, null, null, null );
            if ( !c.moveToNext() )
            {
                ContentValues cv = new ContentValues( 7 );
                cv.put( "_id", track.getId() );
                cv.put( "play_url", track.getPlayUrl() );
                cv.put( "download_url", track.getDownloadUrl() );
                cv.put( "title", track.getTitle() );
                cv.put( "track", track.getNumber() );
                cv.put( "artist_name", track.getArtistName() );
                cv.put( "album_name", track.getAlbumTitle() );
                cv.put( "artist_id", track.getArtistId() );
                cv.put( "album_id", track.getAlbumId() );
                cv.put( "track_length", track.getDuration() );
                cv.put( "cover_url", track.getAlbumArt() );
                mDb.insert( "track", UNKNOWN_STRING, cv );
            }
            c.close();
    
            mDb.execSQL( "COMMIT TRANSACTION" );
    
        }
        catch ( SQLiteException e )
        {
            mDb.execSQL( "ROLLBACK" );
            throw e;
        }
    }

    /**
     * 
     * @param artist
     * @throws IOException
     * @throws SQLiteException
     */
    private void insertArtist( Artist artist) throws IOException, SQLiteException
    {
        if ( artist == null )
        {
            System.out.println( "OMG Artist NULL" );
            return;
        }
        try
        {
            if ( artist.getName().length() > 0 )
            {
                ContentValues cv = new ContentValues( 2 );
                cv.put( "_id", artist.getId() );
                cv.put( "artist_name", artist.getName() );
                cv.put( "album_count", artist.getAlbumCount() );
                cv.put( "track_count", artist.getTrackCount() );
    
                Cursor c = mDb.query( "artist", Music.ID, "_id='"
                        + artist.getId() + "'", null, null, null, null );
                if ( !c.moveToNext() ) // artist doesn't exist
                    mDb.insert( "artist", UNKNOWN_STRING, cv );
                else // artist exists, so lets update with new data
                {
                    cv.remove( "_id" );
                    mDb.update( "artist", cv, "_id='" + artist.getId() + "'", null );
                }
                c.close();
            }
        }
        catch ( SQLiteException e )
        {
            throw e;
        }
    }

    private void insertAlbum( Album album) throws IOException, SQLiteException
    {
        if ( album == null )
        {
            System.out.println( "OMG Album NULL" );
            return;
        }
        try
        {
            if ( album.getName().length() > 0 )
            {
                ContentValues cv = new ContentValues( 2 );
                cv.put( "_id", album.getId() );
                cv.put( "album_name", album.getName() );
                cv.put( "artist_name", album.getArtistName() );
                cv.put( "artist_id", album.getArtistId() );
                cv.put( "year", album.getYear() );
                cv.put( "track_count", album.getTrackCount() );
    
                Cursor c = mDb.query( "album", Music.ID, "_id='"
                        + album.getId() + "'", null, null, null, null );
    
                if ( !c.moveToNext() ) // album doesn't exist
                    mDb.insert( "album", UNKNOWN_STRING, cv );
                else // album exists, so lets update with new data
                {
                    cv.remove( "_id" );
                    mDb.update( "album", cv, "_id='" + album.getId() + "'", null );
                }
    
                c.close();
            }
        }
        catch ( SQLiteException e )
        {
            throw e;
        }
    }

    private void insertPlaylist( Playlist playlist) throws IOException, SQLiteException
    {
        if ( playlist == null )
        {
            System.out.println( "OMG Playlist NULL" );
            return;
        }
        try
        {
            if ( playlist.getName().length() > 0 )
            {
                ContentValues cv = new ContentValues( 2 );
                cv.put( "_id", playlist.getId() );
                cv.put( "playlist_name", playlist.getName() );
                cv.put( "file_count", playlist.getCount() );
                cv.put( "file_name", playlist.getFileName() );
    
                Cursor c = mDb.query( "playlist", Music.ID, "_id='"
                        + playlist.getId() + "'", null, null, null, null );
    
                if ( !c.moveToNext() ) // album doesn't exist
                    mDb.insert( "playlist", UNKNOWN_STRING, cv );
                else // album exists, so lets update with new data
                {
                    cv.remove( "_id" );
                    mDb.update( "playlist", cv, "_id='" + playlist.getId() + "'", null );
                }
    
                c.close();
            }
        }
        catch ( SQLiteException e )
        {
            throw e;
        }
    }

    private void insertToken( Token token, String type ) throws IOException, SQLiteException
    {
        if ( token == null )
        {
            System.out.println( "OMG Token NULL" );
            return;
        }
        try
        {
            if ( token.getToken().length() > 0 )
            {
                ContentValues cv = new ContentValues( 2 );
                cv.put( "type", type );
                cv.put( "token", token.getToken() );
                cv.put( "count", token.getCount() );
    
                Cursor c = mDb.query( "token", new String[] { "token" }, "type='"
                        + type + "' AND token='"+token.getToken()+"'", null, null, null, null );
    
                if ( !c.moveToNext() ) // album doesn't exist
                    mDb.insert( "token", UNKNOWN_STRING, cv );
                else // album exists, so lets update with new data
                {
                    mDb.update( "token", cv, "type='" + type + "' AND token='"+token.getToken()+"'", null );
                }
    
                c.close();
            }
        }
        catch ( SQLiteException e )
        {
            throw e;
        }
    }

    private void refreshTracks() throws SQLiteException, IOException
    {
        DataResult<Track> results = mLocker.getTracks();
        System.out.println( "beginning insertion of " + results.getData().length + " tracks" );
        for( Track t : results.getData() )
        {
            insertTrack( t );
        }
        System.out.println( "insertion complete" );
        mCache.setUpdate( System.currentTimeMillis(), LockerCache.TRACK );
        mCache.saveCache( mContext );
    }
    
    private void refreshPlaylists()  throws SQLiteException, IOException
    {
        DataResult<Playlist> results = mLocker.getPlaylists();
        System.out.println( "beginning insertion of " + results.getData().length + " playlists" );
        for ( Playlist p : results.getData() )
        {
            insertPlaylist( p );
        }
        System.out.println( "insertion complete" );
        mCache.setUpdate( System.currentTimeMillis(), LockerCache.PLAYLIST );
        mCache.saveCache( mContext );
    }
    
    private void refreshArtists()  throws SQLiteException, IOException
    {
        DataResult<Artist> results = mLocker.getArtists();
        System.out.println( "beginning insertion of " + results.getData().length + " artists" );
        for ( Artist a : results.getData() )
        {
            insertArtist( a );
        }
        System.out.println( "insertion complete" );
        mCache.setUpdate( System.currentTimeMillis(), LockerCache.ARTIST );
        mCache.saveCache( mContext );
    }
    
    private void refreshAlbums()  throws SQLiteException, IOException
    {
        DataResult<Album> results = mLocker.getAlbums();
        System.out.println( "beginning insertion of " + results.getData().length + " albums" );
        for ( Album a : results.getData() )
        {
            insertAlbum( a );
        }
        System.out.println( "insertion complete" );
        mCache.setUpdate( System.currentTimeMillis(), LockerCache.ALBUM );
        mCache.saveCache( mContext );
    }
    
    private void refreshAlbumsForArtist(int artist_id)  throws SQLiteException, IOException
    {
        DataResult<Album> results = mLocker.getAlbumsForArtist( artist_id );
        System.out.println( "beginning insertion of " + results.getData().length + " albums for artist id " +artist_id  );
        for ( Album a : results.getData() )
        {
            insertAlbum( a );
        }
        System.out.println( "insertion complete" );
    }
    
    private void refreshTracksforAlbum(int album_id)  throws SQLiteException, IOException
    {
        DataResult<Track> results = mLocker.getTracksForAlbum( album_id );
        System.out.println( "beginning insertion of " + results.getData().length + " tracks for album id " +album_id );
        for( Track t : results.getData() )
        {
            insertTrack( t );
        }
        System.out.println( "insertion complete" );
    }
    
    private void refreshTracksforArtist(int artist_id)  throws SQLiteException, IOException
    {
        DataResult<Track> results = mLocker.getTracksForArtist( artist_id );
        System.out.println( "beginning insertion of " + results.getData().length + " tracks for artist id " +artist_id );
        for( Track t : results.getData() )
        {
            insertTrack( t );
        }
        System.out.println( "insertion complete" );
    }
    
    private void refreshTracksforPlaylist(int playlist_id)  throws SQLiteException, IOException
    {
        DataResult<Track> results = mLocker.getTracksForPlaylist( playlist_id );
        System.out.println( "beginning insertion of " + results.getData().length + " tracks for playlist id " +playlist_id );
        
        mDb.delete( "playlist_tracks", "playlist_id=" + playlist_id, null );
        for ( Track t : results.getData() )
        {
            ContentValues cv = new ContentValues(); // TODO move this outside the loop?
            insertTrack( t );
            cv.put("playlist_id", playlist_id);
            cv.put( "track_id", t.getId() );
            mDb.insert( "playlist_tracks", UNKNOWN_STRING, cv );
        }
        System.out.println( "insertion complete" );
    }
    
    private void refreshSearch( String query, boolean artist, boolean album, boolean track )  throws SQLiteException, IOException
    {
        if( !artist && !album && !track )
            return;
        SearchResult results = mLocker.search( query, artist, album, track, 50, 0 );
        
        if( artist ) 
        {
            System.out.println( "beginning insertion of " + results.getArtists().length + " artists" );
            for ( Artist a : results.getArtists() )
            {
                insertArtist( a );
            }
        }
        if( album ) 
        {
            System.out.println( "beginning insertion of "+  results.getAlbums().length + " albums" );
            for ( Album  a : results.getAlbums() )
            {
                insertAlbum( a );
            }
        }
        if( track ) 
        {
            System.out.println( "beginning insertion of "+  results.getTracks().length + " tracks" );
            for ( Track t : results.getTracks() )
            {
                insertTrack( t );
            }
        }
        System.out.println( "insertion complete" );
    }
    
    private void refreshTokens(Music.Meta type)  throws SQLiteException, IOException
    {
        DataResult<Token> results;
        String typename;
        int cachetype;
        switch( type )
        {
            case ARTIST:
                results = mLocker.getArtistTokens();
                typename = "artist";
                cachetype = LockerCache.ARTIST_TOKENS; 
                break;
            case ALBUM:
                results = mLocker.getAlbumTokens();
                typename = "album";
                cachetype = LockerCache.ALBUM_TOKENS; 
                break;
            case TRACK:
                results = mLocker.getTrackTokens();
                typename = "track";
                cachetype = LockerCache.TRACK_TOKENS; 
                break;
            default:
                    return;
        }
        System.out.println( "beginning insertion of " + results.getData().length + " tokens" );
        for ( Token t : results.getData() )
        {
            insertToken( t, typename );
        }
        System.out.println( "insertion complete" );
        mCache.setUpdate( System.currentTimeMillis(), cachetype );
        mCache.saveCache( mContext );
    }
    
    private Cursor queryTokens( String type )
    {
        return mDb.query( "token", Music.TOKEN, "type='"+type+"'", null, null, null, Music.TOKEN[1] );   
    }

    private Cursor queryPlaylists()
    {
        return mDb.query( "playlist", Music.PLAYLIST, null, null, null, null, "lower("+Music.PLAYLIST[1]+")" );   
    }

    private Cursor queryPlaylists( int playlist_id )
        {
            String selection = "track._id _id, title, artist_name, artist_id, album_name, album_id, track, play_url, download_url, track_length, cover_url";
    //        String table = "playlist " + "JOIN playlist_tracks ON playlist._id = playlist_tracks.playlist_id "
    //                    + "JOIN track ON playlist_tracks.track_id = track._id" +
    //                            "";  
    //        String where =  "playlist_id="+playlist_id;
    //        return mDb.query( true, table, Music.TRACK, where, null, null, null, null, null );
            return mDb.rawQuery( 
                    "SELECT DISTINCT " + selection + " FROM playlist " +
                    "JOIN playlist_tracks ON playlist._id = playlist_tracks.playlist_id " +
                    "JOIN track ON playlist_tracks.track_id = track._id " +
                    "WHERE playlist_id="+playlist_id, null );
       
        }

    private Cursor queryArtists()
    {
        return mDb.query( "artist", Music.ARTIST, null, null, null, null, "lower("+Music.ARTIST[1]+")" );   
    }

    private Cursor queryAlbums()
    {
        return mDb.query( "album", Music.ALBUM, null, null, null, null, "lower("+Music.ALBUM[Music.ALBUM_MAPPING.ALBUM_NAME]+")" );
    }

    private Cursor queryAlbums( int artist_id )
    {
        return  mDb.query( "album", Music.ALBUM, "artist_id=" + artist_id,
                null, null, null, "lower("+Music.ALBUM[Music.ALBUM_MAPPING.ALBUM_NAME]+")" );  
    }

    private Cursor queryTracks()
    {
        return mDb.query( "track", Music.TRACK, null, null, null, null, "lower("+Music.TRACK[Music.TRACK_MAPPING.TITLE]+")" );
    }

    private Cursor queryTracksArtist( int artist_id )
    {
        return mDb.query( "track", Music.TRACK, "artist_id=" + artist_id, null, null, null, "lower("+Music.TRACK[Music.TRACK_MAPPING.TRACKNUM]+")" );
    }

    private Cursor queryTracksAlbum( int album_id )
    {
        return mDb.query( "track", Music.TRACK, "album_id=" + album_id, null, null, null, "lower("+Music.TRACK[Music.TRACK_MAPPING.TRACKNUM]+")" );
    }

    private Cursor querySearch( String query, Music.Meta type )
        {
            String table;
            String[] columns;
            String selection;
    //        String sort;
            switch ( type )
            {
                case TRACK:
                    table = "track";
                    columns = Music.TRACK;
                    selection = "lower(title) LIKE lower('%"+query+"%')";
    //                selection = "MATCH (title) AGAINST ('+"+query+"' IN BOOLEAN MODE)";
                    break;
                case ARTIST:
                    table = "artist";
                    columns = Music.ARTIST;
    //                selection = "MATCH (artist_name) AGAINST ('+"+query+"' IN BOOLEAN MODE)";
                    selection = "lower(artist_name) LIKE lower('%"+query+"%')";
                    break;
                case ALBUM:
                    table = "album";
                    columns = Music.ALBUM;
                    selection = "lower(album_name) LIKE lower('%"+query+"%')";
    //                selection = "MATCH (album_name) AGAINST ('+"+query+"' IN BOOLEAN MODE)";
                    break;
                default: return null;
            }
            return mDb.query( table, columns, selection, null, null, null, null, null);
        }
    
    private Bitmap getArtwork( String urlstr )
    {

        try
        {
            URL url;

            url = new URL( urlstr );

            HttpURLConnection c = ( HttpURLConnection ) url.openConnection();
            c.setDoInput( true );
            c.connect();
            InputStream is = c.getInputStream();
            Bitmap img;
            img = BitmapFactory.decodeStream( is );
            return img;
        }
        catch ( MalformedURLException e )
        {
            Log.d( "LockerArtCache", "LockerArtCache passed invalid URL: " + urlstr );
        }
        catch ( IOException e )
        {
            Log.d( "LockerArtCache", "LockerArtCache IO exception: " + e );
        }
        return null;
    }
    
    public static String[] tokensToString( Token[] tokens )
    {
        String[] list  = new String[tokens.length];
        for ( int i = 0; i < tokens.length; i++ )
        {
            list[i] = tokens[i].getToken();
        }
        return list;
    }
    
    public class DbSearchResult
    {
        public Cursor mArtists = null;
        public Cursor mAlbums = null;
        public Cursor mTracks = null;
    }
    
    public class DbSearchQuery
    {
        public DbSearchQuery( String query, boolean artists, boolean albums, boolean tracks )
        {
            mQuery = query;
            mArtists = artists;
            mAlbums = albums;
            mTracks = tracks;
        }
        public String mQuery;
        public boolean mArtists;
        public boolean mAlbums;
        public boolean mTracks;
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
            db.execSQL( "CREATE TABLE track(" + "_id INTEGER PRIMARY KEY," + "play_url VARCHAR,"
                    + "download_url VARCHAR," + "title VARCHAR," + "track NUMBER(2) DEFAULT 0,"
                    + "artist_id INTEGER," + "artist_name VARCHAR," + "album_id INTEGER,"
                    + "album_name VARCHAR," + "track_length," + "cover_url VARCHAR DEFAULT NULL" + ")" );
            db.execSQL( "CREATE TABLE artist(" + "_id INTEGER PRIMARY KEY,"
                    + "artist_name VARCHAR," + "album_count INTEGER," + "track_count INTEGER"
                    + " )" );
            db.execSQL( "CREATE TABLE album(" + "_id INTEGER PRIMARY KEY," + "album_name VARCHAR, "
                    + "artist_id INTEGER," + "artist_name VARCHAR," + "track_count INTEGER,"
                    + "year INTEGER," + "cover_url VARCHAR DEFAULT NULL" + ")" );
            db.execSQL( "CREATE TABLE playlist(" + "_id INTEGER PRIMARY KEY," + "playlist_name VARCHAR, "
                    + "file_count INTEGER," + "file_name VARCHAR" + ")" );
            db.execSQL( "CREATE TABLE playlist_tracks(" + "playlist_id INTEGER," + "track_id INTEGER" + ")" );
            db.execSQL( "CREATE TABLE token(" + "type VARCHAR," + "token VARCHAR," + "count INTEGER" + ")" );
            db.execSQL( "CREATE TABLE current_playlist(" + "pos INTEGER PRIMARY KEY,"
                    + "track_id INTEGER" + ")" );

        }

        @Override
        public void onUpgrade( SQLiteDatabase db, int oldV, int newV )
        {
            db.execSQL( "DROP TABLE IF EXISTS current_playlist" );
            db.execSQL( "DROP TABLE IF EXISTS album" );
            db.execSQL( "DROP TABLE IF EXISTS artist" );
            db.execSQL( "DROP TABLE IF EXISTS track" );
            db.execSQL( "DROP TABLE IF EXISTS playlist" );
            db.execSQL( "DROP TABLE IF EXISTS playlist_tracks" );
            db.execSQL( "DROP TABLE IF EXISTS token" );
            onCreate( db );
        }

        public SQLiteDatabase getWritableDatabase()
        {
            return super.getWritableDatabase();

        }

    }

}
