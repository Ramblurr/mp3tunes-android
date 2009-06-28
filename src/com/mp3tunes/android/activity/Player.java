package com.mp3tunes.android.activity;

import java.util.Formatter;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.binaryelysium.mp3tunes.api.Locker;
import com.binaryelysium.mp3tunes.api.Track;
import com.mp3tunes.android.MP3tunesApplication;
import com.mp3tunes.android.Music;
import com.mp3tunes.android.R;
import com.mp3tunes.android.RemoteImageHandler;
import com.mp3tunes.android.RemoteImageView;
import com.mp3tunes.android.service.Mp3tunesService;
import com.mp3tunes.android.util.UserTask;
import com.mp3tunes.android.util.Worker;


public class Player extends Activity
{
    protected static final int REFRESH = 0;

    private ImageButton mPrevButton;
    private ImageButton mPlayButton;
    private ImageButton mStopButton;
    private ImageButton mNextButton;
    private ImageButton mQueueButton;
    private RemoteImageView mAlbum;
    private TextView mCurrentTime;
    private TextView mTotalTime;
    private TextView mArtistName;
    private TextView mTrackName;
    private ProgressBar mProgress;
    private ProgressDialog mProgressDialog;
    
    private long mDuration;
    private boolean paused;
    
    private Locker mLocker;
    private Worker mAlbumArtWorker;
    private RemoteImageHandler mAlbumArtHandler;
    private IntentFilter mIntentFilter;
    
