package com.purebeat.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "background_images")
public class BackgroundImageEntity {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String imagePath;
    private long createdAt;
    private boolean isActive;

    public BackgroundImageEntity() {
        this.createdAt = System.currentTimeMillis();
        this.isActive = false;
    }

    public BackgroundImageEntity(String imagePath) {
        this.imagePath = imagePath;
        this.createdAt = System.currentTimeMillis();
        this.isActive = false;
    }

    // Getters and Setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
