package com.purebeat;

import android.app.Application;

import com.purebeat.database.AppDatabase;
import com.purebeat.service.MusicController;
import com.purebeat.util.MusicScanner;

public class PureBeatApplication extends Application {

    private static PureBeatApplication instance;
    private AppDatabase database;
    private MusicController musicController;
    private MusicScanner musicScanner;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        // Initialize database
        database = AppDatabase.getInstance(this);

        // Initialize music scanner
        musicScanner = new MusicScanner(getContentResolver());

        // Initialize music controller
        musicController = new MusicController(this);
    }

    public static PureBeatApplication getInstance() {
        return instance;
    }

    public AppDatabase getDatabase() {
        return database;
    }

    public MusicController getMusicController() {
        return musicController;
    }

    public MusicScanner getMusicScanner() {
        return musicScanner;
    }
}
