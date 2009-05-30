/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/ramblurr/src/workspace/Mp3tunes/src/com/mp3tunes/android/service/ITunesService.aidl
 */
package com.mp3tunes.android.service;
import java.lang.String;
import android.os.RemoteException;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Binder;
import android.os.Parcel;
import android.graphics.Bitmap;
public interface ITunesService extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.mp3tunes.android.service.ITunesService
{
private static final java.lang.String DESCRIPTOR = "com.mp3tunes.android.service.ITunesService";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an ITunesService interface,
 * generating a proxy if needed.
 */
public static com.mp3tunes.android.service.ITunesService asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
com.mp3tunes.android.service.ITunesService in = (com.mp3tunes.android.service.ITunesService)obj.queryLocalInterface(DESCRIPTOR);
if ((in!=null)) {
return in;
}
return new com.mp3tunes.android.service.ITunesService.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
{
return this;
}
public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_pause:
{
data.enforceInterface(DESCRIPTOR);
this.pause();
reply.writeNoException();
return true;
}
case TRANSACTION_stop:
{
data.enforceInterface(DESCRIPTOR);
this.stop();
reply.writeNoException();
return true;
}
case TRANSACTION_prev:
{
data.enforceInterface(DESCRIPTOR);
this.prev();
reply.writeNoException();
return true;
}
case TRANSACTION_next:
{
data.enforceInterface(DESCRIPTOR);
this.next();
reply.writeNoException();
return true;
}
case TRANSACTION_start:
{
data.enforceInterface(DESCRIPTOR);
this.start();
reply.writeNoException();
return true;
}
case TRANSACTION_startAt:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.startAt(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_setShuffleMode:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.setShuffleMode(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getShuffleMode:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getShuffleMode();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_setRepeatMode:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
this.setRepeatMode(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getRepeatMode:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getRepeatMode();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_getMetadata:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String[] _result = this.getMetadata();
reply.writeNoException();
reply.writeStringArray(_result);
return true;
}
case TRANSACTION_getArtUrl:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getArtUrl();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getAlbumArt:
{
data.enforceInterface(DESCRIPTOR);
android.graphics.Bitmap _result = this.getAlbumArt();
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_setAlbumArt:
{
data.enforceInterface(DESCRIPTOR);
android.graphics.Bitmap _arg0;
if ((0!=data.readInt())) {
_arg0 = android.graphics.Bitmap.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.setAlbumArt(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getDuration:
{
data.enforceInterface(DESCRIPTOR);
long _result = this.getDuration();
reply.writeNoException();
reply.writeLong(_result);
return true;
}
case TRANSACTION_getPosition:
{
data.enforceInterface(DESCRIPTOR);
long _result = this.getPosition();
reply.writeNoException();
reply.writeLong(_result);
return true;
}
case TRANSACTION_setPosition:
{
data.enforceInterface(DESCRIPTOR);
int _arg0;
_arg0 = data.readInt();
boolean _result = this.setPosition(_arg0);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_getBufferPercent:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getBufferPercent();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_isPlaying:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isPlaying();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements com.mp3tunes.android.service.ITunesService
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
/* Pause playback */
public void pause() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_pause, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/* Stop playback */
public void stop() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_stop, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/* Play the prev song in the playlist */
public void prev() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_prev, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/* Play the next song in the playlist */
public void next() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_next, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/* Play the current selected item in the playlist */
public void start() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_start, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/* Play the track at a particular position in the playlist */
public void startAt(int pos) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(pos);
mRemote.transact(Stub.TRANSACTION_startAt, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/* SHUFFLE or NORMAL */
public void setShuffleMode(int mode) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(mode);
mRemote.transact(Stub.TRANSACTION_setShuffleMode, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/* Get the shuffle state */
public int getShuffleMode() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getShuffleMode, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/* NONE, SONG, or PLAYLIST */
public void setRepeatMode(int mode) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(mode);
mRemote.transact(Stub.TRANSACTION_setRepeatMode, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/* Get the repeat state */
public int getRepeatMode() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getRepeatMode, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/* Returns the meta data of the current track
	 0: track name
	 1: track id
	 2: artist name
	 3: artist id
	 4: album name
	 5: album id
	*/
public java.lang.String[] getMetadata() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String[] _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getMetadata, _data, _reply, 0);
_reply.readException();
_result = _reply.createStringArray();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.lang.String getArtUrl() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getArtUrl, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public android.graphics.Bitmap getAlbumArt() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
android.graphics.Bitmap _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getAlbumArt, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = android.graphics.Bitmap.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public void setAlbumArt(android.graphics.Bitmap art) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((art!=null)) {
_data.writeInt(1);
art.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_setAlbumArt, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
/* Returns the duration of the current track */
public long getDuration() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
long _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getDuration, _data, _reply, 0);
_reply.readException();
_result = _reply.readLong();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/* Returns the position of the current track */
public long getPosition() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
long _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getPosition, _data, _reply, 0);
_reply.readException();
_result = _reply.readLong();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/* Set the position of the currently played track. Returns true 
	   if the operation was successful. */
public boolean setPosition(int msec) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(msec);
mRemote.transact(Stub.TRANSACTION_setPosition, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/* Returns the percentage the track has buffered */
public int getBufferPercent() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getBufferPercent, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/* Returns true if a track is currently playing */
public boolean isPlaying() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isPlaying, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
}
static final int TRANSACTION_pause = (IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_stop = (IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_prev = (IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_next = (IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_start = (IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_startAt = (IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_setShuffleMode = (IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_getShuffleMode = (IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_setRepeatMode = (IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_getRepeatMode = (IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_getMetadata = (IBinder.FIRST_CALL_TRANSACTION + 10);
static final int TRANSACTION_getArtUrl = (IBinder.FIRST_CALL_TRANSACTION + 11);
static final int TRANSACTION_getAlbumArt = (IBinder.FIRST_CALL_TRANSACTION + 12);
static final int TRANSACTION_setAlbumArt = (IBinder.FIRST_CALL_TRANSACTION + 13);
static final int TRANSACTION_getDuration = (IBinder.FIRST_CALL_TRANSACTION + 14);
static final int TRANSACTION_getPosition = (IBinder.FIRST_CALL_TRANSACTION + 15);
static final int TRANSACTION_setPosition = (IBinder.FIRST_CALL_TRANSACTION + 16);
static final int TRANSACTION_getBufferPercent = (IBinder.FIRST_CALL_TRANSACTION + 17);
static final int TRANSACTION_isPlaying = (IBinder.FIRST_CALL_TRANSACTION + 18);
}
/* Pause playback */
public void pause() throws android.os.RemoteException;
/* Stop playback */
public void stop() throws android.os.RemoteException;
/* Play the prev song in the playlist */
public void prev() throws android.os.RemoteException;
/* Play the next song in the playlist */
public void next() throws android.os.RemoteException;
/* Play the current selected item in the playlist */
public void start() throws android.os.RemoteException;
/* Play the track at a particular position in the playlist */
public void startAt(int pos) throws android.os.RemoteException;
/* SHUFFLE or NORMAL */
public void setShuffleMode(int mode) throws android.os.RemoteException;
/* Get the shuffle state */
public int getShuffleMode() throws android.os.RemoteException;
/* NONE, SONG, or PLAYLIST */
public void setRepeatMode(int mode) throws android.os.RemoteException;
/* Get the repeat state */
public int getRepeatMode() throws android.os.RemoteException;
/* Returns the meta data of the current track
	 0: track name
	 1: track id
	 2: artist name
	 3: artist id
	 4: album name
	 5: album id
	*/
public java.lang.String[] getMetadata() throws android.os.RemoteException;
public java.lang.String getArtUrl() throws android.os.RemoteException;
public android.graphics.Bitmap getAlbumArt() throws android.os.RemoteException;
public void setAlbumArt(android.graphics.Bitmap art) throws android.os.RemoteException;
/* Returns the duration of the current track */
public long getDuration() throws android.os.RemoteException;
/* Returns the position of the current track */
public long getPosition() throws android.os.RemoteException;
/* Set the position of the currently played track. Returns true 
	   if the operation was successful. */
public boolean setPosition(int msec) throws android.os.RemoteException;
/* Returns the percentage the track has buffered */
public int getBufferPercent() throws android.os.RemoteException;
/* Returns true if a track is currently playing */
public boolean isPlaying() throws android.os.RemoteException;
}
