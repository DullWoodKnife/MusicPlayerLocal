package com.purebeat.activity;

import android.Manifest;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.purebeat.PureBeatApplication;
import com.purebeat.R;
import com.purebeat.database.AppDatabase;
import com.purebeat.database.BackgroundImageEntity;
import com.purebeat.model.Song;
import com.purebeat.service.MusicController;
import com.purebeat.service.MusicPlaybackService;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity implements MusicController.MusicControllerCallback {

    private TabLayout tabLayout;
    private ViewPager2 viewPager;
    private View miniPlayer;
    private ImageView ivBackground;
    private TextView tvMiniTitle, tvMiniArtist;
    private ImageView ivMiniAlbumArt;
    private ImageButton btnMiniPlayPause, btnMiniNext;
    private ProgressBar progressBar;

    private MusicController musicController;
    private ExecutorService executor;
    private Handler mainHandler;
    private boolean isBound = false;

    // 使用 AtomicBoolean 避免 Runnable 内存泄漏
    private final AtomicBoolean isProgressUpdating = new AtomicBoolean(false);
    private Runnable progressUpdateRunnable;

    private static final int PERMISSION_REQUEST_CODE = 100;
    private static final String[] TAB_TITLES = {"歌曲", "文件夹", "歌单", "设置"};

    private final ServiceConnection serviceConnection = new ServiceConnection() {
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
        setContentView(R.layout.activity_main);

        initViews();
        initMusicController();
        checkPermissionAndLoadMusic();
        setupViewPager();
        setupMiniPlayer();
        loadBackgroundImage();
    }

    private void initViews() {
        tabLayout = findViewById(R.id.tab_layout);
        viewPager = findViewById(R.id.view_pager);
        miniPlayer = findViewById(R.id.mini_player);
        ivBackground = findViewById(R.id.iv_background);
        tvMiniTitle = findViewById(R.id.tv_mini_title);
        tvMiniArtist = findViewById(R.id.tv_mini_artist);
        ivMiniAlbumArt = findViewById(R.id.iv_mini_album_art);
        btnMiniPlayPause = findViewById(R.id.btn_mini_play_pause);
        btnMiniNext = findViewById(R.id.btn_mini_next);
        progressBar = findViewById(R.id.progress_bar);

        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void initMusicController() {
        PureBeatApplication app = (PureBeatApplication) getApplication();
        musicController = app.getMusicController();
        musicController.setCallback(this);

        Intent intent = new Intent(this, MusicPlaybackService.class);
        startService(intent);
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    private void checkPermissionAndLoadMusic() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_AUDIO},
                        PERMISSION_REQUEST_CODE);
            } else {
                loadMusicInBackground();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                loadMusicInBackground();
            }
        }
    }

    private void loadMusicInBackground() {
        executor.execute(() -> {
            PureBeatApplication app = (PureBeatApplication) getApplication();
            app.getMusicScanner().scanAllSongs();
            app.getMusicController().startService();
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadMusicInBackground();
            } else {
                Toast.makeText(this, R.string.permission_denied, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void setupViewPager() {
        viewPager.setAdapter(new ViewPagerAdapter(this));

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(TAB_TITLES[position]);
        }).attach();
    }

    private void setupMiniPlayer() {
        miniPlayer.setOnClickListener(v -> {
            Intent intent = new Intent(this, PlayerActivity.class);
            startActivity(intent);
        });

        btnMiniPlayPause.setOnClickListener(v -> {
            if (musicController.isPlaying()) {
                musicController.pause();
            } else {
                musicController.play();
            }
        });

        btnMiniNext.setOnClickListener(v -> musicController.playNext());

        startProgressUpdates();
    }

    private void startProgressUpdates() {
        isProgressUpdating.set(true);
        progressUpdateRunnable = new Runnable() {
            @Override
            public void run() {
                // 检查 Activity 是否正在运行
                if (!isProgressUpdating.get() || isFinishing() || isDestroyed()) {
                    return;
                }

                if (musicController != null && musicController.isPlaying()) {
                    long position = musicController.getCurrentPosition();
                    long duration = musicController.getDuration();
                    if (duration > 0) {
                        int progress = (int) (position * 100 / duration);
                        progressBar.setProgress(progress);
                    }
                }
                mainHandler.postDelayed(this, 1000);
            }
        };
        mainHandler.post(progressUpdateRunnable);
    }

    private void stopProgressUpdates() {
        isProgressUpdating.set(false);
        if (progressUpdateRunnable != null) {
            mainHandler.removeCallbacks(progressUpdateRunnable);
        }
    }

    private void loadBackgroundImage() {
        executor.execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            BackgroundImageEntity bg = db.backgroundImageDao().getActiveBackground();
            if (bg != null && bg.getImagePath() != null) {
                String imagePath = bg.getImagePath();
                mainHandler.post(() -> {
                    // 检查 Activity 状态
                    if (isFinishing() || isDestroyed()) {
                        return;
                    }
                    ivBackground.setVisibility(View.VISIBLE);
                    Glide.with(this)
                        .load(imagePath)
                        .into(ivBackground);
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isProgressUpdating.get()) {
            startProgressUpdates();
        }
        updateMiniPlayer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 暂停不必要的更新以节省资源
        stopProgressUpdates();
    }

    private void updateMiniPlayer() {
        // 检查 Activity 状态
        if (isFinishing() || isDestroyed()) {
            return;
        }

        Song currentSong = musicController.getCurrentSong();
        if (currentSong != null) {
            miniPlayer.setVisibility(View.VISIBLE);
            tvMiniTitle.setText(currentSong.getTitle());
            tvMiniArtist.setText(currentSong.getArtist());

            Glide.with(this)
                .load(currentSong.getAlbumArtUri())
                .placeholder(R.drawable.ic_music_note)
                .error(R.drawable.ic_music_note)
                .centerCrop()
                .into(ivMiniAlbumArt);

            btnMiniPlayPause.setImageResource(
                musicController.isPlaying() ? R.drawable.ic_pause : R.drawable.ic_play
            );
        } else {
            miniPlayer.setVisibility(View.GONE);
        }
    }

    @Override
    public void onPlaybackStateChanged(Song currentSong, boolean isPlaying, long position, long duration) {
        // 使用 runOnUiThread 确保在主线程执行，且检查 Activity 状态
        runOnUiThread(() -> {
            if (!isFinishing() && !isDestroyed()) {
                updateMiniPlayer();
            }
        });
    }

    @Override
    public void onPlaylistChanged(List<Song> playlist) {}

    @Override
    public void onPlaybackModeChanged(com.purebeat.model.PlaybackMode mode) {}

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopProgressUpdates();

        // 清理 MusicController 回调
        if (musicController != null) {
            musicController.setCallback(null);
        }

        if (isBound) {
            try {
                unbindService(serviceConnection);
            } catch (IllegalArgumentException e) {
                // Service 未注册，忽略
            }
            isBound = false;
        }

        // 正确关闭 ExecutorService
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
    }

    public MusicController getMusicController() {
        return musicController;
    }

    private static class ViewPagerAdapter extends FragmentStateAdapter {

        public ViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            switch (position) {
                case 0:
                    return new com.purebeat.fragment.SongsFragment();
                case 1:
                    return new com.purebeat.fragment.FoldersFragment();
                case 2:
                    return new com.purebeat.fragment.PlaylistsFragment();
                case 3:
                    return new com.purebeat.fragment.SettingsFragment();
                default:
                    return new com.purebeat.fragment.SongsFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 4;
        }
    }
}
