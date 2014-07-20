package com.example.eventboxchallenge.app;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by roinir.
 */
public class AudioPlaylist extends BroadcastReceiver{
    private static final String TAG = "AudioPlaylist";

    /**
     * Public constants
     */
    public static final int CURRENT_TRACK = -1;
    public static final int NEXT_TRACK = -2;
    public static final int PREVIOUS_TRACK = -3;
    public static final int NO_PREBUFFER = 101;

    /**
     * Private fields
     */
    private final Context mContext;
    private int currentTrack = 0;
    private boolean loopTracks = false;
    private boolean continuousPlay = true;
    private int preBuffer = NO_PREBUFFER;
    private ArrayList<AudioTrack> trackList;
    private AudioPlaylistStateListener mListener;
    private IntentFilter intentFilter = new IntentFilter();

    public interface AudioPlaylistStateListener {
        void onCurrentTrackStart(int trackNum, Bundle info);
        void onCurrentTrackPause(int trackNum);
        void onCurrentTrackBufferUpdate(int percentage);
        void onCurrentTrackPositionChange(int millis);
        void onCurrentTrackError(int trackNum, int cause);
    }

    public interface AudioPlaylistInfoListener {
        void onTrackInfoAvailable(int trackNum, Bundle info);
    }


    /**
     * Constructors
     */
    public AudioPlaylist (Context context, AudioTrack [] tracks) {
        mContext = context;
        trackList = new ArrayList<AudioTrack>();
        mContext.startService(new Intent(mContext,AudioPlayerService.class));

        addTracks(tracks);
    }

    public AudioPlaylist (Context context) {
        trackList = new ArrayList<AudioTrack>();
        mContext = context;
        mContext.startService(new Intent(mContext,AudioPlayerService.class));
    }

