package com.example.eventboxchallenge.app;

import android.animation.Animator;
import android.media.audiofx.Visualizer;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;


public class MainActivity extends ActionBarActivity implements AudioPlaylist.AudioPlaylistStateListener{
    private static final String TAG = "MainActivity";
    private Visualizer mVisualizer;
    private SeekBar seekBar;
    private TextView artistsNameView;
    private TextView trackTitleView;
    private ImageView largeArtworkView;
    private ImageView previewArtwork;
    private TextView currentPositionView;
    private TextView trackDurationView;
    private AudioPlaylist playlist;
    private int trackDurationMills;
    private boolean isPlaying = false;

    /**
     * Activity lifecycle
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_alt);
        // set up the playlist
        playlist = AudioPlaylist.createFromStaticData(this);
        playlist.setContinuousPlay(true);
        playlist.setLoopTracks(true);
        playlist.setPlaylistStateListener(this);
        playlist.setPrebufferPercentage(70);
        playlist.startCurrentTrack();

        // set up button callbacks
        seekBar = (SeekBar) findViewById(R.id.mediaSeekBar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(fromUser) {
                    int dest = (int) (trackDurationMills * progress * 1.0 / seekBar.getMax());
                    playlist.seekToInCurrentTrack(dest);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        artistsNameView = (TextView) findViewById(R.id.artistName);
        trackTitleView = (TextView) findViewById(R.id.trackTitle);
        largeArtworkView = (ImageView) findViewById(R.id.largeArtwork);
        previewArtwork = (ImageView) findViewById(R.id.previewArtwork);

        largeArtworkView.setOnTouchListener(new ArtworkTouchListener(){
            private boolean dragNext;
            private float factor = 1.5f;
            @Override
            public void onClick(View v) {
                if(isPlaying) playlist.pauseCurrentTrack();
                else playlist.startCurrentTrack();
            }

            @Override
            public void onDrag(View v, int deltaX) {
                Log.d(TAG, String.valueOf(deltaX));
                Display display = getWindowManager().getDefaultDisplay();
                int width = display.getWidth();
               ((ImageView) v).setX(deltaX * factor);
                if(previewArtwork.getVisibility() == View.GONE) previewArtwork.setVisibility(View.VISIBLE);
                if(deltaX < 0) {
                    Picasso.with(MainActivity.this)
                            .load(playlist.getTrackArtworkLargeUrl(playlist.NEXT_TRACK))
                            .placeholder(R.drawable.artwork_placeholder)
                            .error(R.drawable.artwork_placeholder)
                            .into(previewArtwork);
                    previewArtwork.setX(width + deltaX * factor);
                } else {
                    Picasso.with(MainActivity.this)
                            .load(playlist.getTrackArtworkLargeUrl(playlist.PREVIOUS_TRACK))
                            .placeholder(R.drawable.artwork_placeholder)
                            .error(R.drawable.artwork_placeholder)
                            .into(previewArtwork);
                    previewArtwork.setX(deltaX * factor - width );
                }
            }

            @Override
            public void onDragStop(View v, int deltaX, MotionEvent e) {
                Display display = getWindowManager().getDefaultDisplay();
                final int width = display.getWidth();

                if(deltaX < 0 && deltaX > -0.3*width) {
                    previewArtwork.animate().x(width).setDuration(200).setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            previewArtwork.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {

                        }
                    });
                    ((ImageView) v).animate().x(0).setDuration(200);
                } else if (deltaX >= 0 && deltaX < 0.3*width ) {
                    previewArtwork.animate().x(-width).setDuration(200).setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            previewArtwork.setVisibility(View.GONE);
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {

                        }
                    });
                    ((ImageView) v).animate().x(0).setDuration(200);
                } else if(deltaX <= -0.3*width){
                    previewArtwork.animate().x(0).setDuration(200).setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            Picasso.with(MainActivity.this)
                                    .load(playlist.getTrackArtworkLargeUrl(playlist.NEXT_TRACK))
                                    .placeholder(R.drawable.artwork_placeholder)
                                    .error(R.drawable.artwork_placeholder)
                                    .into(largeArtworkView);
                            largeArtworkView.setX(0);
                            previewArtwork.setVisibility(View.GONE);
                            previewArtwork.setX(width);
                            playlist.playNext();
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {

                        }
                    });
                    ((ImageView) v).animate().x(-width).setDuration(200);
                } else if(deltaX >= 0.3*width) {
                    previewArtwork.animate().x(0).setDuration(200).setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {

                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            Picasso.with(MainActivity.this)
                                    .load(playlist.getTrackArtworkLargeUrl(playlist.PREVIOUS_TRACK))
                                    .placeholder(R.drawable.artwork_placeholder)
                                    .error(R.drawable.artwork_placeholder)
                                    .into(largeArtworkView);
                            largeArtworkView.setX(0);
                            previewArtwork.setVisibility(View.GONE);
                            previewArtwork.setX(-width);
                            playlist.playPrevious();
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {

                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {

                        }
                    });
                    ((ImageView) v).animate().x(width).setDuration(200);
                }

            }
        });

        trackDurationView = (TextView) findViewById(R.id.trackDuration);

        currentPositionView = (TextView) findViewById(R.id.trackPosition);
    }

    @Override
    protected void onResume() {
        super.onResume();
        playlist.registerAsReceiver();
    }

    @Override
    public void onPause(){
        super.onPause();
//        playlist.pauseCurrentTrack();
        unregisterReceiver(playlist);
    }

    @Override
    public void onStop(){
        super.onStop();
//        playlist.release();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Respond to playlist state events
     */
    @Override
    public void onCurrentTrackStart(int trackNum, Bundle info) {
        isPlaying = true;
//        currentTrackView.setText("Playing: " + info.getString(AudioPlayerService.WHAT_DATA_SOURCE));
//        playButton.setImageResource(R.drawable.pause_button);
        artistsNameView.setText(playlist.getTrackArtist(trackNum));
        trackTitleView.setText(playlist.getTrackTitle(trackNum));
        Picasso.with(this)
                .load(playlist.getTrackArtworkLargeUrl(trackNum))
                .placeholder(R.drawable.artwork_placeholder)
                .error(R.drawable.artwork_placeholder)
                .into(largeArtworkView);


//        int sessionId = info.getInt(AudioPlayerService.WHAT_AUDIO_SESSION);
//        mVisualizer=new Visualizer(sessionId);
//
//        mVisualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
//        mVisualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
//
//            @Override
//            public void onWaveFormDataCapture(Visualizer visualizer, byte[] bytes,
//                                              int samplingRate) {
//                Log.d(TAG,String.valueOf(bytes[0]));
//            }
//
//            @Override
//            public void onFftDataCapture(Visualizer visualizer, byte[] fft,
//                                         int samplingRate) {
//
//            }
//        }, Visualizer.getMaxCaptureRate()/2, true,false);


        int [] bufferedSection = info.getIntArray(AudioPlayerService.WHAT_BUFFERED);
        onCurrentTrackBufferUpdate(bufferedSection[1]);

        int millisDuration = info.getInt(AudioPlayerService.WHAT_DURATION);
        trackDurationMills = millisDuration;
        long second = (millisDuration / 1000) % 60;
        long minute = (millisDuration / (1000 * 60)) % 60;
        trackDurationView.setText(String.format("/%02d:%02d",minute,second));

        int millisPosition = info.getInt(AudioPlayerService.WHAT_POSITION);
        second = (millisPosition / 1000) % 60;
        minute = (millisPosition / (1000 * 60)) % 60;
        currentPositionView.setText(String.format("%02d:%02d",minute,second));
    }

    @Override
    public void onCurrentTrackPause(int trackNum) {
        isPlaying = false;
//        currentTrackView.setText("Paused");
//        playButton.setImageResource(R.drawable.play_button);
    }

    @Override
    public void onCurrentTrackBufferUpdate(int percentage) {
        seekBar.setSecondaryProgress((int) (percentage * seekBar.getMax() * 1.0 / 100));
    }

    @Override
    public void onCurrentTrackError(int trackNum, int cause) {
//        currentTrackView.setText("Error with current track");
    }

    @Override
    public void onCurrentTrackPositionChange(int millis) {
        if(trackDurationMills != 0){
            int progress = (int) (seekBar.getMax() * millis * 1.0 / trackDurationMills);
            seekBar.setProgress(progress);
            long second = (millis / 1000) % 60;
            long minute = (millis / (1000 * 60)) % 60;
            currentPositionView.setText(String.format("%02d:%02d",minute,second));
        }
    }
}
