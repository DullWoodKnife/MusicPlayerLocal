package com.purebeat.database;

import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "playlist_songs",
    primaryKeys = {"playlistId", "songMediaId"},
    foreignKeys = @ForeignKey(
        entity = PlaylistEntity.class,
        parentColumns = "id",
        childColumns = "playlistId",
        onDelete = ForeignKey.CASCADE
    ),
    indices = @Index("playlistId")
)
public class PlaylistSongCrossRef {
    private long playlistId;
    private long songMediaId;
    private int sortOrder;
    private long addedAt;

    public PlaylistSongCrossRef() {
        this.addedAt = System.currentTimeMillis();
    }

    public PlaylistSongCrossRef(long playlistId, long songMediaId, int sortOrder) {
        this.playlistId = playlistId;
        this.songMediaId = songMediaId;
        this.sortOrder = sortOrder;
        this.addedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public long getPlaylistId() { return playlistId; }
    public void setPlaylistId(long playlistId) { this.playlistId = playlistId; }

    public long getSongMediaId() { return songMediaId; }
    public void setSongMediaId(long songMediaId) { this.songMediaId = songMediaId; }

    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }

    public long getAddedAt() { return addedAt; }
    public void setAddedAt(long addedAt) { this.addedAt = addedAt; }
}
