package com.purebeat.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.purebeat.database.PlaylistSongCrossRef;

import java.util.List;

@Dao
public interface PlaylistSongDao {

    @Query("SELECT songMediaId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY sortOrder ASC")
    List<Long> getSongMediaIdsForPlaylist(long playlistId);

    @Query("SELECT * FROM playlist_songs WHERE playlistId = :playlistId ORDER BY sortOrder ASC")
    List<PlaylistSongCrossRef> getPlaylistSongRefs(long playlistId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertPlaylistSong(PlaylistSongCrossRef crossRef);

    @Delete
    void deletePlaylistSong(PlaylistSongCrossRef crossRef);

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId AND songMediaId = :songMediaId")
    void removeSongFromPlaylist(long playlistId, long songMediaId);

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    void deleteAllSongsFromPlaylist(long playlistId);

    @Query("SELECT COUNT(*) FROM playlist_songs WHERE playlistId = :playlistId")
    int getSongCountForPlaylist(long playlistId);

    @Query("SELECT MAX(sortOrder) FROM playlist_songs WHERE playlistId = :playlistId")
    Integer getMaxSortOrder(long playlistId);

    @Query("SELECT EXISTS(SELECT 1 FROM playlist_songs WHERE playlistId = :playlistId AND songMediaId = :songMediaId)")
    boolean isSongInPlaylist(long playlistId, long songMediaId);
}