    @Override
    public void onCreate( Bundle icicle )
    {
        super.onCreate(icicle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.audio_player);
        
        mLocker = ( Locker ) MP3tunesApplication.getInstance().map.get( "mp3tunes_locker" );
//        if ( mLocker == null )
//            logout();
        
        mCurrentTime = (TextView) findViewById(R.id.currenttime);
        mTotalTime = (TextView) findViewById(R.id.totaltime);
        mProgress = (ProgressBar) findViewById(android.R.id.progress);
        mProgress.setMax(1000);
        mAlbum = (RemoteImageView) findViewById(R.id.album);
        mArtistName = (TextView) findViewById(R.id.track_artist);
        mTrackName = (TextView) findViewById(R.id.track_title);
        
        mPrevButton = ( ImageButton ) findViewById( R.id.rew );
        mPrevButton.setOnClickListener(mPrevListener);
        mPlayButton = ( ImageButton ) findViewById( R.id.play );
        mPlayButton.setOnClickListener(mPlayListener);
        mPlayButton.requestFocus();
//        mStopButton = (ImageButton) findViewById(R.id.stop);
//        mStopButton.requestFocus();
//        mStopButton.setOnClickListener(mStopListener);
        mNextButton = ( ImageButton ) findViewById( R.id.fwd );
        mNextButton.setOnClickListener(mNextListener);
        
        mQueueButton = ( ImageButton ) findViewById( R.id.playlist_button );
        mQueueButton.setOnClickListener(mQueueListener);
        
        mAlbumArtWorker = new Worker("album art worker");
        mAlbumArtHandler = new RemoteImageHandler(mAlbumArtWorker.getLooper(),
                mHandler);
        Music.bindToService(this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(Mp3tunesService.META_CHANGED);
        mIntentFilter.addAction(Mp3tunesService.PLAYBACK_FINISHED);
        mIntentFilter.addAction(Mp3tunesService.PLAYBACK_STATE_CHANGED);
        mIntentFilter.addAction(Mp3tunesService.PLAYBACK_ERROR);
        mIntentFilter.addAction(Mp3tunesService.DATABASE_ERROR);
        updateTrackInfo();
    }
    
    @Override
    public void onStart() {
        super.onStart();
        paused = false;
        long next = refreshNow();
        queueNextRefresh(next);
    }
    
    @Override
    public void onStop() {

        paused = true;
        mHandler.removeMessages(REFRESH);

        super.onStop();
    }
    
    @Override
    public void onSaveInstanceState(Bundle outState) {

        outState.putBoolean("configchange", getChangingConfigurations() != 0);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onPause() {
//        Music.unbindFromService( this );
        unregisterReceiver(mStatusListener);
        super.onPause();
    }
    
    @Override
    public void onResume() {
        registerReceiver(mStatusListener, mIntentFilter);
//        Music.bindToService(this);
        updateTrackInfo();
        setPauseButtonImage();

        super.onResume();
    }

    @Override
    public void onDestroy() {
        Music.unbindFromService( this );
        mAlbumArtWorker.quit();
        super.onDestroy();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.player, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.menu_opt_main:
                intent = new Intent();
                intent.setClass( this, LockerList.class );
                intent.setFlags( Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP );
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    private View.OnClickListener mPrevListener = new View.OnClickListener() {

        public void onClick(View v) {

            if (Music.sService == null)
                return;
            // TODO Prev
        }
    };
    
    private View.OnClickListener mPlayListener = new View.OnClickListener() {

        public void onClick(View v) {

            if (Music.sService== null)
                return;
            try
            {
                Music.sService.pause();
            }
            catch ( RemoteException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };
    
    private View.OnClickListener mQueueListener = new View.OnClickListener() {

        public void onClick(View v) 
        {
            startActivity(
                    new Intent(Intent.ACTION_EDIT)
                    .setDataAndType(Uri.EMPTY, "vnd.mp3tunes.android.dir/track")
                    .putExtra("playlist", "nowplaying")
            );
        }
    };
    
    private void setPauseButtonImage()
    {
         try
        {
            if ( Music.sService!= null && Music.sService.isPaused() )
            {
                mPlayButton.setImageResource( R.drawable.play_button );
            }
            else
            {
                mPlayButton.setImageResource( R.drawable.pause_button );
            }
        }
        catch ( RemoteException ex )
        {
        }
    }

    
//    private View.OnClickListener mStopListener = new View.OnClickListener() {
//
//        public void onClick(View v) {
//
//            if (Music.sService== null)
//                return;
//            try
//            {
//                Music.sService.stop();
//            }
//            catch ( RemoteException e )
//            {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//    };
    
    private View.OnClickListener mNextListener = new View.OnClickListener() {

        public void onClick(View v) {

            if (Music.sService== null)
                return;
            try
            {
                Music.sService.next();
            }
            catch ( RemoteException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };
    
    private BroadcastReceiver mStatusListener = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {

            String action = intent.getAction();
            if (action.equals(Mp3tunesService.META_CHANGED)) {
                // redraw the artist/title info and
                // set new max for progress bar
                updateTrackInfo();
            } else if (action.equals(Mp3tunesService.PLAYBACK_FINISHED)) {
                finish();
            } else if (action.equals(Mp3tunesService.PLAYBACK_ERROR)) {
                MP3tunesApplication.getInstance().presentError(
                        context,
                        getResources().getString(
                                R.string.ERROR_GENERIC_TITLE),
                        getResources().getString(
                                R.string.ERROR_GENERIC));
                finish();
            } else if (action.equals(Mp3tunesService.PLAYBACK_ERROR)) {
                // TODO add a skip counter and try to skip 3 times before
                // display an error message
                if (Music.sService== null)
                    return;
                String error = null; // TODO pass some error messages?
                if (error != null) {
                    MP3tunesApplication.getInstance().presentError(context,
                            error, error);
                } else {
                    MP3tunesApplication.getInstance().presentError(
                            context,
                            getResources().getString(
                                    R.string.ERROR_PLAYBACK_FAILED_TITLE),
                            getResources().getString(
                                    R.string.ERROR_PLAYBACK_FAILED));
                }
            } else if(action.equals( Mp3tunesService.PLAYBACK_STATE_CHANGED )) {
                setPauseButtonImage();
            }
        }
    };
    
    private void updateTrackInfo() 
    {
        try {
            if (Music.sService== null)
                return;
            
            String[] metadata = Music.sService.getMetadata();
            String artistName = metadata[2];
            String trackName = metadata[0];
            int album_id = Integer.valueOf( metadata[5] );
            
            if(artistName.equals(Mp3tunesService.UNKNOWN)) {
                mArtistName.setText("");
            } else {
                mArtistName.setText(artistName);
            }
            if(trackName.equals(Mp3tunesService.UNKNOWN)) {
                mTrackName.setText("");
            } else {
                mTrackName.setText(trackName);
            }

            Bitmap bit = Music.getArtworkQuick( Player.this, album_id, mAlbum.getWidth(), mAlbum.getHeight() );
            mAlbum.setArtwork( bit );
            mAlbum.invalidate();
            if (bit == null)
                 new LoadAlbumArtTask().execute((Void) null);
            setPauseButtonImage();
        } catch (java.util.concurrent.RejectedExecutionException e) {
            e.printStackTrace();
        } catch (RemoteException ex) {
            // FIXME why do we finish() ?????
            finish();
        }
    }
    
    private void queueNextRefresh(long delay) {

        if (!paused) {
            Message msg = mHandler.obtainMessage(REFRESH);
            mHandler.removeMessages(REFRESH);
            mHandler.sendMessageDelayed(msg, delay);
        }
    }

    private long refreshNow() {

        if (Music.sService== null)
            return 500;
        try {
            mDuration = Music.sService.getDuration();
            long pos = Music.sService.getPosition();
            int buffpercent = Music.sService.getBufferPercent();
            long remaining = 1000 - (pos % 1000);
            if ((pos >= 0) && (mDuration > 0) && (pos <= mDuration)) {
                mCurrentTime.setText(Music.makeTimeString(this, pos / 1000));
                mTotalTime.setText(Music.makeTimeString(this, mDuration / 1000));
                mProgress.setProgress( (int) ( 1000 * pos / mDuration ) );
                mProgress.setSecondaryProgress( buffpercent * 10 );
                if (mProgressDialog != null) {
                    mProgressDialog.dismiss();
                    mProgressDialog = null;
                }
            } else {
                mCurrentTime.setText("--:--");
                mTotalTime.setText("--:--");
                mProgress.setProgress(0);
                if (mProgressDialog == null
                        && Music.sService.isPlaying()) {
                    mProgressDialog = ProgressDialog.show(this, "",
                            "Buffering", true, false);
                    mProgressDialog
                            .setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
                    mProgressDialog.setCancelable(true);
                }
            }
            // return the number of milliseconds until the next full second, so
            // the counter can be updated at just the right time
            return remaining;
        } catch (RemoteException ex) {
        }
        return 500;
    }

    private final Handler mHandler = new Handler() {

        public void handleMessage(Message msg) {

            switch (msg.what) 
            {
                case REFRESH:
                    long next = refreshNow();
                    queueNextRefresh(next);
                    break;
                case RemoteImageHandler.REMOTE_IMAGE_DECODED:
                    mAlbum.setArtwork((Bitmap) msg.obj);
                    mAlbum.invalidate();
                    try {
                        if (Music.sService!= null)
                            Music.sService
                                    .setAlbumArt((Bitmap) msg.obj);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    break;
    
                default:
                    break;
            }
        }
    };
    
    private class LoadAlbumArtTask extends UserTask<Void, Void, Boolean> 
    {
        String artUrl;

        @Override
        public void onPreExecute() {
        }

        @Override
        public Boolean doInBackground(Void... params) 
        {
            try {
                if (Music.sService!= null) {
                    artUrl = Music.sService.getArtUrl();
                    String[] metadata = Music.sService.getMetadata();
                    int track_id = Integer.parseInt( metadata[1] );
                    Track[] tracks = mLocker.getTracksForAlbum( Integer.parseInt( metadata[5] ) ).getData();
                    // TODO maybe a better way to do this. probably cache it in the database
                    for( Track t : tracks)
                    {
                        if( t.getId() == track_id ) {
                            artUrl = t.getAlbumArt();
                            return true;
                        }
                    }
                    artUrl = Mp3tunesService.UNKNOWN;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return false;
        }
        

        @Override
        public void onPostExecute(Boolean result) {
            System.out.println("Art url: " + artUrl);
            if (artUrl != Mp3tunesService.UNKNOWN) {
                mAlbumArtHandler
                        .removeMessages(RemoteImageHandler.GET_REMOTE_IMAGE);
                mAlbumArtHandler.obtainMessage(
                        RemoteImageHandler.GET_REMOTE_IMAGE, artUrl)
                        .sendToTarget();
            }
        }
    }

}
