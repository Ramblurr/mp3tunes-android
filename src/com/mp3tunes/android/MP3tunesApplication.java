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

import com.tomgibara.android.veecheck.Veecheck;
import com.tomgibara.android.veecheck.util.PrefSettings;

import android.app.AlertDialog;
import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
        
        SharedPreferences prefs = PrefSettings.getSharedPrefs(this);
        //assign some default settings if necessary
        if (prefs.getString(PrefSettings.KEY_CHECK_URI, null) == null) {
            Editor editor = prefs.edit();
            editor.putBoolean(PrefSettings.KEY_ENABLED, true);
            editor.putLong(PrefSettings.KEY_PERIOD, 24 * 60 * 60 * 1000);
            editor.putLong(PrefSettings.KEY_CHECK_INTERVAL, 3 * 24 * 60 * 60 * 1000L);
//            editor.putLong(PrefSettings.KEY_PERIOD, 15 * 1000L);
//            editor.putLong(PrefSettings.KEY_CHECK_INTERVAL, 30 * 1000L);
            editor.putString(PrefSettings.KEY_CHECK_URI, "http://www.binaryelysium.com/code/mp3tunes-update.xml");
            editor.commit();
        }
        System.out.println("Doing reschedule");
        Intent intent = new Intent(Veecheck.getRescheduleAction(this));
        sendBroadcast(intent);
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
    

}
