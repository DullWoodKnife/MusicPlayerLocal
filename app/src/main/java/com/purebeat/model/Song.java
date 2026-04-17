package com.purebeat.model;

import android.net.Uri;

public class Song {
    private long id;
    private String title;
    private String artist;
    private String album;
    private long duration;
    private Uri uri;
    private Uri albumArtUri;
    private long dateAdded;
    private long size;
    private String mimeType;
    private String folderPath;
    private String folderName;

    public Song() {}

    public Song(long id, String title, String artist, String album, long duration,
                Uri uri, Uri albumArtUri, long dateAdded, long size, String mimeType,
                String folderPath, String folderName) {
        this.id = id;
        this.title = title;
        this.artist = artist;
        this.album = album;
        this.duration = duration;
        this.uri = uri;
        this.albumArtUri = albumArtUri;
        this.dateAdded = dateAdded;
        this.size = size;
        this.mimeType = mimeType;
        this.folderPath = folderPath;
        this.folderName = folderName;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getArtist() { return artist; }
    public void setArtist(String artist) { this.artist = artist; }

    public String getAlbum() { return album; }
    public void setAlbum(String album) { this.album = album; }

    public long getDuration() { return duration; }
    public void setDuration(long duration) { this.duration = duration; }

    public Uri getUri() { return uri; }
    public void setUri(Uri uri) { this.uri = uri; }

    public Uri getAlbumArtUri() { return albumArtUri; }
    public void setAlbumArtUri(Uri albumArtUri) { this.albumArtUri = albumArtUri; }

    public long getDateAdded() { return dateAdded; }
    public void setDateAdded(long dateAdded) { this.dateAdded = dateAdded; }

    public long getSize() { return size; }
    public void setSize(long size) { this.size = size; }

    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }

    public String getFolderPath() { return folderPath; }
    public void setFolderPath(String folderPath) { this.folderPath = folderPath; }

    public String getFolderName() { return folderName; }
    public void setFolderName(String folderName) { this.folderName = folderName; }

    public String getDurationFormatted() {
        long minutes = (duration / 1000) / 60;
        long seconds = (duration / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Song song = (Song) o;
        return id == song.id;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(id);
    }
}