    public static AudioPlaylist createFromStaticData(Context context){
        String dataString = "[" +
                "{'title':'Martin Solveig - The Night Out (Madeon Remix)','stream-url':'http://api.soundcloud.com/tracks/40493181/stream','artwork-url':'http://i1.sndcdn.com/artworks-000020311712-1llrn7-large.jpg?671e660','user':{'username':'Madeon'}}," +
                "{'title':'Addicted To You','stream-url':'http://api.soundcloud.com/tracks/110076793/stream','artwork-url':'http://i1.sndcdn.com/artworks-000057612216-7sefkf-large.jpg?671e660','user':{'username':'Avicii \"True\" Album'}}," +
                "{'title':'MNEK x Disclosure - White Noise (XYconstant Remix)','stream-url':'http://api.soundcloud.com/tracks/105997504/stream','artwork-url':'http://i1.sndcdn.com/artworks-000055628311-gsf7jl-large.jpg?671e660','user':{'username':'XYconstant'}}," +
                "{'title':'Sing To The Moon - Laura Mvula (Klangkarussell Remix)','stream-url':'http://api.soundcloud.com/tracks/115914097/stream','artwork-url':'http://i1.sndcdn.com/artworks-000061547365-dc06l8-large.jpg?671e660','user':{'username':'Klangkarussell'}}" +
                "]";
        try {
            JSONArray data = new JSONArray(dataString);
            ArrayList<AudioTrack> tracks = AudioTrack.parseJSONArray(data,AudioTrack.ORIGIN_SOUNDCLOUD,context);
            return new AudioPlaylist(context,tracks.toArray(new AudioTrack[tracks.size()]));
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Public api
     */
    public void registerAsReceiver(){
        mContext.registerReceiver(this,addActionsToFilter(intentFilter));
    }

    public void addTracks(AudioTrack [] tracks) {
        ArrayList<String> trackUrlsList = new ArrayList<String>();
        for(AudioTrack track : tracks){
            trackUrlsList.add(track.getStreamUrl());
            trackList.add(track);
        }
        AudioPlayerService.startActionAdd(mContext,trackUrlsList.toArray(new String[trackUrlsList.size()]));
    }

    public void addTrack(AudioTrack track){
        AudioTrack [] tracks = {track};
        addTracks(tracks);
    }

    public void startCurrentTrack(){
        AudioPlayerService.startActionPlay(mContext,AudioPlayerService.CURRENT_TRACK);
    }

    public void pauseCurrentTrack(){
        AudioPlayerService.startActionPause(mContext,AudioPlayerService.CURRENT_TRACK);
    }

    public void skipTo(int trackNum) {
        AudioPlayerService.startActionPause(mContext,AudioPlayerService.CURRENT_TRACK);
        AudioPlayerService.startActionPlay(mContext,trackNum);
    }

    public void seekToInCurrentTrack(int millis){
        AudioPlayerService.startActionSeek(mContext,AudioPlayerService.CURRENT_TRACK,millis);
    }

    public void playNext(){
        skipTo(AudioPlayerService.NEXT_TRACK);
    }

    public void playPrevious(){
        skipTo(AudioPlayerService.PREVIOUS_TRACK);
    }

    public void setLoopTracks(boolean loop){
        this.loopTracks = loop;
    }

    public void setContinuousPlay(boolean continuous) {
        this.continuousPlay = continuous;
    }

    public void setPlaylistStateListener(AudioPlaylistStateListener listener){
        mListener = listener;
    }

    public void release(){
        AudioPlayerService.startActionRelease(mContext);
    }

    public void requestTrackInfo(final int trackNum, final AudioPlaylistInfoListener listener){
        ResultReceiver receiver = new ResultReceiver(new Handler()){
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if(resultCode == AudioPlayerService.RESULT_SUCCESS){
                    if(listener != null) listener.onTrackInfoAvailable(trackNum,resultData);
                }
            }
        };
        AudioPlayerService.startActionInfo(mContext,trackNum,AudioPlayerService.WHAT_ALL,receiver);
    }

    public void requestCurrentTrackInfo(final AudioPlaylistInfoListener listener){
        requestTrackInfo(AudioPlayerService.CURRENT_TRACK,listener);
    }

    public void setPrebufferPercentage(int percentage){
        preBuffer = percentage;
    }

    public String getTrackTitle(int trackNum){
        trackNum = translateTrackNumber(trackNum);
        return trackList.get(trackNum).getTitle();
    }

    public String getTrackArtist(int trackNum) {
        trackNum = translateTrackNumber(trackNum);
        return trackList.get(trackNum).getArtist();
    }

    public String getTrackArtworkLargeUrl(int trackNum) {
        trackNum = translateTrackNumber(trackNum);
        return trackList.get(trackNum).getArtworkLarge();
    }

    public String getTrackArtworkThumbnailUrl(int trackNum) {
        trackNum = translateTrackNumber(trackNum);
        return trackList.get(trackNum).getArtworkThumbnail();
    }

    /**
     *  Private methods
     */
    private void prebufferTrack(int trackNum){
        AudioPlayerService.startActionPrebuffer(mContext,trackNum);
    }

    private static IntentFilter addActionsToFilter(IntentFilter filter){
        filter.addAction(AudioPlayerService.BROADCAST_BUFFERING_UPDATE);
        filter.addAction(AudioPlayerService.BROADCAST_COMPLETE);
        filter.addAction(AudioPlayerService.BROADCAST_PAUSE);
        filter.addAction(AudioPlayerService.BROADCAST_POSITION);
        filter.addAction(AudioPlayerService.BROADCAST_START);
        filter.addAction(AudioPlayerService.BROADCAST_TRACK_FAILED);
        return filter;
    }

    private int translateTrackNumber(int trackNum){
        if(trackNum == CURRENT_TRACK) trackNum = currentTrack;
        else if(trackNum == PREVIOUS_TRACK) trackNum = currentTrack - 1;
        else if(trackNum == NEXT_TRACK) trackNum = currentTrack + 1;

        if(trackNum >= trackList.size()) trackNum = 0;
        else if (trackNum < 0) trackNum = trackList.size() - 1;

        return trackNum;
    }

    /**
     * Listen to broadcasts from AudioPlayerService and respond accordingly
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action != null) {
            if (action.equals(AudioPlayerService.BROADCAST_START)) {
                int trackNum = intent.getIntExtra(AudioPlayerService.EXTRA_TRACK_NUM, -1);
                currentTrack = intent.getIntExtra(AudioPlayerService.EXTRA_CURRENT_TRACK, -2);
                if (trackNum == currentTrack && mListener != null) {
                    Bundle info = intent.getBundleExtra(AudioPlayerService.EXTRA_TRACK_INFO);
                    mListener.onCurrentTrackStart(trackNum,info);
                    if(info != null) {
                        int[] buffered = info.getIntArray(AudioPlayerService.EXTRA_BUFFERED);
                        if (buffered != null && buffered[1] > preBuffer && currentTrack + 1 < trackList.size())
                            prebufferTrack(AudioPlayerService.NEXT_TRACK);
                    }
                }

            } else if (action.equals(AudioPlayerService.BROADCAST_PAUSE)) {
                int trackNum = intent.getIntExtra(AudioPlayerService.EXTRA_TRACK_NUM, -1);
                int currentTrack = intent.getIntExtra(AudioPlayerService.EXTRA_CURRENT_TRACK, -2);
                if (trackNum == currentTrack && mListener != null)
                    mListener.onCurrentTrackPause(trackNum);

            } else if (action.equals(AudioPlayerService.BROADCAST_COMPLETE)) {
                int trackNum = intent.getIntExtra(AudioPlayerService.EXTRA_TRACK_NUM, -1);
                int currentTrack = intent.getIntExtra(AudioPlayerService.EXTRA_CURRENT_TRACK, -2);

                if (trackNum == currentTrack && continuousPlay && (currentTrack + 1 < trackList.size() || loopTracks)) {
                    int nextTrack = currentTrack + 1 < trackList.size() ? AudioPlayerService.NEXT_TRACK : 0;
                    AudioPlayerService.startActionPlay(mContext, nextTrack);

                }

            } else if (action.equals(AudioPlayerService.BROADCAST_BUFFERING_UPDATE)) {
                int trackNum = intent.getIntExtra(AudioPlayerService.EXTRA_TRACK_NUM, -1);
                int currentTrack = intent.getIntExtra(AudioPlayerService.EXTRA_CURRENT_TRACK, -2);
                int percentage = intent.getIntExtra(AudioPlayerService.EXTRA_BUFFERED, 0);
                if (trackNum == currentTrack && mListener != null) {
                    if(preBuffer <= percentage && currentTrack + 1 < trackList.size())
                        prebufferTrack(AudioPlayerService.NEXT_TRACK);
                    mListener.onCurrentTrackBufferUpdate(percentage);
                }

            } else if (action.equals(AudioPlayerService.BROADCAST_POSITION)) {
                int millis = intent.getIntExtra(AudioPlayerService.EXTRA_POSITION, 0);
                if(mListener != null) mListener.onCurrentTrackPositionChange(millis);

            } else if (action.equals(AudioPlayerService.BROADCAST_TRACK_FAILED)) {
                int trackNum = intent.getIntExtra(AudioPlayerService.EXTRA_TRACK_NUM, -1);
                int currentTrack = intent.getIntExtra(AudioPlayerService.EXTRA_CURRENT_TRACK, -2);
                int cause = intent.getIntExtra(AudioPlayerService.EXTRA_FAIL_CAUSE, 0);
                if (trackNum == currentTrack && mListener != null)
                    mListener.onCurrentTrackError(trackNum, cause);

            }
        }
    }
}
