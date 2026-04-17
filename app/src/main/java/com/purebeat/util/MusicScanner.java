package com.purebeat.util;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.purebeat.model.Folder;
import com.purebeat.model.Song;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MusicScanner {

    private ContentResolver contentResolver;

    public MusicScanner(ContentResolver contentResolver) {
        this.contentResolver = contentResolver;
    }

    public List<Song> scanAllSongs() {
        List<Song> songs = new ArrayList<>();

        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATA
        };

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0 AND " +
                          MediaStore.Audio.Media.DURATION + " > 10000";

        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";

        Cursor cursor = contentResolver.query(
            collection,
            projection,
            selection,
            null,
            sortOrder
        );

        if (cursor != null) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            int mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE);
            int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                String title = cursor.getString(titleColumn);
                String artist = cursor.getString(artistColumn);
                String album = cursor.getString(albumColumn);
                long duration = cursor.getLong(durationColumn);
                long dateAdded = cursor.getLong(dateAddedColumn);
                long size = cursor.getLong(sizeColumn);
                String mimeType = cursor.getString(mimeTypeColumn);
                String data = cursor.getString(dataColumn);

                if (TextUtils.isEmpty(title)) {
                    title = "Unknown";
                }
                if (TextUtils.isEmpty(artist)) {
                    artist = "Unknown Artist";
                }
                if (TextUtils.isEmpty(album)) {
                    album = "Unknown Album";
                }
                if (TextUtils.isEmpty(mimeType)) {
                    mimeType = "audio/mpeg";
                }

                Uri contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);

                Uri albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), id);

                // Extract folder information
                String folderPath = "";
                String folderName = "Unknown Folder";
                if (!TextUtils.isEmpty(data)) {
                    File file = new File(data);
                    File parent = file.getParentFile();
                    if (parent != null) {
                        folderPath = parent.getAbsolutePath();
                        folderName = parent.getName();
                    }
                }

                Song song = new Song(
                    id, title, artist, album, duration,
                    contentUri, albumArtUri, dateAdded, size, mimeType,
                    folderPath, folderName
                );

                songs.add(song);
            }

            cursor.close();
        }

        return songs;
    }

    public Map<String, Folder> scanFolders(List<Song> songs) {
        Map<String, Folder> folderMap = new HashMap<>();

        for (Song song : songs) {
            String folderPath = song.getFolderPath();
            if (folderPath == null || folderPath.isEmpty()) {
                folderPath = "Unknown";
                song.setFolderPath(folderPath);
                song.setFolderName("Unknown Folder");
            }

            if (!folderMap.containsKey(folderPath)) {
                folderMap.put(folderPath, new Folder(folderPath, song.getFolderName()));
            }

            folderMap.get(folderPath).addSong(song);
        }

        return folderMap;
    }

    public List<Folder> getFolders(List<Song> songs) {
        Map<String, Folder> folderMap = scanFolders(songs);
        List<Folder> folders = new ArrayList<>(folderMap.values());

        // Sort folders by name
        folders.sort((f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

        return folders;
    }

    public List<Song> getSongsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        List<Song> songs = new ArrayList<>();
        StringBuilder selection = new StringBuilder();
        selection.append(MediaStore.Audio.Media._ID).append(" IN (");
        for (int i = 0; i < ids.size(); i++) {
            selection.append(ids.get(i));
            if (i < ids.size() - 1) {
                selection.append(",");
            }
        }
        selection.append(")");

        Uri collection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
        } else {
            collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        String[] projection = {
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATE_ADDED,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.DATA
        };

        Cursor cursor = contentResolver.query(
            collection,
            projection,
            selection.toString(),
            null,
            null
        );

        if (cursor != null) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int dateAddedColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            int mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE);
            int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                String title = cursor.getString(titleColumn);
                String artist = cursor.getString(artistColumn);
                String album = cursor.getString(albumColumn);
                long duration = cursor.getLong(durationColumn);
                long dateAdded = cursor.getLong(dateAddedColumn);
                long size = cursor.getLong(sizeColumn);
                String mimeType = cursor.getString(mimeTypeColumn);
                String data = cursor.getString(dataColumn);

                if (TextUtils.isEmpty(title)) title = "Unknown";
                if (TextUtils.isEmpty(artist)) artist = "Unknown Artist";
                if (TextUtils.isEmpty(album)) album = "Unknown Album";
                if (TextUtils.isEmpty(mimeType)) mimeType = "audio/mpeg";

                Uri contentUri = ContentUris.withAppendedId(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
                Uri albumArtUri = ContentUris.withAppendedId(
                    Uri.parse("content://media/external/audio/albumart"), id);

                String folderPath = "";
                String folderName = "Unknown Folder";
                if (!TextUtils.isEmpty(data)) {
                    File file = new File(data);
                    File parent = file.getParentFile();
                    if (parent != null) {
                        folderPath = parent.getAbsolutePath();
                        folderName = parent.getName();
                    }
                }

                Song song = new Song(
                    id, title, artist, album, duration,
                    contentUri, albumArtUri, dateAdded, size, mimeType,
                    folderPath, folderName
                );

                songs.add(song);
            }

            cursor.close();
        }

        return songs;
    }
}
