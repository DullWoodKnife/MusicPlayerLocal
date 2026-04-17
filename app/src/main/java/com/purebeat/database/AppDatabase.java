package com.purebeat.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.purebeat.dao.BackgroundImageDao;
import com.purebeat.dao.PlaylistDao;
import com.purebeat.dao.PlaylistSongDao;

@Database(
    entities = {
        PlaylistEntity.class,
        PlaylistSongCrossRef.class,
        BackgroundImageEntity.class
    },
    version = 1,
    exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "purebeat_database";
    private static volatile AppDatabase INSTANCE;

    public abstract PlaylistDao playlistDao();
    public abstract PlaylistSongDao playlistSongDao();
    public abstract BackgroundImageDao backgroundImageDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    ).build();
                }
            }
        }
        return INSTANCE;
    }
}
