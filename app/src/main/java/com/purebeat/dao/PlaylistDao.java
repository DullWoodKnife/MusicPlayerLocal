package com.purebeat.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.purebeat.database.PlaylistEntity;

import java.util.List;

@Dao
public interface PlaylistDao {

    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    List<PlaylistEntity> getAllPlaylists();

    @Query("SELECT * FROM playlists WHERE id = :playlistId")
    PlaylistEntity getPlaylistById(long playlistId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPlaylist(PlaylistEntity playlist);

    @Update
    void updatePlaylist(PlaylistEntity playlist);

    @Delete
    void deletePlaylist(PlaylistEntity playlist);

    @Query("DELETE FROM playlists WHERE id = :playlistId")
    void deletePlaylistById(long playlistId);
}
