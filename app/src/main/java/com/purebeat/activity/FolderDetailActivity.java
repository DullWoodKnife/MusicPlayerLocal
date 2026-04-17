package com.purebeat.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.purebeat.PureBeatApplication;
import com.purebeat.R;
import com.purebeat.adapter.SongAdapter;
import com.purebeat.model.Folder;
import com.purebeat.model.Song;
import com.purebeat.service.MusicController;
import com.purebeat.util.MusicScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FolderDetailActivity extends AppCompatActivity implements SongAdapter.OnSongClickListener {

    private TextView tvFolderName;
    private RecyclerView recyclerView;
    private ImageButton btnBack;

    private String folderPath;
    private String folderName;
    private List<Song> songs = new ArrayList<>();
    private MusicController musicController;
    private ExecutorService executor;
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_folder_detail);

        folderPath = getIntent().getStringExtra("folder_path");
        folderName = getIntent().getStringExtra("folder_name");

        initViews();
        initMusicController();
        loadSongs();
    }

    private void initViews() {
        tvFolderName = findViewById(R.id.tv_folder_name);
        recyclerView = findViewById(R.id.recycler_view);
        btnBack = findViewById(R.id.btn_back);

        tvFolderName.setText(folderName);

        SongAdapter adapter = new SongAdapter();
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
            PureBeatApplication app = (PureBeatApplication) getApplication();
            MusicScanner scanner = app.getMusicScanner();
            List<Song> allSongs = scanner.scanAllSongs();
            List<Folder> folders = scanner.getFolders(allSongs);

            List<Song> folderSongs = new ArrayList<>();
            for (Folder folder : folders) {
                if (folder.getPath().equals(folderPath)) {
                    folderSongs = folder.getSongs();
                    break;
                }
            }

            final List<Song> finalSongs = folderSongs;
            mainHandler.post(() -> {
                songs = finalSongs;
                SongAdapter adapter = (SongAdapter) recyclerView.getAdapter();
                if (adapter != null) {
                    adapter.setSongs(songs);
                }
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
        // No action for folder songs long click
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
