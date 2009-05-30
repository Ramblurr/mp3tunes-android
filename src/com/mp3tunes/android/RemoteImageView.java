/*
 * Originally AlbumView.java from the Android Music App
 * 
 * Copyright (C) 2008 Casey Link <unnamedrambler@gmail.com> 
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mp3tunes.android;

import com.mp3tunes.android.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Xfermode;
import android.util.AttributeSet;
import android.widget.RelativeLayout;


public class RemoteImageView extends RelativeLayout
{
    int mUnknownImage;
    
    public RemoteImageView( Context context )
    {

        super( context );
        setWillNotDraw( false );
        mUnknownImage = 0;
    }

    public RemoteImageView( Context context, AttributeSet attrs )
    {

        super( context, attrs );
        setWillNotDraw( false );
        TypedArray a = getContext().obtainStyledAttributes( attrs, R.styleable.AlbumView);
        mUnknownImage = a.getResourceId( R.styleable.AlbumView_unknownImage, 0 );
        System.out.println("unknown image id : " + mUnknownImage);
    }
    public void setDefaultImage(int resid) 
    {
        mUnknownImage = resid;
    }
    public void setArtwork( Bitmap art )
    {

        mScale = 1.00f;
        cX = 0f;
        cY = 0f;
        cR = 0f; // 24f;

        if ( art == null )
        {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inPreferredConfig = Bitmap.Config.ARGB_8888;
            art = BitmapFactory.decodeResource( getResources(),
                    mUnknownImage, opts );
        }

        if ( art != null )
        {
            mBit = art; // Bitmap.createBitmap(art, 2, 2, art.width()-4,
            // art.height()-4);
            mCoverPaint = new Paint();
            // mCoverPaint.setAntiAlias(true);
            mCoverPaint.setFilterBitmap( true );
            mCoverPaint.setDither( true );

            BitmapShader sh1 = new BitmapShader( mBit, Shader.TileMode.REPEAT,
                    Shader.TileMode.REPEAT );
            LinearGradient sh2 = new LinearGradient( 0f, mBit.getHeight(), 0f,
                    mBit.getHeight() / 3, 0x7f000000, 0x00000000,
                    Shader.TileMode.CLAMP );
            Xfermode mode = new PorterDuffXfermode( PorterDuff.Mode.DST_IN );
            mReflectionShader = new ComposeShader( sh1, sh2, mode );

            mCamera = new Camera();
            mCamera.rotateY( cR );
            mCamera.translate( cX, cY, 0 );
        }
    }

    public Bitmap getArtwork() {
    	return mBit;
    }
    
    private void drawArtwork( Canvas canvas )
    {

        if ( mBit == null )
            return;

        canvas.save();

        mCamera.applyToCanvas( canvas );

        int mywidth = getWidth();
        float artwidth = mBit.getWidth();
        float scale = ( ( float ) ( mywidth - getPaddingLeft() - getPaddingLeft() ) )
                / artwidth * mScale;
        canvas.translate( getPaddingLeft(), getPaddingTop() );

        canvas.scale( scale, scale );
        mCoverPaint.setAlpha( 255 );
        canvas.drawBitmap( mBit, 0f, 0f, mCoverPaint );

        if ( false )
        {
            // draw the reflection
            canvas.scale( 1, -1, 0, mBit.getHeight() );
            mCoverPaint.setAlpha( 64 );
            mCoverPaint.setShader( mReflectionShader );
            canvas.drawRect(
                    new Rect( 0, 0, mBit.getWidth(), mBit.getHeight() ),
                    mCoverPaint );
            mCoverPaint.setShader( null );
        }

        canvas.restore();
    }

    @Override
    protected void onDraw( Canvas canvas )
    {

        super.onDraw( canvas );

        drawArtwork( canvas );
    }

    void adjustParams( double scale, double ix, double iy, double cx,
            double cy, double cr )
    {

        mScale += scale;
        setPadding( getPaddingLeft() + ( int ) ix,
                getPaddingTop() + ( int ) iy, getPaddingRight(),
                getPaddingBottom() );
        cX += cx;
        cY += cy;
        cR += cr;

        mCamera.rotateY( ( float ) cr ); // little r
        mCamera.translate( ( float ) cx, ( float ) cy, 0 ); // little too

        System.out.println( "parameters: " + mScale + " " + getPaddingLeft()
                + " " + getPaddingTop() + " " + cX + " " + cY + " " + cR + " " );
        invalidate();
    }

    private Bitmap mBit;
    private Paint mCoverPaint;
    private Camera mCamera;
    private ComposeShader mReflectionShader;

    float mScale;
    float cX;
    float cY;
    float cR;
}
