package com.example.eventboxchallenge.app;

import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.ResultReceiver;

import java.io.IOException;
import java.util.ArrayList;


public class AudioPlayerService extends Service implements MediaPlayerWrapper.MediaPlayerWrapperListener{
    public static final String TAG = "AudioPlayerService";
    /**
     * Supported actions, extras, and broadcasts
     */
    public static final String ACTION_ADD = "com.example.eventboxchallenge.app.action.ACTION_ADD";
    public static final String ACTION_PLAY = "com.example.eventboxchallenge.app.action.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.eventboxchallenge.app.action.ACTION_PAUSE";
    public static final String ACTION_PREBUFFER = "com.example.eventboxchallenge.app.action.ACTION_PREBUFFER";
    public static final String ACTION_SEEK = "com.example.eventboxchallenge.app.action.ACTION_SEEK";
    public static final String ACTION_INFO = "com.example.eventboxchallenge.app.action.ACTION_INFO";
    public static final String ACTION_RELEASE = "com.example.eventboxchallenge.app.action.ACTION_RELEASE";

    public static final String EXTRA_TRACKS = "com.example.eventboxchallenge.app.extra.TRACKS";
    public static final String EXTRA_TRACK_NUM = "com.example.eventboxchallenge.app.extra.TRACK_NUM";
    public static final String EXTRA_POSITION = "com.example.eventboxchallenge.app.extra.TRACK_POSITION";
    public static final String EXTRA_RESULT_RECEIVER = "com.example.eventboxchallenge.app.extra.RESULT_RECEIVER";
    public static final String EXTRA_WHAT = "com.example.eventboxchallenge.app.extra.WHAT";
    public static final String EXTRA_BUFFERED = "com.example.eventboxchallenge.app.extra.BUFFERED";
    public static final String EXTRA_CURRENT_TRACK = "com.example.eventboxchallenge.app.extra.CURRENT_TRACK";
    public static final String EXTRA_FAIL_CAUSE = "com.example.eventboxchallenge.app.extra.FAIL_CAUSE";
    public static final String EXTRA_TRACK_INFO = "com.example.eventboxchallenge.app.extra.EXTRA_TRACK_INFO";

    public static final String BROADCAST_START = "com.example.eventboxchallenge.app.broadcast.START";
    public static final String BROADCAST_PAUSE = "com.example.eventboxchallenge.app.broadcast.PAUSE";
    public static final String BROADCAST_BUFFERING_UPDATE = "com.example.eventboxchallenge.app.broadcast.BUFFERING_UPDATE";
    public static final String BROADCAST_COMPLETE = "com.example.eventboxchallenge.app.broadcast.COMPLETE";
    public static final String BROADCAST_POSITION = "com.example.eventboxchallenge.app.broadcast.POSITION";
    public static final String BROADCAST_TRACK_FAILED = "com.example.eventboxchallenge.app.broadcast.TRACK_FAILED";

    public static final int ERROR_BAD_SRC = 1;
    public static final int ERROR_MAX_TRIES = 2;

    /**
     * Public constants
     */
    public static final int CURRENT_TRACK = -1;
    public static final int NEXT_TRACK = -2;
    public static final int PREVIOUS_TRACK = -3;
    public static final String WHAT_DURATION = "duration";
    public static final String WHAT_BUFFERED = "buffered";
    public static final String WHAT_POSITION = "position";
    public static final String WHAT_DATA_SOURCE = "data-source";
    public static final String WHAT_AUDIO_SESSION = "audio-session";
    public static final String WHAT_ALL = "all";
    public static final int RESULT_SUCCESS = 1;


    /**
     * Private fields
     */
    private ArrayList<MediaPlayerWrapper> playerList = new ArrayList<MediaPlayerWrapper>();
    private int currentTrack = 0;
    private static final int positionUpdateInterval = 1000;
    private static final int UPDATE_PROGRESS = 123;
    private static final int maxTries = 3;

