package com.purebeat.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.purebeat.database.BackgroundImageEntity;

import java.util.List;

@Dao
public interface BackgroundImageDao {

    @Query("SELECT * FROM background_images ORDER BY createdAt DESC")
    List<BackgroundImageEntity> getAllBackgroundImages();

    @Query("SELECT * FROM background_images WHERE isActive = 1 LIMIT 1")
    BackgroundImageEntity getActiveBackground();

    @Query("SELECT * FROM background_images WHERE id = :id")
    BackgroundImageEntity getBackgroundById(long id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertBackground(BackgroundImageEntity backgroundImage);

    @Update
    void updateBackground(BackgroundImageEntity backgroundImage);

    @Delete
    void deleteBackground(BackgroundImageEntity backgroundImage);

    @Query("UPDATE background_images SET isActive = 0")
    void deactivateAllBackgrounds();

    @Query("UPDATE background_images SET isActive = 1 WHERE id = :id")
    void activateBackground(long id);

    @Query("UPDATE background_images SET isActive = 0 WHERE id = :id")
    void deactivateBackground(long id);
}
