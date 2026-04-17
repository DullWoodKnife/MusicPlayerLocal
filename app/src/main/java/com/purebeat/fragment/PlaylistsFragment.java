package com.purebeat.fragment;

import android.content.Intent;
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
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.purebeat.PureBeatApplication;
import com.purebeat.R;
import com.purebeat.activity.MainActivity;
import com.purebeat.activity.PlaylistDetailActivity;
import com.purebeat.adapter.PlaylistAdapter;
import com.purebeat.database.AppDatabase;
import com.purebeat.database.PlaylistEntity;
import com.purebeat.model.Playlist;
import com.purebeat.service.MusicController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaylistsFragment extends Fragment implements PlaylistAdapter.OnPlaylistClickListener {

    private RecyclerView recyclerView;
    private PlaylistAdapter adapter;
    private TextView tvEmpty;
    private FloatingActionButton fabAdd;
    private List<Playlist> playlists = new ArrayList<>();
    private MusicController musicController;
    private ExecutorService executor;
    private Handler mainHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlists, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view);
        tvEmpty = view.findViewById(R.id.tv_empty);
        fabAdd = view.findViewById(R.id.fab_add);

        adapter = new PlaylistAdapter();
        adapter.setOnPlaylistClickListener(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        PureBeatApplication app = (PureBeatApplication) requireActivity().getApplication();
        musicController = app.getMusicController();

        fabAdd.setOnClickListener(v -> showCreatePlaylistDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        loadPlaylists();
    }

    private void loadPlaylists() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            List<PlaylistEntity> entities = db.playlistDao().getAllPlaylists();

            List<Playlist> loadedPlaylists = new ArrayList<>();
            for (PlaylistEntity entity : entities) {
                int songCount = db.playlistSongDao().getSongCountForPlaylist(entity.getId());
                loadedPlaylists.add(new Playlist(entity.getId(), entity.getName(),
                    entity.getCreatedAt(), songCount));
            }

            mainHandler.post(() -> {
                playlists = loadedPlaylists;
                adapter.setPlaylists(playlists);

                if (playlists.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    private void showCreatePlaylistDialog() {
        View dialogView = LayoutInflater.from(getContext())
            .inflate(R.layout.dialog_create_playlist, null);
        TextInputEditText etName = dialogView.findViewById(R.id.et_playlist_name);

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.create_playlist)
            .setView(dialogView)
            .setPositiveButton(R.string.confirm, (dialog, which) -> {
                String name = etName.getText() != null ? etName.getText().toString().trim() : "";
                if (!name.isEmpty()) {
                    createPlaylist(name);
                }
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void createPlaylist(String name) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            PlaylistEntity entity = new PlaylistEntity(name);
            db.playlistDao().insertPlaylist(entity);

            mainHandler.post(() -> {
                Toast.makeText(getContext(), R.string.playlist_created, Toast.LENGTH_SHORT).show();
                loadPlaylists();
            });
        });
    }

    @Override
    public void onPlaylistClick(Playlist playlist) {
        Intent intent = new Intent(getContext(), PlaylistDetailActivity.class);
        intent.putExtra("playlist_id", playlist.getId());
        intent.putExtra("playlist_name", playlist.getName());
        startActivity(intent);
    }

    @Override
    public void onPlaylistDeleteClick(Playlist playlist) {
        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.delete_playlist)
            .setMessage(R.string.confirm_delete_playlist)
            .setPositiveButton(R.string.delete, (dialog, which) -> {
                deletePlaylist(playlist);
            })
            .setNegativeButton(R.string.cancel, null)
            .show();
    }

    private void deletePlaylist(Playlist playlist) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            db.playlistDao().deletePlaylistById(playlist.getId());

            mainHandler.post(() -> {
                Toast.makeText(getContext(), R.string.playlist_deleted, Toast.LENGTH_SHORT).show();
                loadPlaylists();
            });
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }
}
