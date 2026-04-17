package com.purebeat.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.purebeat.PureBeatApplication;
import com.purebeat.R;
import com.purebeat.adapter.SongAdapter;
import com.purebeat.database.AppDatabase;
import com.purebeat.model.Song;
import com.purebeat.service.MusicController;
import com.purebeat.util.MusicScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PlaylistDetailActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener {

    private TextView tvPlaylistName;
    private RecyclerView recyclerView;
    private SongAdapter adapter;
    private ImageButton btnBack;

    private long playlistId;
    private String playlistName;
    private List<Song> songs = new ArrayList<>();
    private MusicController musicController;
    private ExecutorService executor;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        playlistId = getIntent().getLongExtra("playlist_id", -1);
        playlistName = getIntent().getStringExtra("playlist_name");

        initViews();
        initMusicController();
        loadSongs();
    }

    private void initViews() {
        tvPlaylistName = findViewById(R.id.tv_playlist_name);
        recyclerView = findViewById(R.id.recycler_view);
        btnBack = findViewById(R.id.btn_back);

        tvPlaylistName.setText(playlistName);

        adapter = new SongAdapter();
        adapter.setOnSongClickListener(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        btnBack.setOnClickListener(v -> finish());
    }

    private void initMusicController() {
        PureBeatApplication app = (PureBeatApplication) getApplication();
        musicController = app.getMusicController();
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void loadSongs() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            List<Long> songIds = db.playlistSongDao().getSongMediaIdsForPlaylist(playlistId);

            if (songIds.isEmpty()) {
                mainHandler.post(() -> {
                    songs = new ArrayList<>();
                    adapter.setSongs(songs);
                });
                return;
            }

            PureBeatApplication app = (PureBeatApplication) getApplication();
            MusicScanner scanner = app.getMusicScanner();
            List<Song> playlistSongs = scanner.getSongsByIds(songIds);

            mainHandler.post(() -> {
                songs = playlistSongs;
                adapter.setSongs(songs);
            });
        });
    }

    @Override
    public void onSongClick(Song song, int position) {
        musicController.setPlaylist(songs, position);
        musicController.play();
    }

    @Override
    public void onSongLongClick(Song song, int position) {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            db.playlistSongDao().removeSongFromPlaylist(playlistId, song.getId());

            mainHandler.post(() -> {
                Toast.makeText(this, R.string.song_removed, Toast.LENGTH_SHORT).show();
                loadSongs();
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
