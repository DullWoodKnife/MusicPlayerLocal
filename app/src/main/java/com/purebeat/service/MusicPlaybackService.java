package com.purebeat.service;

import android.app.PendingIntent;
import android.content.Intent;

import androidx.annotation.Nullable;
import androidx.media3.common.AudioAttributes;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.session.MediaSession;
import androidx.media3.session.MediaSessionService;

import com.purebeat.activity.MainActivity;
import com.purebeat.model.PlaybackMode;
import com.purebeat.model.Song;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MusicPlaybackService extends MediaSessionService {

    private MediaSession mediaSession;
    private ExoPlayer player;
    private Player.Listener playerListener;

    // 使用线程安全的列表
    private List<Song> currentPlaylist = new CopyOnWriteArrayList<>();
    private List<Song> originalPlaylist = new CopyOnWriteArrayList<>();
    private volatile int currentIndex = -1;
    private volatile PlaybackMode playbackMode = PlaybackMode.SEQUENCE;

    @Override
    public void onCreate() {
        super.onCreate();

        player = new ExoPlayer.Builder(this)
            .setAudioAttributes(
                new AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .build();

        // 创建监听器引用以便后续移除
        playerListener = new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_ENDED) {
                    handleSongEnded();
                }
            }

            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                    currentIndex = player.getCurrentMediaItemIndex();
                }
            }
        };

        player.addListener(playerListener);

        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        mediaSession = new MediaSession.Builder(this, player)
            .setSessionActivity(pendingIntent)
            .build();
    }

    @Nullable
    @Override
    public MediaSession onGetSession(MediaSession.ControllerInfo controllerInfo) {
        return mediaSession;
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        if (player != null && !player.getPlayWhenReady()) {
            stopSelf();
        }
    }

    @Override
    public void onDestroy() {
        // 1. 首先释放 MediaSession
        if (mediaSession != null) {
            mediaSession.release();
            mediaSession = null;
        }

        // 2. 移除播放器监听器
        if (player != null && playerListener != null) {
            player.removeListener(playerListener);
            playerListener = null;
        }

        // 3. 释放播放器
        if (player != null) {
            player.release();
            player = null;
        }

        // 4. 清空播放列表
        currentPlaylist.clear();
        originalPlaylist.clear();

        super.onDestroy();
    }

    private void handleSongEnded() {
        PlaybackMode mode = playbackMode; // 读取副本
        switch (mode) {
            case REPEAT_ONE:
                if (player != null) {
                    player.seekTo(0);
                    player.play();
                }
                break;
            case SEQUENCE:
                if (currentIndex < currentPlaylist.size() - 1) {
                    playNextInternal();
                }
                break;
            case SHUFFLE:
                if (player != null && player.hasNextMediaItem()) {
                    player.seekToNext();
                    currentIndex = player.getCurrentMediaItemIndex();
                }
                break;
        }
    }

    private void playNextInternal() {
        if (player != null && currentIndex < currentPlaylist.size() - 1) {
            currentIndex++;
            player.seekTo(currentIndex, 0);
        }
    }

    // Public methods for external control
    public void setPlaylist(List<Song> songs, int startIndex) {
        originalPlaylist = new CopyOnWriteArrayList<>(songs);
        currentPlaylist = new CopyOnWriteArrayList<>(songs);
        currentIndex = startIndex;

        PlaybackMode mode = playbackMode;
        if (mode == PlaybackMode.SHUFFLE) {
            Collections.shuffle(currentPlaylist);
            // Move current song to first position
            if (startIndex >= 0 && startIndex < songs.size()) {
                Song currentSong = songs.get(startIndex);
                currentPlaylist.remove(currentSong);
                currentPlaylist.add(0, currentSong);
                currentIndex = 0;
            }
        }

        if (player != null) {
            List<MediaItem> mediaItems = new ArrayList<>();
            for (Song song : currentPlaylist) {
                MediaItem mediaItem = new MediaItem.Builder()
                    .setMediaId(String.valueOf(song.getId()))
                    .setUri(song.getUri())
                    .build();
                mediaItems.add(mediaItem);
            }

            player.setMediaItems(mediaItems, currentIndex, 0);
            player.prepare();
        }
    }

    public void play() {
        if (player != null) {
            player.play();
        }
    }

    public void pause() {
        if (player != null) {
            player.pause();
        }
    }

    public void playNext() {
        if (player == null) return;

        if (playbackMode == PlaybackMode.REPEAT_ONE) {
            player.seekTo(0);
            player.play();
            return;
        }

        if (player.hasNextMediaItem()) {
            player.seekToNext();
            currentIndex = player.getCurrentMediaItemIndex();
        } else if (playbackMode == PlaybackMode.SEQUENCE) {
            // Loop back to beginning
            player.seekTo(0, 0);
            currentIndex = 0;
        }
    }

    public void playPrevious() {
        if (player == null) return;

        if (player.getCurrentPosition() > 3000) {
            player.seekTo(0);
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPrevious();
            currentIndex = player.getCurrentMediaItemIndex();
        } else {
            player.seekTo(0);
            currentIndex = currentPlaylist.size() - 1;
        }
    }

    public void seekTo(long position) {
        if (player != null) {
            player.seekTo(position);
        }
    }

    public void setPlaybackMode(PlaybackMode mode) {
        playbackMode = mode;

        if (player == null) return;

        switch (mode) {
            case REPEAT_ONE:
                player.setRepeatMode(Player.REPEAT_MODE_ONE);
                player.setShuffleModeEnabled(false);
                break;
            case SHUFFLE:
                player.setRepeatMode(Player.REPEAT_MODE_OFF);
                player.setShuffleModeEnabled(true);
                // Reshuffle
                if (!originalPlaylist.isEmpty()) {
                    List<Song> shuffled = new ArrayList<>(originalPlaylist);
                    Collections.shuffle(shuffled);
                    currentPlaylist = new CopyOnWriteArrayList<>(shuffled);
                    currentIndex = player.getCurrentMediaItemIndex();
                }
                break;
            case SEQUENCE:
                player.setRepeatMode(Player.REPEAT_MODE_OFF);
                player.setShuffleModeEnabled(false);
                currentPlaylist = new CopyOnWriteArrayList<>(originalPlaylist);
                break;
        }
    }

    public long getCurrentPosition() {
        return player != null ? player.getCurrentPosition() : 0;
    }

    public long getDuration() {
        return player != null ? player.getDuration() : 0;
    }

    public boolean isPlaying() {
        return player != null && player.isPlaying();
    }

    public int getCurrentIndex() {
        return player != null ? player.getCurrentMediaItemIndex() : -1;
    }

    public Song getCurrentSong() {
        int index = currentIndex;
        if (index >= 0 && index < currentPlaylist.size()) {
            return currentPlaylist.get(index);
        }
        return null;
    }

    public List<Song> getCurrentPlaylist() {
        return new ArrayList<>(currentPlaylist);
    }

    public PlaybackMode getPlaybackMode() {
        return playbackMode;
    }

    public void addToPlaylist(Song song) {
        currentPlaylist.add(song);
        originalPlaylist.add(song);

        if (player != null) {
            MediaItem mediaItem = new MediaItem.Builder()
                .setMediaId(String.valueOf(song.getId()))
                .setUri(song.getUri())
                .build();
            player.addMediaItem(mediaItem);
        }
    }

    public void playSongAt(int index) {
        if (player != null && index >= 0 && index < currentPlaylist.size()) {
            player.seekTo(index, 0);
            player.play();
            currentIndex = index;
        }
    }
}
