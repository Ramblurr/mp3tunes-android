/***************************************************************************
 *   Copyright (C) 2009  Casey Link <unnamedrambler@gmail.com>             *
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
package com.mp3tunes.android.util;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.util.Log;

import com.mp3tunes.android.R;
import com.mp3tunes.android.activity.UpdateActivity;
import com.tomgibara.android.veecheck.VeecheckNotifier;
import com.tomgibara.android.veecheck.VeecheckService;
import com.tomgibara.android.veecheck.VeecheckState;
import com.tomgibara.android.veecheck.util.DefaultNotifier;
import com.tomgibara.android.veecheck.util.PrefState;


public class UpdateService extends VeecheckService
{
    public static final int NOTIFICATION_ID = 1;

    @Override
    protected VeecheckNotifier createNotifier() {
        //it's good practice to set up filters to help guard against malicious intents 
        IntentFilter[] filters = new IntentFilter[1];
        try {
            IntentFilter filter = new IntentFilter(Intent.ACTION_VIEW);
            filter.addDataType("text/html");
            filter.addDataScheme("http");
            filters[0] = filter;
        } catch (MalformedMimeTypeException e) {
            Log.e("veecheck", "Invalid data type for filter.", e);
        }
        
        //return a default notifier implementation
        return new DefaultNotifier(this, NOTIFICATION_ID, filters,
                new Intent(this, UpdateActivity.class),
                R.drawable.icon,
                R.string.update_ticker,
                R.string.update_title,
                R.string.update_message);
    }
    
    @Override
    protected VeecheckState createState() {
        return new PrefState(this);
    }
}
