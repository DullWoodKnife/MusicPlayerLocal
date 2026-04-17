package com.purebeat.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.purebeat.PureBeatApplication;
import com.purebeat.R;
import com.purebeat.activity.FolderDetailActivity;
import com.purebeat.adapter.FolderAdapter;
import com.purebeat.model.Folder;
import com.purebeat.model.Song;
import com.purebeat.service.MusicController;
import com.purebeat.util.MusicScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FoldersFragment extends Fragment implements FolderAdapter.OnFolderClickListener {

    private RecyclerView recyclerView;
    private FolderAdapter adapter;
    private TextView tvEmpty;
    private List<Folder> folders = new ArrayList<>();
    private MusicController musicController;
    private ExecutorService executor;
    private Handler mainHandler;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_folders, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recyclerView = view.findViewById(R.id.recycler_view);
        tvEmpty = view.findViewById(R.id.tv_empty);

        adapter = new FolderAdapter();
        adapter.setOnFolderClickListener(this);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        PureBeatApplication app = (PureBeatApplication) requireActivity().getApplication();
        musicController = app.getMusicController();

        loadFolders();
    }

    private void loadFolders() {
        executor.execute(() -> {
            PureBeatApplication app = (PureBeatApplication) requireActivity().getApplication();
            MusicScanner scanner = app.getMusicScanner();
            List<Song> songs = scanner.scanAllSongs();
            List<Folder> scannedFolders = scanner.getFolders(songs);

            mainHandler.post(() -> {
                folders = scannedFolders;
                adapter.setFolders(folders);

                if (folders.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    recyclerView.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                }
            });
        });
    }

    @Override
    public void onFolderClick(Folder folder) {
        Intent intent = new Intent(getContext(), FolderDetailActivity.class);
        intent.putExtra("folder_path", folder.getPath());
        intent.putExtra("folder_name", folder.getName());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }
}
