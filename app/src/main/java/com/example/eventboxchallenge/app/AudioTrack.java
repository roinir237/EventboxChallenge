package com.example.eventboxchallenge.app;

import android.content.Context;
import android.graphics.Bitmap;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by roinir.
 */
public class AudioTrack {

    public static final int ORIGIN_SOUNDCLOUD = 1;

    private static final String CLIENT_ID = "5c43a6cf3ad30678c69b66823fba5836";
    private static final String ClIENT_SECRET = "70b2578d43f1306680674ef299cff62e";

    private String artist;
    private String title;
    private String streamUrl;
    private String artworkLarge;
    private String artworkThumbnail;

    public AudioTrack(int origin, JSONObject data, Context context) throws IOException, JSONException{
        switch (origin){
            case ORIGIN_SOUNDCLOUD:
                parseSoundcloudData(data,context);
                break;
        }
    }

    private void parseSoundcloudData(JSONObject data, Context context) throws IOException, JSONException{
        artist = data.getJSONObject("user").getString("username");
        title = data.getString("title");
        streamUrl = data.getString("stream-url") + "?client_id=" + CLIENT_ID;
        artworkThumbnail = data.getString("artwork-url");
        artworkLarge =artworkThumbnail.replace("large","t500x500");
    }

    public String getArtist() {
        return artist;
    }

    public String getTitle() {
        return title;
    }

    public String getStreamUrl() {
        return streamUrl;
    }

    public String getArtworkLarge() {
        return artworkLarge;
    }

    public String getArtworkThumbnail() {
        return artworkThumbnail;
    }

    public static ArrayList<AudioTrack> parseJSONArray(JSONArray data, int origin, Context context) {
        ArrayList<AudioTrack> tracks = new ArrayList<AudioTrack>();

        for(int i = 0; i < data.length(); i++){
            try {
                JSONObject trackData = data.getJSONObject(i);
                AudioTrack track = new AudioTrack(origin, trackData, context);
                tracks.add(track);
            } catch (IOException e){
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        return tracks;
    }
}
