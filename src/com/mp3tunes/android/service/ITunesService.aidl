package com.mp3tunes.android.service;
interface ITunesService {  

	/* Pause playback */
	void pause(); 
	
	/* Stop playback */
	void stop();
	
	/* Play the prev song in the playlist */
	void prev(); 
	
	/* Play the next song in the playlist */
	void next(); 
	
	/* Play the current selected item in the playlist */
	void start();

	/* Play the track at a particular position in the playlist */
	void startAt(int pos);
	
	
	/* SHUFFLE or NORMAL */
	void setShuffleMode(int mode);
	
	/* Get the shuffle state */
	int getShuffleMode();
	
	/* NONE, SONG, or PLAYLIST */
	void setRepeatMode(int mode);
	
	/* Get the repeat state */
	int getRepeatMode();
	
	/* Returns the current track's artist name*/
	String getArtistName();
	
	/* Returns the current track's album name*/
	String getAlbumName();
	
	/* Returns the current track name */
	String getTrackName();
	
	String getArtUrl();
	Bitmap getAlbumArt();
	void setAlbumArt(in Bitmap art);
	
	/* Returns the duration of the current track */
	long   getDuration();
	
	/* Returns the position of the current track */
	long   getPosition(); 
	
	/* Set the position of the currently played track. Returns true 
	   if the operation was successful. */
	boolean setPosition(in int msec);
	
	/* Returns the percentage the track has buffered */
	int	   getBufferPercent();
	
	/* Returns true if a track is currently playing */
	boolean isPlaying();
	
} 