package com.purebeat.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.purebeat.PureBeatApplication;
import com.purebeat.R;
import com.purebeat.adapter.SongAdapter;
import com.purebeat.database.AppDatabase;
import com.purebeat.database.PlaylistEntity;
import com.purebeat.model.Playlist;
import com.purebeat.model.Song;
import com.purebeat.service.MusicController;
import com.purebeat.util.MusicScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class SongsFragment extends Fragment implements SongAdapter.OnSongClickListener {

    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private TextView tvEmpty;
    private List<Song> songs = new ArrayList<>();
    private MusicController musicController;
    private ExecutorService executor;
    private Handler mainHandler;

    // 使用 AtomicBoolean 控制更新循环
    private final AtomicBoolean isUpdating = new AtomicBoolean(false);
    private Runnable updatePlayingRunnable;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_songs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view);
        tvEmpty = view.findViewById(R.id.tv_empty);

        adapter = new SongAdapter();
        adapter.setOnSongClickListener(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        PureBeatApplication app = (PureBeatApplication) requireActivity().getApplication();
        musicController = app.getMusicController();

        loadSongs();
    }

    @Override
    public void onResume() {
        super.onResume();
        // 只有在歌曲列表已加载后才开始更新
        if (!songs.isEmpty() && !isUpdating.get()) {
            startPlayingStateUpdates();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // Fragment 不可见时暂停更新
        stopPlayingStateUpdates();
    }

    private void loadSongs() {
        executor.execute(() -> {
            PureBeatApplication app = (PureBeatApplication) requireActivity().getApplication();
            MusicScanner scanner = app.getMusicScanner();
            List<Song> scannedSongs = scanner.scanAllSongs();

            mainHandler.post(() -> {
                // 检查 Fragment 状态
                if (!isAdded() || getActivity() == null) {
                    return;
                }

                songs = scannedSongs;
                adapter.setSongs(songs);

                if (songs.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }

                updatePlayingState();
                // 歌曲加载完成后开始播放状态更新
                startPlayingStateUpdates();
            });
        });
    }

    private void startPlayingStateUpdates() {
        if (!isUpdating.compareAndSet(false, true)) {
            return; // 已经在更新
        }

        updatePlayingRunnable = new Runnable() {
            @Override
            public void run() {
                // 检查 Fragment 状态
                if (!isUpdating.get() || !isAdded() || getActivity() == null || getActivity().isFinishing()) {
                    isUpdating.set(false);
                    return;
                }

                updatePlayingState();
                mainHandler.postDelayed(this, 500);
            }
        };
        mainHandler.post(updatePlayingRunnable);
    }

    private void stopPlayingStateUpdates() {
        isUpdating.set(false);
        if (updatePlayingRunnable != null) {
            mainHandler.removeCallbacks(updatePlayingRunnable);
        }
    }

    private void updatePlayingState() {
        // 额外的安全检查
        if (musicController == null || !isAdded() || getActivity() == null) {
            return;
        }

        Song currentSong = musicController.getCurrentSong();
        if (currentSong != null) {
            int index = -1;
            for (int i = 0; i < songs.size(); i++) {
                if (songs.get(i).getId() == currentSong.getId()) {
                    index = i;
                    break;
                }
            }
            adapter.setCurrentPlayingIndex(index);
        } else {
            adapter.setCurrentPlayingIndex(-1);
        }
    }

    @Override
    public void onSongClick(Song song, int position) {
        if (!isAdded() || getActivity() == null) {
            return;
        }
        musicController.setPlaylist(songs, position);
        musicController.play();
    }

    @Override
    public void onSongLongClick(Song song, int position) {
        if (!isAdded() || getActivity() == null) {
            return;
        }
        showAddToPlaylistDialog(song);
    }

    private void showAddToPlaylistDialog(Song song) {
        if (!isAdded() || getActivity() == null) {
            return;
        }

        final Song songToAdd = song; // 捕获 final 引用

        executor.execute(() -> {
            if (!isAdded() || getActivity() == null) {
                return;
            }

            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<PlaylistEntity> playlistEntities = db.playlistDao().getAllPlaylists();

            if (playlistEntities.isEmpty()) {
                mainHandler.post(() -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), R.string.no_playlists, Toast.LENGTH_SHORT).show();
                    }
                });
                return;
            }

            List<Playlist> playlists = new ArrayList<>();
            for (PlaylistEntity entity : playlistEntities) {
                int songCount = db.playlistSongDao().getSongCountForPlaylist(entity.getId());
                playlists.add(new Playlist(entity.getId(), entity.getName(),
                    entity.getCreatedAt(), songCount));
            }

            final List<Playlist> finalPlaylists = playlists;
            mainHandler.post(() -> {
                if (!isAdded() || getContext() == null) {
                    return;
                }

                String[] names = new String[finalPlaylists.size()];
                for (int i = 0; i < finalPlaylists.size(); i++) {
                    names[i] = finalPlaylists.get(i).getName();
                }

                new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.add_to_playlist)
                    .setItems(names, (dialog, which) -> {
                        Playlist selected = finalPlaylists.get(which);
                        addSongToPlaylist(songToAdd, selected.getId());
                    })
                    .setNegativeButton(R.string.cancel, null)
                    .show();
            });
        });
    }

    private void addSongToPlaylist(Song song, long playlistId) {
        if (!isAdded() || getActivity() == null) {
            return;
        }

        final Song songToAdd = song;

        executor.execute(() -> {
            if (!isAdded() || getActivity() == null) {
                return;
            }

            AppDatabase db = AppDatabase.getInstance(requireContext());
            Integer maxOrder = db.playlistSongDao().getMaxSortOrder(playlistId);
            int newOrder = (maxOrder != null ? maxOrder : -1) + 1;

            com.purebeat.database.PlaylistSongCrossRef ref =
                new com.purebeat.database.PlaylistSongCrossRef(playlistId, songToAdd.getId(), newOrder);
            db.playlistSongDao().insertPlaylistSong(ref);

            mainHandler.post(() -> {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), R.string.song_added, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 停止所有更新
        stopPlayingStateUpdates();

        // 清理 RecyclerView
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
        }

        // 关闭 ExecutorService
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }

        // 清空数据引用
        songs = new ArrayList<>();
        adapter = null;
    }
}
