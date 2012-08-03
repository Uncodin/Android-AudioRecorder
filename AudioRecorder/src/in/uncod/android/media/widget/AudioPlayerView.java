package in.uncod.android.media.widget;

import java.io.IOException;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.SeekBar;

public class AudioPlayerView extends LinearLayout implements MediaPlayer.OnPreparedListener,
        View.OnClickListener, MediaPlayer.OnCompletionListener, MediaPlayer.OnErrorListener {

    private MediaPlayer mMediaPlayer;
    private ImageButton mPlayPauseButton;
    private SeekBar mSeekBar;
    private String mMediaLocation;
    private PlayerState mPlayerState;
    Thread playbackProgressUpdater;

    Handler mHandler = new Handler();

    enum PlayerState {
        Playing, Paused, Preparing
    }

    public AudioPlayerView(Context context) {
        super(context);

        initPlayer();
    }

    public AudioPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);

        initPlayer();
    }

    public AudioPlayerView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        initPlayer();
    }

    private void initPlayer() {
        mPlayPauseButton = new ImageButton(getContext());
        mPlayPauseButton.setOnClickListener(this);
        addView(mPlayPauseButton);

        updateButtonState(PlayerState.Preparing);

        mSeekBar = new SeekBar(getContext());
        mSeekBar.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(mSeekBar);

        setGravity(Gravity.CENTER_VERTICAL);
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        updateButtonState(PlayerState.Paused);

        playbackProgressUpdater = new Thread(new ProgressUpdate());
        playbackProgressUpdater.start();
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        updateButtonState(PlayerState.Paused);
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onClick(View view) {
        switch (mPlayerState) {
        case Paused:

            mMediaPlayer.start();
            updateButtonState(PlayerState.Playing);

            break;

        case Playing:

            mMediaPlayer.pause();
            updateButtonState(PlayerState.Paused);

            break;
        }
    }

    public void setMediaLocation(String location) {

        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setOnCompletionListener(this);
        mMediaPlayer.setOnErrorListener(this);
        mMediaLocation = location;

        prepareMediaPlayer();
    }

    private void prepareMediaPlayer() {
        updateButtonState(PlayerState.Preparing);

        if (mMediaLocation != null) {
            try {
                mMediaPlayer.setDataSource(mMediaLocation);
                mMediaPlayer.prepareAsync();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void updateButtonState(PlayerState playerState) {

        mPlayerState = playerState;

        switch (playerState) {
        case Paused:

            mPlayPauseButton.setImageResource(android.R.drawable.ic_media_play);
            mPlayPauseButton.setEnabled(true);

            break;
        case Playing:

            mPlayPauseButton.setImageResource(android.R.drawable.ic_media_pause);
            mPlayPauseButton.setEnabled(true);

            break;

        case Preparing:

            mPlayPauseButton.setImageResource(android.R.drawable.ic_media_play);
            mPlayPauseButton.setEnabled(false);

            break;
        }
    }

    private class ProgressUpdate implements Runnable {
        public void run() {
            while (mMediaPlayer != null) {
                double percent = (double) mMediaPlayer.getCurrentPosition() / mMediaPlayer.getDuration();
                final int progress = (int) (percent * 100.0);
                mHandler.post(new Runnable() {
                    public void run() {
                        mSeekBar.setProgress(progress);
                        mSeekBar.setMax(100);
                    }
                });
                try {
                    Thread.sleep(500);
                }
                catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);

        mPlayPauseButton.setEnabled(enabled);
        mSeekBar.setProgress(0);

        try {
            if (mMediaPlayer != null) {
                mMediaPlayer.seekTo(0);
            }
        }
        finally {
            // Ignore exception; should only happen when no media has been loaded into the player
        }
    }
}
