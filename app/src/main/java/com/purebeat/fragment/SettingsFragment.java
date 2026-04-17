package com.purebeat.fragment;

import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.purebeat.PureBeatApplication;
import com.purebeat.R;
import com.purebeat.database.AppDatabase;
import com.purebeat.database.BackgroundImageEntity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 设置页 Fragment。
 *
 * 背景图功能设计（解决 recreate() 导致的播放器状态丢失问题）：
 * - 背景图路径持久化到 SharedPreferences（独立于 Activity 生命周期）
 * - MainActivity 在 onCreate 时从 SharedPreferences 读取并加载背景图
 * - SettingsFragment 通过 Fragment Result API 通知 MainActivity 刷新背景
 * - ActivityResultLauncher 使用静态注册块（Fragment#registerForActivityResult），
 *   避免在 Fragment attached 之前注册导致 IllegalStateException 崩溃
 */
public class SettingsFragment extends Fragment {

    private static final String PREF_NAME = "purebeat_settings";
    private static final String KEY_BG_IMAGE_PATH = "background_image_path";

    // Fragment Result API，用于跨 Fragment 通知 MainActivity 刷新背景图
    private static final String RESULT_KEY_BG_CHANGED = "bg_changed";
    private static final String RESULT_KEY_BG_REMOVED = "bg_removed";

    private TextView tvScanMusic, tvSelectBackground, tvRemoveBackground, tvAbout;
    private ExecutorService executor;
    private SharedPreferences pref;

    // 静态注册 ActivityResultLauncher（必须放在 Fragment 构造块或静态字段中，
    // 在 onCreateView 之前注册；放在 onViewCreated 中注册可能因 Fragment 状态
    // 不一致导致 IllegalStateException）
    private final ActivityResultLauncher<String> imagePickerLauncher =
        registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                // Fragment Result API：通知父 Activity（MainActivity）刷新背景
                if (uri != null) {
                    requireActivity().getSupportFragmentManager()
                        .setFragmentResult(RESULT_KEY_BG_CHANGED, new Bundle());
                    saveBackgroundImage(uri);
                }
            }
        );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        pref = requireContext().getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE);
        executor = Executors.newSingleThreadExecutor();

        tvScanMusic = view.findViewById(R.id.tv_scan_music);
        tvSelectBackground = view.findViewById(R.id.tv_select_background);
        tvRemoveBackground = view.findViewById(R.id.tv_remove_background);
        tvAbout = view.findViewById(R.id.tv_about);

        tvScanMusic.setOnClickListener(v -> scanMusic());
        tvSelectBackground.setOnClickListener(v -> selectBackgroundImage());
        tvRemoveBackground.setOnClickListener(v -> removeBackgroundImage());
        tvAbout.setOnClickListener(v -> showAboutDialog());
    }

    private void scanMusic() {
        if (!isAdded() || getContext() == null) return;

        Toast.makeText(getContext(), R.string.scanning, Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            if (!isAdded() || getActivity() == null) return;

            PureBeatApplication app = (PureBeatApplication) requireActivity().getApplication();
            app.getMusicScanner().scanAllSongs();

            if (isAdded() && getContext() != null) {
                requireActivity().runOnUiThread(() -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), R.string.scan_complete, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void selectBackgroundImage() {
        if (!isAdded()) return;
        imagePickerLauncher.launch("image/*");
    }

    private void saveBackgroundImage(Uri uri) {
        if (!isAdded() || getContext() == null) return;

        executor.execute(() -> {
            if (!isAdded() || getContext() == null) return;

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

                // 持久化到 SharedPreferences（独立于 Activity 生命周期）
                pref.edit().putString(KEY_BG_IMAGE_PATH, bgFile.getAbsolutePath()).apply();

                // 保存到数据库（用于多图管理）
                AppDatabase db = AppDatabase.getInstance(requireContext());
                db.backgroundImageDao().deactivateAllBackgrounds();
                BackgroundImageEntity entity = new BackgroundImageEntity(bgFile.getAbsolutePath());
                entity.setActive(true);
                db.backgroundImageDao().insertBackground(entity);

                requireActivity().runOnUiThread(() -> {
                    if (isAdded() && getContext() != null) {
                        Toast.makeText(getContext(), R.string.background_updated, Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                if (isAdded() && getContext() != null) {
                    requireActivity().runOnUiThread(() -> {
                        if (isAdded() && getContext() != null) {
                            Toast.makeText(getContext(), R.string.error, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });
    }

    private void removeBackgroundImage() {
        if (!isAdded() || getContext() == null) return;

        String savedPath = pref.getString(KEY_BG_IMAGE_PATH, null);
        if (savedPath == null) {
            Toast.makeText(getContext(), R.string.no_background_to_remove, Toast.LENGTH_SHORT).show();
            return;
        }

        executor.execute(() -> {
            if (!isAdded() || getContext() == null) return;

            // 删除文件
            File file = new File(savedPath);
            if (file.exists()) {
                file.delete();
            }

            // 清除 SharedPreferences
            pref.edit().remove(KEY_BG_IMAGE_PATH).apply();

            // 清除数据库记录
            AppDatabase db = AppDatabase.getInstance(requireContext());
            db.backgroundImageDao().deactivateAllBackgrounds();

            requireActivity().runOnUiThread(() -> {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), R.string.background_removed, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showAboutDialog() {
        if (!isAdded() || getContext() == null) return;

        new MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.about)
            .setMessage("PureBeat v1.0.0\n\n一款简洁的本地音乐播放器\n支持多种音频格式\n无需登录，保护隐私")
            .setPositiveButton(R.string.ok, null)
            .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }
}
