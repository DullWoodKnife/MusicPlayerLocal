package com.purebeat.model;

import java.util.ArrayList;
import java.util.List;

public class Folder {
    private String path;
    private String name;
    private int songCount;
    private List<Song> songs;

    public Folder() {
        this.songs = new ArrayList<>();
    }

    public Folder(String path, String name) {
        this.path = path;
        this.name = name;
        this.songs = new ArrayList<>();
        this.songCount = 0;
    }

    public void addSong(Song song) {
        if (songs == null) {
            songs = new ArrayList<>();
        }
        songs.add(song);
        songCount = songs.size();
    }

    // Getters and Setters
    public String getPath() { return path; }
    public void setPath(String path) { this.path = path; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getSongCount() { return songCount; }
    public void setSongCount(int songCount) { this.songCount = songCount; }

    public List<Song> getSongs() { return songs; }
    public void setSongs(List<Song> songs) {
        this.songs = songs;
        this.songCount = songs != null ? songs.size() : 0;
    }
}
