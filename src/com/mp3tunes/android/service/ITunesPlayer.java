/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/ramblurr/src/workspace/Mp3tunes/src/com/mp3tunes/android/service/ITunesPlayer.aidl
 */
package com.mp3tunes.android.service;
import java.lang.String;
import android.os.RemoteException;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Binder;
import android.os.Parcel;
public interface ITunesPlayer extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements com.mp3tunes.android.service.ITunesPlayer
{
private static final java.lang.String DESCRIPTOR = "com.mp3tunes.android.service.ITunesPlayer";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an ITunesPlayer interface,
 * generating a proxy if needed.
 */
public static com.mp3tunes.android.service.ITunesPlayer asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
com.mp3tunes.android.service.ITunesPlayer in = (com.mp3tunes.android.service.ITunesPlayer)obj.queryLocalInterface(DESCRIPTOR);
if ((in!=null)) {
return in;
}
return new com.mp3tunes.android.service.ITunesPlayer.Stub.Proxy(obj);
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
case TRANSACTION_startPlaying:
{
data.enforceInterface(DESCRIPTOR);
this.startPlaying();
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
case TRANSACTION_getArtistName:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getArtistName();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getAlbumName:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getAlbumName();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getTrackName:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getTrackName();
reply.writeNoException();
reply.writeString(_result);
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
long _arg0;
_arg0 = data.readLong();
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
private static class Proxy implements com.mp3tunes.android.service.ITunesPlayer
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
/* Pause playback */
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
/* Stop playback */
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
/* Play the prev song in the playlist */
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
/* Play the next song in the playlist */
public void startPlaying() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_startPlaying, _data, _reply, 0);
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
/* Get the repear state */
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
/* Returns the current track's artist name*/
public java.lang.String getArtistName() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getArtistName, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/* Returns the current track's album name*/
public java.lang.String getAlbumName() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getAlbumName, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
/* Returns the current track name */
public java.lang.String getTrackName() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getTrackName, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
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
public boolean setPosition(long msec) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeLong(msec);
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
static final int TRANSACTION_startPlaying = (IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_setShuffleMode = (IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_getShuffleMode = (IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_setRepeatMode = (IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_getRepeatMode = (IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_getArtistName = (IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_getAlbumName = (IBinder.FIRST_CALL_TRANSACTION + 10);
static final int TRANSACTION_getTrackName = (IBinder.FIRST_CALL_TRANSACTION + 11);
static final int TRANSACTION_getArtUrl = (IBinder.FIRST_CALL_TRANSACTION + 12);
static final int TRANSACTION_getDuration = (IBinder.FIRST_CALL_TRANSACTION + 13);
static final int TRANSACTION_getPosition = (IBinder.FIRST_CALL_TRANSACTION + 14);
static final int TRANSACTION_setPosition = (IBinder.FIRST_CALL_TRANSACTION + 15);
static final int TRANSACTION_getBufferPercent = (IBinder.FIRST_CALL_TRANSACTION + 16);
static final int TRANSACTION_isPlaying = (IBinder.FIRST_CALL_TRANSACTION + 17);
}
public void pause() throws android.os.RemoteException;
/* Pause playback */
public void stop() throws android.os.RemoteException;
/* Stop playback */
public void prev() throws android.os.RemoteException;
/* Play the prev song in the playlist */
public void next() throws android.os.RemoteException;
/* Play the next song in the playlist */
public void startPlaying() throws android.os.RemoteException;
/* SHUFFLE or NORMAL */
public void setShuffleMode(int mode) throws android.os.RemoteException;
/* Get the shuffle state */
public int getShuffleMode() throws android.os.RemoteException;
/* NONE, SONG, or PLAYLIST */
public void setRepeatMode(int mode) throws android.os.RemoteException;
/* Get the repear state */
public int getRepeatMode() throws android.os.RemoteException;
/* Returns the current track's artist name*/
public java.lang.String getArtistName() throws android.os.RemoteException;
/* Returns the current track's album name*/
public java.lang.String getAlbumName() throws android.os.RemoteException;
/* Returns the current track name */
public java.lang.String getTrackName() throws android.os.RemoteException;
public java.lang.String getArtUrl() throws android.os.RemoteException;
/* Returns the duration of the current track */
public long getDuration() throws android.os.RemoteException;
/* Returns the position of the current track */
public long getPosition() throws android.os.RemoteException;
/* Set the position of the currently played track. Returns true 
	   if the operation was successful. */
public boolean setPosition(long msec) throws android.os.RemoteException;
/* Returns the percentage the track has buffered */
public int getBufferPercent() throws android.os.RemoteException;
public boolean isPlaying() throws android.os.RemoteException;
}
