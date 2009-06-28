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
package com.mp3tunes.android.activity;

import java.util.concurrent.ExecutionException;

import com.binaryelysium.mp3tunes.api.LockerException;
import com.mp3tunes.android.MP3tunesApplication;
import com.mp3tunes.android.PrivateAPIKey;
import com.mp3tunes.android.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * This activity handles authentication.
 */
public class Login extends Activity
{

    public static final String PREFS = "LoginPrefs";
    private boolean mLoginShown;
    private EditText mPassField;
    private EditText mUserField;
    private Button mLoginButton;
    private AlertDialog mProgDialog;
    
    private static final int PROGRESS = 0;

    String authInfo;

    /** Called when the activity is first created. */
    @Override
    public void onCreate( Bundle icicle )
    {
        super.onCreate( icicle );
        requestWindowFeature( Window.FEATURE_NO_TITLE );
        SharedPreferences settings = getSharedPreferences( PREFS, 0 );
        String user = settings.getString( "mp3tunes_user", "" );
        String pass = settings.getString( "mp3tunes_pass", "" );
        System.out.println("user: " + user + " pass: " + pass);
        if ( !user.equals( "" ) && !pass.equals( "" ) )
        {
            AsyncTask task = new LoginTask().execute( user, pass );
            try
            {
                task.get();
            }
            catch ( InterruptedException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            catch ( ExecutionException e )
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        setContentView( R.layout.login );

        mPassField = ( EditText ) findViewById( R.id.password );
        mUserField = ( EditText ) findViewById( R.id.username );
        mLoginButton = ( Button ) findViewById( R.id.sign_in_button );
        mPassField.setOnKeyListener( mKeyListener );
        
        AlertDialog.Builder builder;
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.progress_dialog,
                                       (ViewGroup) findViewById(R.id.layout_root));

        TextView text = (TextView) layout.findViewById(R.id.progress_text);
        text.setText(R.string.loading_authentication);

        builder = new AlertDialog.Builder(this);
        builder.setView(layout);
        mProgDialog = builder.create();

        // restore text fields
        if ( icicle != null )
        {
            user = icicle.getString( "username" );
            pass = icicle.getString( "pass" );
            if ( user != null )
                mUserField.setText( user );

            if ( pass != null )
                mPassField.setText( pass );
        }
        mLoginButton.setOnClickListener( mClickListener );
    }
    
    @Override
    protected Dialog onCreateDialog( int id )
    {
        switch ( id )
        {
        case PROGRESS:
            return mProgDialog;
        default:
            return null;
        }
    }

    OnClickListener mClickListener = new View.OnClickListener()
    {

        public void onClick( View v )
        {
            String user = mUserField.getText().toString();
            String password = mPassField.getText().toString();

            if ( user.length() == 0 || password.length() == 0 )
            {
                MP3tunesApplication.getInstance().presentError( v.getContext(),
                        getResources().getString( R.string.ERROR_MISSINGINFO_TITLE ),
                        getResources().getString( R.string.ERROR_MISSINGINFO ) );
                return;
            }
            showDialog( PROGRESS );
            new LoginTask().execute( user, password );
        }
    };

    OnKeyListener mKeyListener = new OnKeyListener()
    {

        public boolean onKey( View v, int keyCode, KeyEvent event )
        {
            switch ( event.getKeyCode() )
            {
            case KeyEvent.KEYCODE_ENTER:
                mLoginButton.setPressed( true );
                mLoginButton.performClick();
                return true;
            }
            return false;
        }
    };
    
    private class LoginTask extends AsyncTask<String, Void, String>
    {
        com.binaryelysium.mp3tunes.api.Locker locker;
        String user;
        String pass;
        @Override
        protected String doInBackground( String... params )
        {
            user = params[0];
            pass = params[1];
            try 
            {
                locker = new com.binaryelysium.mp3tunes.api.Locker( PrivateAPIKey.KEY, user, pass );
                return "";
            } catch (LockerException e) {
                return e.getMessage();
            }
        }
        
        @Override
        protected void onPostExecute( String result )
        {
            try
            {
                dismissDialog( PROGRESS );    
            } catch (IllegalArgumentException e) 
            {
                // do nothing
            }
            
            if( result.equals( "" )  )
            {
                MP3tunesApplication.getInstance().map.put( "mp3tunes_locker", locker );
                SharedPreferences settings = getSharedPreferences( PREFS, 0 );
                SharedPreferences.Editor editor = settings.edit();
                editor.putString( "mp3tunes_user", user );
                editor.putString( "mp3tunes_pass", pass );
                editor.commit();
                Intent intent = new Intent( Login.this, LockerList.class );
                startActivity( intent );
                finish();
            }
            else if ( result.contains( "auth failure" ) )
            {
                MP3tunesApplication.getInstance().presentError( Login.this,
                        getResources().getString( R.string.ERROR_AUTH_TITLE ),
                        getResources().getString( R.string.ERROR_AUTH ) );
                ( ( EditText ) findViewById( R.id.password ) ).setText( "" );
            }
            else if ( result.contains( "connection issue" ) )
            {
                MP3tunesApplication.getInstance().presentError( Login.this,
                        getResources().getString( R.string.ERROR_SERVER_UNAVAILABLE_TITLE ),
                        getResources().getString( R.string.ERROR_SERVER_UNAVAILABLE ) );
            }
        }
        
    }

}
