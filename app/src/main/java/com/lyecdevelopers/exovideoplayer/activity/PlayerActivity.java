package com.lyecdevelopers.exovideoplayer.activity;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.WindowManager;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.LoadControl;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.LoopingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;
import com.lyecdevelopers.exovideoplayer.R;


/**
 * A fullscreen activity to play audio or video streams.
 */
public class PlayerActivity extends AppCompatActivity {

    public static final String VIDEO_TYPE = "video_type";
    public static final String VIDEO_URI = "video_uri";
    public static final String SUBTITLE_URI = "subtitle_uri";
    public static final String VIDEO_URI_LIST = "video_uri_list";
    public static final int SIMPLE_VIDEO = 101;
    public static final int VIDEO_WITH_SUBTITLE = 102;
    public static final int LOOPING_VIDEO = 103;
    public static final int SEQUENTIAL_VIDEO = 104;
    // bandwidth meter to measure and estimate bandwidth
    private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
    private static final String TAG = "PlayerActivity";
    private SimpleExoPlayer player;
    private SimpleExoPlayerView playerView;

    private long playbackPosition;
    private int currentWindow;
    private boolean playWhenReady = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.player_activity);

        playerView = (SimpleExoPlayerView) findViewById(R.id.player_view);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (Util.SDK_INT > 23) {
            initializePlayer();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if ((Util.SDK_INT <= 23 || player == null)) {
            initializePlayer();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (Util.SDK_INT <= 23) {
            releasePlayer();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (Util.SDK_INT > 23) {
            releasePlayer();
        }
    }

    private void initializePlayer() {
        if (player == null) {

            // 1. Create a default TrackSelector
            Handler mainHandler = new Handler();
            BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
            TrackSelection.Factory videoTrackSelectionFactory = new AdaptiveTrackSelection.Factory(bandwidthMeter);
            TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

            // 2. Create a default LoadControl
            LoadControl loadControl = new DefaultLoadControl();

            // 3. Create the player

            player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, loadControl);

            playerView = findViewById(R.id.player_view);
            playerView.setPlayer(player);

            player.setPlayWhenReady(playWhenReady);
            player.seekTo(currentWindow, playbackPosition);


        }

        int videoType = getIntent().getIntExtra(VIDEO_TYPE, SIMPLE_VIDEO);
        MediaSource mediaSource = getMediaSource(videoType);

        player.prepare(mediaSource, true, false);
    }

    private void releasePlayer() {
        if (player != null) {
            playbackPosition = player.getCurrentPosition();
            currentWindow = player.getCurrentWindowIndex();
            playWhenReady = player.getPlayWhenReady();
            player.release();
            player = null;
        }
    }


    public MediaSource getMediaSource(int videoType) {
        String videoUri = getIntent().getStringExtra(VIDEO_URI);
        String subtitleUri = getIntent().getStringExtra(SUBTITLE_URI);
        String[] videoUriList = getIntent().getStringArrayExtra(VIDEO_URI_LIST);
        Log.d(TAG, "initializePlayer videoUri : " + videoUri + ", subtitleUri : " + subtitleUri);


        MediaSource mediaSource = null;
        switch (videoType) {
            case SIMPLE_VIDEO:
                mediaSource = buildMediaSource(Uri.parse(videoUri));
                break;

     /* case VIDEO_WITH_SUBTITLE:
        mediaSource = buildMergingMediaSource(Uri.parse(videoUri), Uri.parse(subtitleUri));
        break;
*/
            case LOOPING_VIDEO:
                MediaSource mSource = buildMediaSource(Uri.parse(videoUri));
                mediaSource = new LoopingMediaSource(mSource);
                break;

            case SEQUENTIAL_VIDEO:
                if (null != videoUriList && videoUriList.length > 1) {
                    // Plays the first video, then the second video.
                    mediaSource = new ConcatenatingMediaSource(buildMediaSource(Uri.parse(videoUriList[0])), buildMediaSource(Uri.parse(videoUriList[1])));
                }
                break;

            default:
                break;
        }

        return mediaSource;
    }

    private MediaSource buildMediaSource(Uri uri) {
        // Measures bandwidth during playback. Can be null if not required.
        DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "ExoVideoPlayer"), bandwidthMeter);
        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
        // This is the MediaSource representing the media to be played.
        return new ExtractorMediaSource(uri, dataSourceFactory, extractorsFactory, null, null);
    }

/*  private MediaSource buildMergingMediaSource(Uri videoUri, Uri subtitleUri) {
    // Measures bandwidth during playback. Can be null if not required.
    DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
    // Produces DataSource instances through which media data is loaded.
    DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "ExoPlayerDemo"), bandwidthMeter);
    // Produces Extractor instances for parsing the media data.
    ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();
    MediaSource videoSource = new ExtractorMediaSource(videoUri,dataSourceFactory, extractorsFactory, null, null);

    Format textFormat = Format.createTextSampleFormat(null, MimeTypes.APPLICATION_SUBRIP,
            null, Format.NO_VALUE, Format.NO_VALUE, "en", null);

    MediaSource subtitleSource = new SingleSampleMediaSource(subtitleUri, dataSourceFactory, textFormat, C.TIME_UNSET);
    // Plays the video with the sideloaded subtitle.
    return new MergingMediaSource(videoSource, subtitleSource);
  }*/


}
