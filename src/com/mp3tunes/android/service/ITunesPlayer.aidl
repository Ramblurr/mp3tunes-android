package com.mp3tunes.android.service;
interface ITunesPlayer {  

	void pause(); /* Pause playback */
	void stop(); /* Stop playback */
	void prev(); /* Play the prev song in the playlist */
	void next(); /* Play the next song in the playlist */
	void startPlaying();
	
	
	/* SHUFFLE or NORMAL */
	void setShuffleMode(int mode);
	
	/* Get the shuffle state */
	int getShuffleMode();
	
	/* NONE, SONG, or PLAYLIST */
	void setRepeatMode(int mode);
	
	/* Get the repear state */
	int getRepeatMode();
	
	/* Returns the current track's artist name*/
	String getArtistName();
	
	/* Returns the current track's album name*/
	String getAlbumName();
	
	/* Returns the current track name */
	String getTrackName();
	String getArtUrl();
	
	/* Returns the duration of the current track */
	long   getDuration();
	
	/* Returns the position of the current track */
	long   getPosition(); 
	
	/* Set the position of the currently played track. Returns true 
	   if the operation was successful. */
	boolean setPosition(in long msec);
	
	/* Returns the percentage the track has buffered */
	int	   getBufferPercent();
	
	boolean isPlaying();
	
} 