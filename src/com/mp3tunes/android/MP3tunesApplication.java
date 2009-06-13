/***************************************************************************
 *   Copyright 2008 Casey Link <unnamedrambler@gmail.com>                  *
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
package com.mp3tunes.android;

import java.util.WeakHashMap;

import android.app.AlertDialog;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.IBinder;


public class MP3tunesApplication extends Application
{

    public WeakHashMap map; // used to store global instance specific data
    public static final String LAST_UPDATE = "LastUpdate"; // for SharedPreferences
//    public com.mp3tunes.android.service.ITunesService player = null;

    private static MP3tunesApplication instance;
    private Context mCtx;

    public static MP3tunesApplication getInstance()
    {

        return instance;
    }

    public void onCreate()
    {

        super.onCreate();
        instance = this;

        this.map = new WeakHashMap();
    }    


    
    public void onTerminate()
    {
        // clean up application global
        this.map.clear();
        this.map = null;

        instance = null;
        super.onTerminate();
    }
    
    
    /**
     * Shows an error dialog to the user.
     * @param ctx
     * @param title
     * @param description
     */
    public void presentError(Context ctx, String title, String description) {
        AlertDialog.Builder d = new AlertDialog.Builder(ctx);
        d.setTitle(title);
        d.setMessage(description);
        d.setIcon(android.R.drawable.ic_dialog_alert);
        d.setNeutralButton("OK",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton)
                    {
                    }
                });
        d.show();
    }
    
    /**
     * returns the last time the cache was updated. 
     * this value is stored in Android's SharedPreferences
     * @return a timestamp for the last time the cache was updated in 
     *         or -1 if never.
     */
    public long getLastUpdate()
    {
        SharedPreferences prefs = getSharedPreferences( LAST_UPDATE, 0 );
        return prefs.getLong( "last_update", -1 );
    }
    
    /**
     * sets the last time the cache was updated. 
     * this value is stored in Android's SharedPreferences
     */
    public void setLastUpdate(long timestamp) 
    {
        SharedPreferences prefs = getSharedPreferences( LAST_UPDATE, 0 );
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong( "last_update", timestamp );
        editor.commit();
    }
    
    /**
     * clears the lastupdate timestamp
     */
    public void clearUpdate()
    {
        SharedPreferences prefs = getSharedPreferences( LAST_UPDATE, 0 );
        SharedPreferences.Editor editor = prefs.edit();
        editor.putLong( "last_update", -1 );
        editor.commit();
    }
    
    

}
