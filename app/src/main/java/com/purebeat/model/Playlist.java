package com.purebeat.model;

public class Playlist {
    private long id;
    private String name;
    private long createdAt;
    private int songCount;
    private String coverImagePath;

    public Playlist() {
        this.createdAt = System.currentTimeMillis();
    }

    public Playlist(String name) {
        this.name = name;
        this.createdAt = System.currentTimeMillis();
        this.songCount = 0;
    }

    public Playlist(long id, String name, long createdAt, int songCount) {
        this.id = id;
        this.name = name;
        this.createdAt = createdAt;
        this.songCount = songCount;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int getSongCount() { return songCount; }
    public void setSongCount(int songCount) { this.songCount = songCount; }

    public String getCoverImagePath() { return coverImagePath; }
    public void setCoverImagePath(String coverImagePath) { this.coverImagePath = coverImagePath; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Playlist playlist = (Playlist) o;
        return id == playlist.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
