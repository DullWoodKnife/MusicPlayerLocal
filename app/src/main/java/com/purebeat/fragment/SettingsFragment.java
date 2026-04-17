package com.purebeat.fragment;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.purebeat.PureBeatApplication;
import com.purebeat.R;
import com.purebeat.database.AppDatabase;
import com.purebeat.database.BackgroundImageEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsFragment extends Fragment {

    private TextView tvScanMusic, tvSelectBackground, tvRemoveBackground, tvAbout;
    private ExecutorService executor;
    private Handler mainHandler;

    private ActivityResultLauncher<String> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    saveBackgroundImage(uri);
                }
            }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvScanMusic = view.findViewById(R.id.tv_scan_music);
        tvSelectBackground = view.findViewById(R.id.tv_select_background);
        tvRemoveBackground = view.findViewById(R.id.tv_remove_background);
        tvAbout = view.findViewById(R.id.tv_about);

        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        tvScanMusic.setOnClickListener(v -> scanMusic());
        tvSelectBackground.setOnClickListener(v -> selectBackgroundImage());
        tvRemoveBackground.setOnClickListener(v -> removeBackgroundImage());
        tvAbout.setOnClickListener(v -> showAboutDialog());
    }

    private void scanMusic() {
        Toast.makeText(getContext(), R.string.scanning, Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            PureBeatApplication app = (PureBeatApplication) requireActivity().getApplication();
            app.getMusicScanner().scanAllSongs();

            mainHandler.post(() -> {
                Toast.makeText(getContext(), R.string.scan_complete, Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void selectBackgroundImage() {
        imagePickerLauncher.launch("image/*");
    }

    private void saveBackgroundImage(Uri uri) {
        executor.execute(() -> {
            try {
                InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
                if (inputStream == null) return;

                File bgDir = new File(requireContext().getFilesDir(), "backgrounds");
                if (!bgDir.exists()) {
                    bgDir.mkdirs();
                }

                File bgFile = new File(bgDir, "custom_background.jpg");
                FileOutputStream outputStream = new FileOutputStream(bgFile);

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                inputStream.close();
                outputStream.close();

                // Save to database
                AppDatabase db = AppDatabase.getInstance(requireContext());
                db.backgroundImageDao().deactivateAllBackgrounds();

                BackgroundImageEntity entity = new BackgroundImageEntity(bgFile.getAbsolutePath());
                entity.setActive(true);
                db.backgroundImageDao().insertBackground(entity);

                mainHandler.post(() -> {
                    Toast.makeText(getContext(), R.string.background_set, Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    Toast.makeText(getContext(), R.string.error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void removeBackgroundImage() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(requireContext());
            BackgroundImageEntity bg = db.backgroundImageDao().getActiveBackground();

            if (bg != null) {
                // Delete file
                File file = new File(bg.getImagePath());
                if (file.exists()) {
                    file.delete();
                }

                // Remove from database
                db.backgroundImageDao().deleteBackground(bg);

                mainHandler.post(() -> {
                    Toast.makeText(getContext(), R.string.background_removed, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showAboutDialog() {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.about)
            .setMessage("PureBeat v1.0.0\n\n一款简洁的本地音乐播放器\n支持多种音频格式\n无需登录，保护隐私")
            .setPositiveButton(R.string.ok, null)
            .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        executor.shutdown();
    }
}
