package com.example.eventboxchallenge.app;

import android.media.AudioManager;
import android.media.MediaPlayer;

import java.io.IOException;

/**
 * Created by roinir.
 */
public class MediaPlayerWrapper extends MediaPlayer implements MediaPlayer.OnPreparedListener,
    MediaPlayer.OnCompletionListener, MediaPlayer.OnBufferingUpdateListener, MediaPlayer.OnErrorListener,
    MediaPlayer.OnInfoListener, MediaPlayer.OnSeekCompleteListener{

    public static final int STATE_ERROR = -1;
    public static final int STATE_IDLE = 0;
    public static final int STATE_INITIALIZED = 1;
    public static final int STATE_PREPARED = 2;
    public static final int STATE_PREPARING = 3;
    public static final int STATE_STOPPED = 4;

    private int state = STATE_IDLE;
    private final String dataSource;
    private int [] bufferedPercentage = new int[2];
    private int errorNumber = 0;

    public interface MediaPlayerWrapperListener{
        void onBufferingUpdate(MediaPlayerWrapper player, int percentage);
        void onCompletion(MediaPlayerWrapper player);
        boolean onError(MediaPlayerWrapper player, int what, int extra);
        boolean onInfo(MediaPlayerWrapper player, int what, int extra);
        void onPrepared(MediaPlayerWrapper mp);
        void onSeekComplete(MediaPlayerWrapper mp);
    }

    private MediaPlayerWrapperListener listener;

    public MediaPlayerWrapper(String trackUrl) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException{
        super();

        this.setAudioStreamType(AudioManager.STREAM_MUSIC);
        dataSource = trackUrl;
        this.setDataSource(trackUrl);

        super.setOnPreparedListener(this);
        super.setOnErrorListener(this);
        super.setOnCompletionListener(this);
        super.setOnBufferingUpdateListener(this);
        super.setOnInfoListener(this);
        super.setOnSeekCompleteListener(this);
    }

    public int getState() {
        return state;
    }

    public int getNumberOfErrors() {
        return errorNumber;
    }

    public String getDataSource(){
        return dataSource;
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        state = STATE_PREPARED;
        if (listener != null) listener.onPrepared((MediaPlayerWrapper)mediaPlayer);
    }

    @Override
    public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
        super.setDataSource(path);
        state = STATE_INITIALIZED;
    }

    @Override
    public void stop() throws IllegalStateException {
        super.stop();
        state = STATE_STOPPED;
    }

    public int [] getBufferedPercentage(){
        return bufferedPercentage;
    }

    public void setWrapperListener(MediaPlayerWrapperListener listener) {
        this.listener = listener;
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mediaPlayer, int i) {
        bufferedPercentage[1] = i;
        if(listener != null) listener.onBufferingUpdate((MediaPlayerWrapper)mediaPlayer,i);
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        if(listener != null) listener.onCompletion((MediaPlayerWrapper) mediaPlayer);
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i2) {
        state = STATE_ERROR;
        errorNumber++;
        return listener != null ? listener.onError((MediaPlayerWrapper)mediaPlayer,i,i2) : false;
    }

    @Override
    public boolean onInfo(MediaPlayer mediaPlayer, int i, int i2) {
        return listener != null ? listener.onInfo((MediaPlayerWrapper) mediaPlayer, i, i2) : false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mediaPlayer) {
        bufferedPercentage[0] = (int) (mediaPlayer.getCurrentPosition()*1.0/mediaPlayer.getDuration());
        if(listener != null) listener.onSeekComplete((MediaPlayerWrapper) mediaPlayer);
    }

    @Override
    public void reset(){
        super.reset();
        state = 0;
    }

    @Override
    public void prepare() throws IOException, IllegalStateException {
        state = STATE_PREPARING;
        super.prepare();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        MediaPlayerWrapper that = (MediaPlayerWrapper) o;

        if (dataSource != null ? !dataSource.equals(that.dataSource) : that.dataSource != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return dataSource != null ? dataSource.hashCode() : 0;
    }
}
