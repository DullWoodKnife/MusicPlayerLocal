package com.purebeat.activity;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.purebeat.PureBeatApplication;
import com.purebeat.R;
import com.purebeat.model.PlaybackMode;
import com.purebeat.model.Song;
import com.purebeat.service.MusicController;
import com.purebeat.service.MusicPlaybackService;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

public class PlayerActivity extends AppCompatActivity implements MusicController.MusicControllerCallback {

    private ImageView ivBackground, ivAlbumArt;
    private TextView tvSongTitle, tvArtistName, tvCurrentTime, tvDuration;
    private SeekBar seekBar;
    private ImageButton btnBack, btnPlayMode, btnPrevious, btnPlayPause, btnNext;

    private MusicController musicController;
    private Handler handler;
    private Runnable updateRunnable;
    private boolean isSeeking = false;
    private boolean isBound = false;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        initViews();
        initMusicController();
        setupListeners();
        startProgressUpdates();
    }

    private void initViews() {
        ivBackground = findViewById(R.id.iv_background);
        ivAlbumArt = findViewById(R.id.iv_album_art);
        tvSongTitle = findViewById(R.id.tv_song_title);
        tvArtistName = findViewById(R.id.tv_artist_name);
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvDuration = findViewById(R.id.tv_duration);
        seekBar = findViewById(R.id.seek_bar);
        btnBack = findViewById(R.id.btn_back);
        btnPlayMode = findViewById(R.id.btn_play_mode);
        btnPrevious = findViewById(R.id.btn_previous);
        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnNext = findViewById(R.id.btn_next);

        handler = new Handler(Looper.getMainLooper());
    }

    private void initMusicController() {
        PureBeatApplication app = (PureBeatApplication) getApplication();
        musicController = app.getMusicController();
        musicController.setCallback(this);

        Intent intent = new Intent(this, MusicPlaybackService.class);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());

        btnPlayMode.setOnClickListener(v -> {
            musicController.cyclePlaybackMode();
        });

        btnPrevious.setOnClickListener(v -> musicController.playPrevious());

        btnPlayPause.setOnClickListener(v -> {
            if (musicController.isPlaying()) {
                musicController.pause();
            } else {
                musicController.play();
            }
        });

        btnNext.setOnClickListener(v -> musicController.playNext());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    long duration = musicController.getDuration();
                    long newPosition = (duration * progress) / 100;
                    tvCurrentTime.setText(formatDuration(newPosition));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isSeeking = false;
                long duration = musicController.getDuration();
                long newPosition = (duration * seekBar.getProgress()) / 100;
                musicController.seekTo(newPosition);
            }
        });
    }

    private void startProgressUpdates() {
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (musicController != null && !isSeeking) {
                    long position = musicController.getCurrentPosition();
                    long duration = musicController.getDuration();

                    if (duration > 0) {
                        int progress = (int) (position * 100 / duration);
                        seekBar.setProgress(progress);
                        tvCurrentTime.setText(formatDuration(position));
                        tvDuration.setText(formatDuration(duration));
                    }
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.post(updateRunnable);
    }

    private String formatDuration(long millis) {
        long minutes = (millis / 1000) / 60;
        long seconds = (millis / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public void onPlaybackStateChanged(Song currentSong, boolean isPlaying, long position, long duration) {
        runOnUiThread(() -> {
            if (currentSong != null) {
                tvSongTitle.setText(currentSong.getTitle());
                tvArtistName.setText(currentSong.getArtist());

                Glide.with(this)
                    .load(currentSong.getAlbumArtUri())
                    .placeholder(R.drawable.ic_music_note)
                    .error(R.drawable.ic_music_note)
                    .centerCrop()
                    .into(ivAlbumArt);

                // Also set as background
                Glide.with(this)
                    .load(currentSong.getAlbumArtUri())
                    .centerCrop()
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            ivBackground.setImageDrawable(resource);
                            ivBackground.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {}
                    });
            }

            btnPlayPause.setImageResource(
                isPlaying ? R.drawable.ic_pause : R.drawable.ic_play
            );
        });
    }

    @Override
    public void onPlaylistChanged(java.util.List<Song> playlist) {}

    @Override
    public void onPlaybackModeChanged(PlaybackMode mode) {
        runOnUiThread(() -> {
            switch (mode) {
                case SEQUENCE:
                    btnPlayMode.setImageResource(R.drawable.ic_repeat);
                    break;
                case SHUFFLE:
                    btnPlayMode.setImageResource(R.drawable.ic_shuffle);
                    break;
                case REPEAT_ONE:
                    btnPlayMode.setImageResource(R.drawable.ic_repeat_one);
                    break;
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        onPlaybackStateChanged(
            musicController.getCurrentSong(),
            musicController.isPlaying(),
            musicController.getCurrentPosition(),
            musicController.getDuration()
        );
        onPlaybackModeChanged(musicController.getPlaybackMode());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateRunnable);
        if (isBound) {
            unbindService(serviceConnection);
            isBound = false;
        }
    }
}
