/***************************************************************************
 *   Copyright 2008 Casey Link <unnamedrambler@gmail.com>                  *
 *   Copyright (C) 2007-2008 sibyl project http://code.google.com/p/sibyl/ *
 *   Copyright 2005-2009 Last.fm Ltd.                                      *
 *   Portions contributed by Casey Link, Lukasz Wisniewski,                *
 *   Mike Jennings, and Michael Novak Jr.                                  *
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
package com.mp3tunes.android.service;

import java.io.IOException;
import java.util.Stack;
import java.util.Timer;
import java.util.TimerTask;

import com.binaryelysium.mp3tunes.api.Locker;
import com.binaryelysium.mp3tunes.api.Track;
import com.mp3tunes.android.LockerDb;
import com.mp3tunes.android.MP3tunesApplication;
import com.mp3tunes.android.Music;
import com.mp3tunes.android.R;
import com.mp3tunes.android.activity.Login;
import com.mp3tunes.android.activity.Player;
import com.mp3tunes.android.util.UserTask;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.PowerManager.WakeLock;
import android.preference.PreferenceManager;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class Mp3tunesService extends Service
{

    private static final int NOTIFY_ID = 10911251; // mp3 in ascii
    private MediaPlayer mMp = new MediaPlayer();
    private MediaPlayer mNextMp = null;
    private LockerDb mDb;
    private int mShuffleMode = Music.ShuffleMode.NORMAL;
    private int mRepeatMode = Music.RepeatMode.NO_REPEAT;
    private int mBufferPercent;
    private Locker mLocker;
    private int mServiceState = STATE.STOPPED;
    private Stack<Integer> mTrackHistory;
    
    // the index of the CurrentTrack in the playlist
    private int mCurrentPosition = 0;
    
    // the current track in the playlist
    private Track mCurrentTrack = null; 
    
    // the next track to be played (used to buffer in the background) 
    // NOTE: isn't always necessarily the next track
    private Track mNextTrack = null;
    
    // the index of the next Track in the playlist
    private int mNextPosition = 1;
    
    private NotificationManager mNm = null;
    private WifiLock mWifiLock;
    private WakeLock mWakeLock;
    private boolean mNextPrepared = false;
    private int mAutoSkipCount = 0;
    private Bitmap mAlbumArt;

    private static class STATE
    {

        private final static int STOPPED = 0;
        private final static int PREPARING = 1;
        private final static int PLAYING = 2;
        private final static int SKIPPING = 3;
        private final static int PAUSED = 4;
    }
    
    public static final String META_CHANGED = "com.mp3tunes.android.metachanged";
    public static final String PLAYBACK_FINISHED = "com.mp3tunes.android.playbackcomplete";
    public static final String PLAYBACK_STATE_CHANGED = "com.mp3tunes.android.playstatechanged";
    public static final String PLAYBACK_ERROR = "com.mp3tunes.android.playbackerror";
    public static final String DATABASE_ERROR = "com.mp3tunes.android.databaseerror";
    public static final String UNKNOWN = "com.mp3tunes.android.unknown";

    /**
     * Tracks whether there are activities currently bound to the service so
     * that we can determine when it would be safe to call stopSelf().
     */
    boolean mActive = false;

    /**
     * Used for pausing on incoming call
     */
    private TelephonyManager mTelephonyManager;

    @Override
    public void onCreate()
    {
        super.onCreate();
        mNm = ( NotificationManager ) getSystemService( NOTIFICATION_SERVICE );
        mBufferPercent = 0;
        
        //we dont want the service to be killed while playing
        setForeground( true );
        // we dont want to sleep while playing
//        mMp.setScreenOnWhilePlaying( true ); 

        // We want to keep the wifi enabled while playing, because
        // all our playing is done via streaming
        
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MP3tunes Player");
        
        WifiManager wm = ( WifiManager ) getSystemService( Context.WIFI_SERVICE );
        mWifiLock = wm.createWifiLock( "MP3tunes Player" );

        mTelephonyManager = ( TelephonyManager ) this.getSystemService( Context.TELEPHONY_SERVICE );
        mTelephonyManager.listen( mPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE );
        
        mTrackHistory = new Stack<Integer>();
        
        mLocker = ( Locker ) MP3tunesApplication.getInstance().map.get( "mp3tunes_locker" );
        
        try
        // establish a connection with the database
        {
            mDb = new LockerDb( this, null ); // TODO fix this, we dont want to pass a null locker

        }
        catch ( Exception ex )
        {
            // database connection failed.
            // Show an error and exit gracefully.
            System.out.println( ex.getMessage() );
            ex.printStackTrace();
            mServiceState = STATE.STOPPED;
            notifyChange(DATABASE_ERROR);
            mNm.cancel( NOTIFY_ID );
             if( mWakeLock.isHeld())
                 mWakeLock.release();

            if ( mWifiLock.isHeld() )
                mWifiLock.release();
            
        }
    }

    PhoneStateListener mPhoneStateListener = new PhoneStateListener()
    {

        private FadeVolumeTask mFadeVolumeTask = null;

        @Override
        public void onCallStateChanged( int state, String incomingNumber )
        {
            if ( mFadeVolumeTask != null )
                mFadeVolumeTask.cancel();

            if ( state == TelephonyManager.CALL_STATE_IDLE ) // fade music in to
            // 100%
            {
                mFadeVolumeTask = new FadeVolumeTask( FadeVolumeTask.FADE_IN, 5000 )
                {

                    @Override
                    public void onPreExecute()
                    {
                        if ( mServiceState == STATE.PAUSED )
                            Mp3tunesService.this.pause();
                    }

                    @Override
                    public void onPostExecute()
                    {
                        mFadeVolumeTask = null;
                    }
                };
            }
            else
            { // fade music out to silence
                if ( mServiceState == STATE.PAUSED )
                {
                    // this particular state of affairs should be impossible,
                    // seeing as we are the only
                    // component that dares the pause the radio. But we cater to
                    // it just in case
                    mMp.setVolume( 0.0f, 0.0f );
                    return;
                }

                // fade out faster if making a call, this feels more natural
                int duration = state == TelephonyManager.CALL_STATE_RINGING ? 3000 : 1500;

                mFadeVolumeTask = new FadeVolumeTask( FadeVolumeTask.FADE_OUT, duration )
                {

                    @Override
                    public void onPreExecute()
                    {}

                    @Override
                    public void onPostExecute()
                    {
                        Mp3tunesService.this.pause();
                        mFadeVolumeTask = null;
                    }
                };
            }
            super.onCallStateChanged( state, incomingNumber );
        }
    };

    @Override
    public void onDestroy()
    {

        mMp.stop();
        mMp.release();
        mNm.cancel( NOTIFY_ID );
    }

    @Override
    public IBinder onBind( Intent arg0 )
    {
        mActive = true;
        if(mServiceState != STATE.STOPPED) {
            notifyChange(META_CHANGED);
        }
        return mBinder;
    }
    
    @Override
    public void onRebind( Intent intent )
    {

        mActive = true;
        mDeferredStopHandler.cancelStopSelf();
        if(mServiceState != STATE.STOPPED) {
            notifyChange(META_CHANGED);
        }
        super.onRebind( intent );
    }
    
    @Override
    public boolean onUnbind( Intent intent )
    {

        mActive = false;

        if ( mServiceState != STATE.STOPPED ) // || mResumeAfterCall == true)
            return true;

        mDeferredStopHandler.deferredStopSelf();
        return true;
    }

    private void playingNotify()
    {

         if ( mCurrentTrack == null )
             return;
         Notification notification = new Notification(
         R.drawable.logo_statusbar, "Playing: "
                    + mCurrentTrack.getTitle() + " by " + mCurrentTrack.getArtistName(), System
                    .currentTimeMillis() );
            PendingIntent contentIntent = PendingIntent.getActivity( this, 0,
         new Intent( this, Player.class ), 0 );
         String info = mCurrentTrack.getTitle();
         String name = mCurrentTrack.getArtistName();
         notification.setLatestEventInfo( this, name, info, contentIntent );
         notification.flags |= Notification.FLAG_ONGOING_EVENT;
        
         mNm.notify( NOTIFY_ID, notification );
    }

    private OnCompletionListener mOnCompletionListener = new OnCompletionListener()
    {

        public void onCompletion( MediaPlayer mp )
        {
            new NextTrackTask().execute((Void)null);
        }
    };

    private OnBufferingUpdateListener mOnBufferingUpdateListener = new OnBufferingUpdateListener()
    {

        public void onBufferingUpdate( MediaPlayer mp, int percent )
        {

         mBufferPercent = percent;
         if(mNextMp == null && percent == 100) 
         {
             // Check if we're running low on tracks
             if(mDb.getPlaylistSize()  > mCurrentPosition ) 
             {
                 mNextPrepared = false;
                 mNextMp = new MediaPlayer();
                 mNextPosition = mCurrentPosition + 1;
                 mNextTrack = mDb.getTrackPlaylist( mNextPosition );
                 playTrack( mNextTrack, mNextPosition ,mNextMp );
             }
         }
        }
    };

    private OnErrorListener mOnErrorListener = new OnErrorListener()
    {

        public boolean onError( MediaPlayer mp, int what, int extra )
        {
            new RefreshSessionTask().execute((Void)null);
            // Likely it failed because the session is invalid so lets  
            // get a new session.
            return true;
        }
    };

    private OnPreparedListener mOnPreparedListener = new OnPreparedListener()
    {

        public void onPrepared( MediaPlayer mp )
        {
            if ( mp == Mp3tunesService.this.mMp )
            {
                if ( mServiceState == STATE.PREPARING )
                {
                    mp.start();
                    playingNotify();
                    mServiceState = STATE.PLAYING;
                    mAutoSkipCount = 0;
                    notifyChange( META_CHANGED );
                }
                else
                {
                    mp.stop();
                }
            }
            else
            {
                mNextPrepared = true;
            }
        }
    };

    private void playTrack( Track track, int playlist_index, MediaPlayer p )
    {
        try
        {
            if (/*mServiceState == STATE.STOPPED ||*/ mServiceState == STATE.PREPARING || track == null )
                return;
            
            if(p == mMp) {
                if( mCurrentTrack != null )
                    mTrackHistory.add( mCurrentTrack.getId() );
                mCurrentTrack = track;
                mCurrentPosition = playlist_index;
                mAlbumArt = null;
            }
            
            int bitrate = chooseBitrate();
            String url = track.getPlayUrl() + "&bitrate=" + bitrate;
            Log.i( "MP3tunes", "Streaming: " + url );
            p.reset();
            url = updateUrlSession(url);
            p.setDataSource( url );
            p.setOnCompletionListener( mOnCompletionListener );
            p.setOnBufferingUpdateListener( mOnBufferingUpdateListener );
            p.setOnPreparedListener( mOnPreparedListener );
            p.setOnErrorListener( mOnErrorListener );
            
            mDeferredStopHandler.cancelStopSelf();

            // We do this because there has been bugs in our phonecall fade code
            // that resulted in the music never becoming audible again after a call.
            // Leave this precaution here please.
            p.setVolume( 1.0f, 1.0f );
            
            if(p == mMp)
                mServiceState = STATE.PREPARING;
            p.prepareAsync();
        }
        catch ( IllegalStateException e )
        {
            Log.e( getString( R.string.app_name ), e.toString() );
        }
        catch ( IOException e )
        {
            Log.e( getString( R.string.app_name ), e.getMessage() );
        }
    }
    
    private String updateUrlSession( String url )
    {
        String sid = mLocker.getCurrentSession().getSessionId();
        url = url.replaceFirst( "sid=(.*?)&", "sid="+sid+"&" );
        System.out.println("fixed url: " + url);
        return url;
    }
    
    private int chooseBitrate()
    {
        int bitrate = PreferenceManager.getDefaultSharedPreferences(this).getInt( "bitrate", -1 );
        
        if( bitrate == -1 )
        {
            int[] vals = getResources().getIntArray( R.array.rate_values );
            
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE); 
            int type = cm.getActiveNetworkInfo().getType();
            if(type == ConnectivityManager.TYPE_WIFI)
            {
                bitrate = vals[5]; // 5 = 192000 // TODO this shouldn't be harcoded
            } 
            else if(type == ConnectivityManager.TYPE_MOBILE) 
            {
                TelephonyManager tm = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

                switch (tm.getNetworkType())
                {
                    case TelephonyManager.NETWORK_TYPE_UNKNOWN:
                    case TelephonyManager.NETWORK_TYPE_GPRS:
                        bitrate = vals[3];
                        break;
                    case TelephonyManager.NETWORK_TYPE_EDGE:
                    case TelephonyManager.NETWORK_TYPE_UMTS:
                        bitrate = vals[4];
                        break;
                } 
            }
        }
        return bitrate;
    }
    
    private void nextTrack()
    {
        System.out.println("next track called");
        if(mServiceState == STATE.SKIPPING || mServiceState == STATE.STOPPED)
            return;
        
        mServiceState = STATE.SKIPPING;
        
        if(mNextMp != null) {
            System.out.println("Next mp != null");
            mMp.stop();
            mMp = mNextMp;
            mNextMp = null;
            mServiceState = STATE.PREPARING;
            mCurrentTrack = mNextTrack;
            mCurrentPosition = mNextPosition;
            mNextTrack = null;
            mNextPosition = -1;
            mAlbumArt = null;
            if( mNextPrepared )
                mOnPreparedListener.onPrepared(mMp);

            notifyChange( META_CHANGED );
            return;
        }
        
        // Check again, if size still == 0 then the playlist is empty.
        if ( mDb.getPlaylistSize() > mCurrentPosition )
        {
            //playTrack will check if mStopping is true, and stop us if the user has
            //pressed stop while we were fetching the playlist
            int pos = mCurrentPosition + 1;
            System.out.println("preparing next track");
            Track track = mDb.getTrackPlaylist( pos);
            System.out.println( "track: " + track.getTitle() + " by " + track.getArtistName() );
            playTrack( track, pos, mMp );
            notifyChange( META_CHANGED );
        }
        else
        {
            // playlist finished
            notifyChange( PLAYBACK_FINISHED );
            mNm.cancel( NOTIFY_ID );
            if( mWakeLock.isHeld() )
                mWakeLock.release();

           if ( mWifiLock.isHeld() )
               mWifiLock.release();
        }
    }
    
    private void pause()
    {
        if ( mServiceState == STATE.STOPPED )
            return;

        if ( mServiceState != STATE.PAUSED)
        {
            if ( mActive == false )
                mDeferredStopHandler.deferredStopSelf();
            Notification notification = new Notification(
                    R.drawable.right_play, "MP3tunes Paused", System
                    .currentTimeMillis() );
            PendingIntent contentIntent = PendingIntent.getActivity( this, 0,
                    new Intent( this, Player.class ), 0 );
            String info;
            String name;
            if ( mCurrentTrack != null )
            {
                name = mCurrentTrack.getArtistName();
                info = mCurrentTrack.getTitle();
            }
            else
            {
                name = "Paused";
                info = mCurrentTrack.getTitle() + " by \n"
                + mCurrentTrack.getArtistName();
            }
            notification.setLatestEventInfo( this, name, info, contentIntent );
//            notification.flags |= Notification.FLAG_ONGOING_EVENT;
            mNm.cancel( NOTIFY_ID );
            mNm.notify( NOTIFY_ID, notification );
            mMp.pause();
            mServiceState = STATE.PAUSED;
            notifyChange( PLAYBACK_STATE_CHANGED );
        }
        else
        {
            playingNotify();
            mMp.start();
            mServiceState = STATE.PLAYING;
            notifyChange( PLAYBACK_STATE_CHANGED );
        }
    }

    private void stop()
    {
        if ( mServiceState == STATE.PLAYING )
        {
            mMp.stop();
        }
        mNm.cancel( NOTIFY_ID );
        mServiceState = STATE.STOPPED;
        Mp3tunesService.this.notifyChange(PLAYBACK_FINISHED);
         if( mWakeLock.isHeld() )
             mWakeLock.release();

        if ( mWifiLock.isHeld() )
            mWifiLock.release();
        mDeferredStopHandler.deferredStopSelf();
    }

    boolean setCurrentPosition( int msec )
    {
        if ( mServiceState != STATE.PLAYING && mServiceState != STATE.PAUSED )
            return false;

        mMp.seekTo( msec );
        // because when we move to an other pos the music starts
        if ( mServiceState == STATE.PAUSED )
            mMp.pause();
        return true;

    }
    
    private void notifyChange( String what )
    {

        Intent i = new Intent( what );
        if ( mCurrentTrack != null )
        {
            i.putExtra( "artist", mCurrentTrack.getArtistName() );
            i.putExtra( "album", mCurrentTrack.getAlbumTitle() );
            i.putExtra( "track", mCurrentTrack.getTitle() );
            i.putExtra( "duration", mCurrentTrack.getDuration());
            i.putExtra( "id", mCurrentTrack.getId());
        }
        sendBroadcast( i );
    }

    /**
     * Deferred stop implementation from the five music player for android:
     * http://code.google.com/p/five/ (C) 2008 jasta00
     */
    private final DeferredStopHandler mDeferredStopHandler = new DeferredStopHandler();

    private class DeferredStopHandler extends Handler
    {

        /* Wait 1 minute before vanishing. */
        public static final long DEFERRAL_DELAY = 1 * ( 60 * 1000 );

        private static final int DEFERRED_STOP = 0;

        public void handleMessage( Message msg )
        {

            switch ( msg.what )
            {
            case DEFERRED_STOP:
                stopSelf();
                break;
            default:
                super.handleMessage( msg );
            }
        }

        public void deferredStopSelf()
        {

            Log.i( "Mp3tunes", "Service stop scheduled " + ( DEFERRAL_DELAY / 1000 / 60 )
                    + " minutes from now." );
            sendMessageDelayed( obtainMessage( DEFERRED_STOP ), DEFERRAL_DELAY );
        }

        public void cancelStopSelf()
        {

            if ( hasMessages( DEFERRED_STOP ) == true )
            {
                Log.i( "Mp3tunes", "Service stop cancelled." );
                removeMessages( DEFERRED_STOP );
            }
        }
    };

    private final ITunesService.Stub mBinder = new ITunesService.Stub()
    {

        public int getBufferPercent() throws RemoteException
        {
            
            return mBufferPercent;
        }

        public long getDuration() throws RemoteException
        {
            if ( mServiceState != STATE.PLAYING && mServiceState != STATE.PAUSED )
                return 0;
            else
                return mMp.getDuration();
        }

        public long getPosition() throws RemoteException
        {
            if ( mServiceState != STATE.PLAYING && mServiceState != STATE.PAUSED )
                return 0;
            else
                return mMp.getCurrentPosition();
        }

        public int getRepeatMode() throws RemoteException
        {
            return mRepeatMode;
        }

        public int getShuffleMode() throws RemoteException
        {
            return mShuffleMode;
        }

        public boolean isPlaying() throws RemoteException
        {
            return mServiceState != STATE.STOPPED;
        }

        public void next() throws RemoteException
        {
            nextTrack();
        }

        public void pause() throws RemoteException
        {
            Mp3tunesService.this.pause();
        }

        public void prev() throws RemoteException
        {
        // TODO Auto-generated method stub

        }

        public boolean setPosition( int msec ) throws RemoteException
        {
            return setCurrentPosition( msec );
        }

        public void setRepeatMode( int mode ) throws RemoteException
        {
            mRepeatMode = mode;
        }

        public void setShuffleMode( int mode ) throws RemoteException
        {
            mShuffleMode = mode;
        }

        public void start() throws RemoteException
        { 
            if( mDb.getPlaylistSize() <= 0 )
                throw new RemoteException();
            playTrack( mDb.getTrackPlaylist( 1 ), 1, mMp );
            notifyChange( META_CHANGED );
        }

        public void startAt( int pos ) throws RemoteException
        {
            if( mDb.getPlaylistSize() < pos )
                throw new RemoteException();
            playTrack( mDb.getTrackPlaylist( pos ), pos, mMp );
            notifyChange( META_CHANGED );
            
        }

        public void stop() throws RemoteException
        {
            Mp3tunesService.this.stop();
        }

        public void setAlbumArt( Bitmap art ) throws RemoteException
        {
            mAlbumArt = art;
        }

        public Bitmap getAlbumArt() throws RemoteException
        {
            return mAlbumArt;
        }

        /* Returns the meta data of the current track
        0: track name
        1: track id
        2: artist name
        3: artist id
        4: album name
        5: album id
       */
        public String[] getMetadata() throws RemoteException
        {
            String[] data;
            if( mCurrentTrack == null )
                data = new String[] { UNKNOWN, UNKNOWN, UNKNOWN , UNKNOWN , UNKNOWN , UNKNOWN  };
            else
                data = new String[] { mCurrentTrack.getTitle(), Integer.toString( mCurrentTrack.getId() ), mCurrentTrack.getArtistName(), 
                    Integer.toString( mCurrentTrack.getArtistId() ), mCurrentTrack.getAlbumTitle(), Integer.toString(mCurrentTrack.getAlbumId() ) };
            
            return data;
        }

        public String getArtUrl() throws RemoteException
        {
            if ( mServiceState != STATE.PLAYING && mServiceState != STATE.PAUSED )
                return null;
            return mCurrentTrack.getAlbumArt(); 
        }

        public boolean isPaused() throws RemoteException
        {
            return mServiceState == STATE.PAUSED;   
        }

    };
    
    private class NextTrackTask extends UserTask<Void, Void, Boolean> {

        public Boolean doInBackground(Void... input) {
            boolean success = false;
            try
            {
                nextTrack();
                success = true;
            }
            catch ( Exception e )
            {
                success = false;
            }
            return success;
        }

        @Override
        public void onPostExecute(Boolean result) {
            if(!result) {
                notifyChange( PLAYBACK_ERROR );
            }
        }
    }

    /**
     * Class responsible for fading in/out volume, for instance when a phone
     * call arrives
     * 
     * @author Lukasz Wisniewski
     * 
     *         TODO if volume is not at 1.0 or 0.0 when this starts (eg. old
     *         fade task didn't finish) then this sounds broken. Hard to fix
     *         though as you have to recalculate the fade duration etc.
     * 
     *         TODO setVolume is not logarithmic, and the ear is. We need a
     *         natural log scale see:
     *         http://stackoverflow.com/questions/207016/how
     *         -to-fade-out-volume-naturally see:
     *         http://code.google.com/android/
     *         reference/android/media/MediaPlayer
     *         .html#setVolume(float,%20float)
     */
    private abstract class FadeVolumeTask extends TimerTask
    {

        public static final int FADE_IN = 0;
        public static final int FADE_OUT = 1;

        private int mCurrentStep = 0;
        private int mSteps;
        private int mMode;

        /**
         * Constructor, launches timer immediately
         * 
         * @param mode
         *            Volume fade mode <code>FADE_IN</code> or
         *            <code>FADE_OUT</code>
         * @param millis
         *            Time the fade process should take
         * @param steps
         *            Number of volume gradations within given fade time
         */
        public FadeVolumeTask( int mode, int millis )
        {
            this.mMode = mode;
            this.mSteps = millis / 20; // 20 times per second
            this.onPreExecute();
            new Timer().scheduleAtFixedRate( this, 0, millis / mSteps );
        }

        @Override
        public void run()
        {
            float volumeValue = 1.0f;

            if ( mMode == FADE_OUT )
            {
                volumeValue *= ( float ) ( mSteps - mCurrentStep ) / ( float ) mSteps;
            }
            else
            {
                volumeValue *= ( float ) ( mCurrentStep ) / ( float ) mSteps;
            }

            mMp.setVolume( volumeValue, volumeValue );

            if ( mCurrentStep >= mSteps )
            {
                this.onPostExecute();
                this.cancel();
            }

            mCurrentStep++;
        }

        /**
         * Task executed before launching timer
         */
        public abstract void onPreExecute();

        /**
         * Task executer after timer finished working
         */
        public abstract void onPostExecute();
    }
    
    private class RefreshSessionTask extends UserTask<Void, Void, Boolean> {

        public void onPreExecute()
        {}
        public Boolean doInBackground(Void... input) {
            boolean success = false;
            try
            {
                SharedPreferences settings = getSharedPreferences( Login.PREFS, 0 );
                String user = settings.getString( "mp3tunes_user", "" );
                String pass = settings.getString( "mp3tunes_pass", "" );
                if ( !user.equals( "" ) && !pass.equals( "" ) )
                {
                    mLocker.refreshSession( user, pass );
                    success = true;
                }
            }
            catch ( Exception e )
            {
                success = false;
            }
            return success;
        }

        @Override
        public void onPostExecute(Boolean result) {
            if(!result) {
                notifyChange( PLAYBACK_ERROR );
                mAutoSkipCount++;
            }
            if ( mAutoSkipCount++ > 4 )
            {
                // If we weren't able to start playing after 3 attempts, bail
                // out and notify
                // the user. This will bring us into a stopped state.
                mServiceState = STATE.STOPPED;
                notifyChange(PLAYBACK_ERROR);
                mNm.cancel( NOTIFY_ID );
                 if( mWakeLock.isHeld())
                     mWakeLock.release();

                if ( mWifiLock.isHeld() )
                    mWifiLock.release();
                mDeferredStopHandler.deferredStopSelf();
            }
            else
            {

                // restart the current track
                mServiceState = STATE.STOPPED;
                mMp.stop();
                playTrack( mCurrentTrack, mCurrentPosition, mMp );
                // Enter a state that will allow nextSong to do its thang
//                new NextTrackTask().execute((Void)null);
            }
        }
    }

}