    /**
     * Handler to broadcast the position of the current track every positionUpdateInterval
     */
    protected Handler positionHandler = new Handler()
    {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == UPDATE_PROGRESS){
                if(playerList != null) {
                    MediaPlayerWrapper player = playerList.get(currentTrack);
                    if (player.getState() != MediaPlayerWrapper.STATE_ERROR && player.isPlaying()) {
                        int currentPosition = player.getCurrentPosition();
                        broadcastTrackPosition(currentTrack, currentPosition);
                        msg = obtainMessage(UPDATE_PROGRESS);
                        sendMessageDelayed(msg, positionUpdateInterval);
                    }
                }
            }
        }
    };

    /**
     * Static helper methods.
     */
    public static void startActionPlay(Context context, int trackNum) {
        Intent intent = new Intent(context, AudioPlayerService.class);
        intent.setAction(ACTION_PLAY);
        intent.putExtra(EXTRA_TRACK_NUM,trackNum);
        context.startService(intent);
    }

    public static void startActionAdd(Context context, String [] tracks) {
        Intent intent = new Intent(context, AudioPlayerService.class);
        intent.setAction(ACTION_ADD);
        intent.putExtra(EXTRA_TRACKS,tracks);
        context.startService(intent);
    }

    public static void startActionPause(Context context, int trackNum) {
        Intent intent = new Intent(context, AudioPlayerService.class);
        intent.setAction(ACTION_PAUSE);
        intent.putExtra(EXTRA_TRACK_NUM,trackNum);
        context.startService(intent);
    }

    public static void startActionPrebuffer(Context context, int trackNum) {
        Intent intent = new Intent(context, AudioPlayerService.class);
        intent.setAction(ACTION_PREBUFFER);
        intent.putExtra(EXTRA_TRACK_NUM,trackNum);
        context.startService(intent);
    }

    public static void startActionSeek(Context context, int trackNum, int millis) {
        Intent intent = new Intent(context, AudioPlayerService.class);
        intent.setAction(ACTION_SEEK);
        intent.putExtra(EXTRA_TRACK_NUM, trackNum);
        intent.putExtra(EXTRA_POSITION,millis);
        context.startService(intent);
    }

    public static void startActionInfo(Context context, int trackNum, String what, ResultReceiver receiver) {
        Intent intent = new Intent(context, AudioPlayerService.class);
        intent.setAction(ACTION_INFO);
        intent.putExtra(EXTRA_RESULT_RECEIVER, receiver);
        intent.putExtra(EXTRA_TRACK_NUM,trackNum);
        intent.putExtra(EXTRA_WHAT,what);
        context.startService(intent);
    }

    public static void startActionRelease(Context context) {
        Intent intent = new Intent(context, AudioPlayerService.class);
        context.startService(intent);
    }

    public AudioPlayerService() {
        super();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)  {
        if (intent != null) {
            final String action = intent.getAction();
            if(action != null) {
                if (ACTION_ADD.equals(action)) {
                    final int trackNum = translateTrackNumber(intent.getIntExtra(EXTRA_TRACK_NUM, playerList.size()));
                    final String[] tracks = intent.getStringArrayExtra(EXTRA_TRACKS);
                    handleActionAdd(tracks);

                } else if (ACTION_PLAY.equals(action)) {
                    final int trackNum = translateTrackNumber(intent.getIntExtra(EXTRA_TRACK_NUM, playerList.size()));
                    handleActionPlay(trackNum);

                } else if (ACTION_PAUSE.equals(action)) {
                    final int trackNum = translateTrackNumber(intent.getIntExtra(EXTRA_TRACK_NUM, playerList.size()));
                    handleActionPause(trackNum);

                } else if (ACTION_PREBUFFER.equals(action)) {
                    final int trackNum = translateTrackNumber(intent.getIntExtra(EXTRA_TRACK_NUM, playerList.size()));
                    handleActionPrebuffer(trackNum);

                } else if (ACTION_SEEK.equals(action)) {
                    final int millis = intent.getIntExtra(EXTRA_POSITION, 0);
                    final int trackNum = translateTrackNumber(intent.getIntExtra(EXTRA_TRACK_NUM, playerList.size()));
                    handleActionSeek(trackNum, millis);

                } else if (ACTION_INFO.equals(action)) {
                    String what = intent.getStringExtra(EXTRA_WHAT);
                    final int trackNum = translateTrackNumber(intent.getIntExtra(EXTRA_TRACK_NUM, playerList.size()));
                    ResultReceiver resultReceiver = intent.getParcelableExtra(EXTRA_RESULT_RECEIVER);
                    handleActionInfo(trackNum, what, resultReceiver);

                } else if (ACTION_RELEASE.equals(action)) {
                    handleActionRelease();

                }
            }
        }

        return Service.START_STICKY;
    }

    /**
     * Handler methods
     */
    private void handleActionAdd(String [] trackUrlsList) {
        if(playerList == null) playerList = new ArrayList<MediaPlayerWrapper>();

        for(String trackUrl : trackUrlsList){
            playerList.add(loadTrackIntoPlayer(trackUrl));
        }
    }

    private void handleActionPlay(int trackNum){
        currentTrack = trackNum;
        MediaPlayerWrapper player = playerList.get(trackNum);
        if(player.getState() == MediaPlayerWrapper.STATE_PREPARED) {
            player.start();
            broadcastTrackStart(trackNum);
            positionHandler.sendEmptyMessage(UPDATE_PROGRESS);
        }
        else if(player.getState() == MediaPlayerWrapper.STATE_INITIALIZED ||
                player.getState() == MediaPlayerWrapper.STATE_STOPPED) player.prepareAsync();
        else {
            MediaPlayerWrapper newPlayer = loadTrackIntoPlayer(player.getDataSource());
            playerList.get(trackNum).release();
            playerList.set(trackNum,newPlayer);
            newPlayer.prepareAsync();
        }
    }

    private void handleActionPause(int trackNum){
        currentTrack = trackNum;
        MediaPlayerWrapper player = playerList.get(trackNum);
        if(player.getState() == MediaPlayerWrapper.STATE_PREPARED && player.isPlaying()){
            player.pause();
            broadcastTrackPause(trackNum);
        }
    }

    private void handleActionPrebuffer(int trackNum){
        MediaPlayerWrapper nextPlayer = playerList.get(trackNum);
        if (nextPlayer.getState() == MediaPlayerWrapper.STATE_INITIALIZED ||
                nextPlayer.getState() == MediaPlayerWrapper.STATE_STOPPED) {
            try {
                nextPlayer.prepareAsync();
            } catch (IllegalStateException e) {
               e.printStackTrace();
            }
        } else if (nextPlayer.getState() == MediaPlayerWrapper.STATE_ERROR) {
            String src = nextPlayer.getDataSource();
            nextPlayer.reset();
            try {
                nextPlayer.setDataSource(src);
                nextPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        } else if (nextPlayer.getState() == MediaPlayerWrapper.STATE_IDLE) {
            String src = nextPlayer.getDataSource();
            try {
                nextPlayer.setDataSource(src);
                nextPlayer.prepareAsync();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleActionSeek(int trackNum, int millis){
        MediaPlayerWrapper player = playerList.get(trackNum);
        if(player.getState() == MediaPlayerWrapper.STATE_PREPARED) player.seekTo(millis);
    }

    private void handleActionInfo(int trackNum, String what, ResultReceiver resultReceiver){
        Bundle info = bundleTrackInfo(trackNum, what);
        resultReceiver.send(RESULT_SUCCESS,info);
    }

    private void handleActionRelease() {
        for (MediaPlayerWrapper player : playerList){
            player.release();
            player = null;
        }

        playerList = new ArrayList<MediaPlayerWrapper>();
    }

    /**
     * Event broadcasts
     */
    private void broadcastTrackStart(int trackNum){
        Intent i = new Intent(BROADCAST_START);
        i.putExtra(EXTRA_TRACK_NUM,trackNum);
        i.putExtra(EXTRA_CURRENT_TRACK,currentTrack);
        i.putExtra(EXTRA_TRACK_INFO,bundleTrackInfo(trackNum,WHAT_ALL));
        this.sendBroadcast(i);
    }

    private void broadcastTrackPause(int trackNum){
        Intent i = new Intent(BROADCAST_PAUSE);
        i.putExtra(EXTRA_TRACK_NUM,trackNum);
        i.putExtra(EXTRA_CURRENT_TRACK,currentTrack);
        this.sendBroadcast(i);
    }

    private void broadcastBufferingUpdate(int trackNum, int percentage){
        Intent i = new Intent(BROADCAST_BUFFERING_UPDATE);
        i.putExtra(EXTRA_TRACK_NUM,trackNum);
        i.putExtra(EXTRA_BUFFERED,percentage);
        i.putExtra(EXTRA_CURRENT_TRACK,currentTrack);
        this.sendBroadcast(i);
    }

    private void broadcastTrackComplete(int trackNum){
        Intent i = new Intent(BROADCAST_COMPLETE);
        i.putExtra(EXTRA_TRACK_NUM,trackNum);
        i.putExtra(EXTRA_CURRENT_TRACK,currentTrack);
        this.sendBroadcast(i);
    }

    private void broadcastTrackPosition(int trackNum, int position){
        Intent i = new Intent(BROADCAST_POSITION);
        i.putExtra(EXTRA_TRACK_NUM,trackNum);
        i.putExtra(EXTRA_POSITION,position);
        this.sendBroadcast(i);
    }

    private void broadcastTrackFailed(int trackNum, int cause){
        Intent i = new Intent(BROADCAST_TRACK_FAILED);
        i.putExtra(EXTRA_TRACK_NUM,trackNum);
        i.putExtra(EXTRA_CURRENT_TRACK,currentTrack);
        i.putExtra(EXTRA_FAIL_CAUSE,cause);
        this.sendBroadcast(i);
    }

    /**
     * Private helper methods
     */
    private MediaPlayerWrapper loadTrackIntoPlayer(String trackUrl) {
        MediaPlayerWrapper player = null;

        try {
            player = new MediaPlayerWrapper(trackUrl);
            player.setWrapperListener(this);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return player;
    }

    private int translateTrackNumber(int trackNum){
        if(trackNum == CURRENT_TRACK) trackNum = currentTrack;
        else if(trackNum == PREVIOUS_TRACK) trackNum = currentTrack - 1;
        else if(trackNum == NEXT_TRACK) trackNum = currentTrack + 1;

        if(trackNum >= playerList.size()) trackNum = 0;
        else if (trackNum < 0) trackNum = playerList.size() - 1;

        return trackNum;
    }

    private Bundle bundleTrackInfo(int trackNum, String what){
        Bundle info = new Bundle();
        MediaPlayerWrapper player = playerList.get(currentTrack);
        if(what.equals(WHAT_DURATION) || what.equals(WHAT_ALL)) info.putInt(WHAT_DURATION,player.getDuration());
        if(what.equals(WHAT_POSITION) || what.equals(WHAT_ALL)) info.putInt(WHAT_POSITION, player.getCurrentPosition());
        if(what.equals(WHAT_BUFFERED) || what.equals(WHAT_ALL)) info.putIntArray(WHAT_BUFFERED, player.getBufferedPercentage());
        if(what.equals(WHAT_DATA_SOURCE) || what.equals(WHAT_ALL)) info.putString(WHAT_DATA_SOURCE, player.getDataSource());
        if(what.equals(WHAT_AUDIO_SESSION) || what.equals(WHAT_ALL)) info.putInt(WHAT_AUDIO_SESSION, player.getAudioSessionId());
        return info;
    }

    private void removePlayer(int trackNum){
        if(currentTrack >= trackNum) currentTrack--;
        if(currentTrack == trackNum) playerList.get(trackNum).stop();
        playerList.get(trackNum).release();
        playerList.remove(trackNum);
    }


    /**
     * MediaPlayerWrapper event listeners.
     */
    @Override
    public void onBufferingUpdate(MediaPlayerWrapper player, int percentage) {
        int playerNum = playerList.indexOf(player);
        broadcastBufferingUpdate(playerNum, percentage);
    }

    @Override
    public void onCompletion(MediaPlayerWrapper player) {
        int playerNum = playerList.indexOf(player);
        broadcastTrackComplete(playerNum);
    }

    @Override
    public void onPrepared(MediaPlayerWrapper player) {
        int playerNum = playerList.indexOf(player);
        if(currentTrack == playerNum) {
            player.start();
            broadcastTrackStart(playerNum);
            positionHandler.sendEmptyMessage(UPDATE_PROGRESS);

        }
    }

    @Override
    public boolean onError(MediaPlayerWrapper player, int what, int extra) {
        if(player.getNumberOfErrors() > maxTries) {
            int playerNum = playerList.indexOf(player);
            String path = player.getDataSource();
            player.reset();
            try {
                player.setDataSource(path);
                player.prepareAsync();
            } catch (IOException e) {
                if (player.getNumberOfErrors() > maxTries) {
                    broadcastTrackFailed(playerNum, ERROR_BAD_SRC);
                    removePlayer(playerNum);
                }
            }
        }
        return true;
    }


    /**
     * Unimplemented
     */
    @Override
    public boolean onInfo(MediaPlayerWrapper player, int what, int extra) {
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayerWrapper mp) {

    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
