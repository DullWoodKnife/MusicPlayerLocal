package com.purebeat.service;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import androidx.media3.session.SessionToken;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.purebeat.model.PlaybackMode;
import com.purebeat.model.Song;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MusicController {

    private final Context context;
    private MediaController mediaController;
    private ListenableFuture<MediaController> controllerFuture;
    private MusicControllerCallback callback;

    // 使用弱引用避免内存泄漏
    private WeakReference<MusicControllerCallback> callbackWeakReference;

    // 线程安全的播放列表
    private List<Song> currentPlaylist = new CopyOnWriteArrayList<>();
    private PlaybackMode playbackMode = PlaybackMode.SEQUENCE;
    private Song currentSong;
    private volatile boolean isPlaying = false;
    private volatile long currentPosition = 0;
    private volatile long duration = 0;

    public interface MusicControllerCallback {
        void onPlaybackStateChanged(Song currentSong, boolean isPlaying, long position, long duration);
        void onPlaylistChanged(List<Song> playlist);
        void onPlaybackModeChanged(PlaybackMode mode);
    }

    public MusicController(Context context) {
        this.context = context.getApplicationContext();
    }

    public void setCallback(MusicControllerCallback callback) {
        this.callback = callback;
        // 同时保存弱引用
        if (callback != null) {
            callbackWeakReference = new WeakReference<>(callback);
        } else {
            callbackWeakReference = null;
        }
    }

    /**
     * 安全调用回调，检查引用是否有效
     */
    private void invokeCallback(CallbackInvoker invoker) {
        // 首先检查直接引用
        MusicControllerCallback cb = callback;
        if (cb != null) {
            try {
                invoker.invoke(cb);
                return;
            } catch (Exception e) {
                // 忽略异常
            }
        }

        // 然后检查弱引用
        WeakReference<MusicControllerCallback> ref = callbackWeakReference;
        if (ref != null) {
            cb = ref.get();
            if (cb != null) {
                try {
                    invoker.invoke(cb);
                } catch (Exception e) {
                    // 忽略异常
                }
            }
        }
    }

    private interface CallbackInvoker {
        void invoke(MusicControllerCallback callback);
    }

    public void connect() {
        if (controllerFuture != null) {
            return; // 已经在连接中
        }

        SessionToken sessionToken = new SessionToken(
            context,
            new ComponentName(context, MusicPlaybackService.class)
        );

        controllerFuture = new MediaController.Builder(context, sessionToken).buildAsync();
        controllerFuture.addListener(new Runnable() {
            @Override
            public void run() {
                try {
                    mediaController = controllerFuture.get();
                    setupPlayerListener();
                    updateState();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, MoreExecutors.directExecutor());
    }

    public void disconnect() {
        if (mediaController != null) {
            mediaController.removeListener(playerListener);
            MediaController.releaseFuture(controllerFuture);
            mediaController = null;
        }
        controllerFuture = null;
    }

    private void setupPlayerListener() {
        if (mediaController != null) {
            mediaController.addListener(playerListener);
        }
    }

    private final Player.Listener playerListener = new Player.Listener() {
        @Override
        public void onIsPlayingChanged(boolean playing) {
            isPlaying = playing;
            updateState();
        }

        @Override
        public void onPlaybackStateChanged(int playbackState) {
            updateState();
        }

        @Override
        public void onMediaItemTransition(MediaItem mediaItem, int reason) {
            updateCurrentSong();
            updateState();
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
            switch (repeatMode) {
                case Player.REPEAT_MODE_ONE:
                    playbackMode = PlaybackMode.REPEAT_ONE;
                    break;
                case Player.REPEAT_MODE_OFF:
                    playbackMode = PlaybackMode.SEQUENCE;
                    break;
            }
            invokeCallback(new CallbackInvoker() {
                @Override
                public void invoke(MusicControllerCallback callback) {
                    callback.onPlaybackModeChanged(playbackMode);
                }
            });
        }

        @Override
        public void onShuffleModeEnabledChanged(boolean shuffleModeEnabled) {
            if (shuffleModeEnabled) {
                playbackMode = PlaybackMode.SHUFFLE;
            }
            invokeCallback(new CallbackInvoker() {
                @Override
                public void invoke(MusicControllerCallback callback) {
                    callback.onPlaybackModeChanged(playbackMode);
                }
            });
        }
    };

    private void updateState() {
        if (mediaController != null) {
            currentPosition = mediaController.getCurrentPosition();
            duration = mediaController.getDuration();
            if (duration < 0) duration = 0;
        }

        final Song song = currentSong;
        final boolean playing = isPlaying;
        final long pos = currentPosition;
        final long dur = duration;

        invokeCallback(new CallbackInvoker() {
            @Override
            public void invoke(MusicControllerCallback callback) {
                callback.onPlaybackStateChanged(song, playing, pos, dur);
            }
        });
    }

    private void updateCurrentSong() {
        if (mediaController != null && currentPlaylist != null) {
            int index = mediaController.getCurrentMediaItemIndex();
            if (index >= 0 && index < currentPlaylist.size()) {
                currentSong = currentPlaylist.get(index);
            }
        }
    }

    // Public control methods
    public void setPlaylist(List<Song> songs, int startIndex) {
        this.currentPlaylist = new CopyOnWriteArrayList<>(songs);

        if (mediaController != null) {
            List<MediaItem> mediaItems = new ArrayList<>();
            for (Song song : songs) {
                MediaItem mediaItem = new MediaItem.Builder()
                    .setMediaId(String.valueOf(song.getId()))
                    .setUri(song.getUri())
                    .build();
                mediaItems.add(mediaItem);
            }

            mediaController.setMediaItems(mediaItems, startIndex, 0);
            mediaController.prepare();

            if (startIndex >= 0 && startIndex < songs.size()) {
                currentSong = songs.get(startIndex);
            }
        }

        // 同步通知 Service 更新内部播放列表状态，确保 Service 端 currentPlaylist
        // 和 currentIndex 与客户端保持一致，避免 getCurrentSong() 返回 null
        Intent intent = new Intent(context, MusicPlaybackService.class);
        intent.setAction("com.purebeat.ACTION_SET_PLAYLIST");
        intent.putExtra("start_index", startIndex);
        // 将歌曲列表通过 Parcelable 传递
        ArrayList<android.os.Parcelable> parcelableList = new ArrayList<>();
        for (Song song : songs) {
            parcelableList.add(song);
        }
        intent.putParcelableArrayListExtra("songs", parcelableList);
        context.startService(intent);

        final List<Song> playlist = this.currentPlaylist;
        invokeCallback(new CallbackInvoker() {
            @Override
            public void invoke(MusicControllerCallback callback) {
                callback.onPlaylistChanged(playlist);
            }
        });
    }

    public void play() {
        if (mediaController != null) {
            mediaController.play();
        }
    }

    public void pause() {
        if (mediaController != null) {
            mediaController.pause();
        }
    }

    public void playNext() {
        if (mediaController != null) {
            if (mediaController.hasNextMediaItem()) {
                mediaController.seekToNext();
            } else if (playbackMode == PlaybackMode.SEQUENCE) {
                mediaController.seekTo(0, 0);
            }
        }
    }

    public void playPrevious() {
        if (mediaController != null) {
            if (mediaController.getCurrentPosition() > 3000) {
                mediaController.seekTo(0);
            } else if (mediaController.hasPreviousMediaItem()) {
                mediaController.seekToPrevious();
            } else {
                mediaController.seekTo(0);
            }
        }
    }

    public void seekTo(long position) {
        if (mediaController != null) {
            mediaController.seekTo(position);
        }
    }

    public void setPlaybackMode(PlaybackMode mode) {
        playbackMode = mode;
        if (mediaController != null) {
            switch (mode) {
                case REPEAT_ONE:
                    mediaController.setRepeatMode(Player.REPEAT_MODE_ONE);
                    mediaController.setShuffleModeEnabled(false);
                    break;
                case SHUFFLE:
                    mediaController.setRepeatMode(Player.REPEAT_MODE_OFF);
                    mediaController.setShuffleModeEnabled(true);
                    break;
                case SEQUENCE:
                    mediaController.setRepeatMode(Player.REPEAT_MODE_OFF);
                    mediaController.setShuffleModeEnabled(false);
                    break;
            }
        }
        final PlaybackMode playbackModeFinal = mode;
        invokeCallback(new CallbackInvoker() {
            @Override
            public void invoke(MusicControllerCallback callback) {
                callback.onPlaybackModeChanged(playbackModeFinal);
            }
        });
    }

    public void cyclePlaybackMode() {
        PlaybackMode newMode = playbackMode.next();
        setPlaybackMode(newMode);
    }

    // Getters
    public boolean isPlaying() {
        return isPlaying;
    }

    public long getCurrentPosition() {
        if (mediaController != null) {
            return mediaController.getCurrentPosition();
        }
        return 0;
    }

    public long getDuration() {
        if (mediaController != null) {
            long dur = mediaController.getDuration();
            return dur > 0 ? dur : duration;
        }
        return duration;
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public List<Song> getCurrentPlaylist() {
        return new ArrayList<>(currentPlaylist);
    }

    public PlaybackMode getPlaybackMode() {
        return playbackMode;
    }

    public int getCurrentIndex() {
        if (mediaController != null) {
            return mediaController.getCurrentMediaItemIndex();
        }
        return -1;
    }

    public void playSongAt(int index) {
        if (mediaController != null && index >= 0 && index < currentPlaylist.size()) {
            mediaController.seekTo(index, 0);
            mediaController.play();
        }
    }

    public void startService() {
        Intent intent = new Intent(context, MusicPlaybackService.class);
        context.startService(intent);
    }
}
