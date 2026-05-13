package com.nigdroid.sangeet;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;
import java.util.List;

public final class MusicLibrary {

    private MusicLibrary() {
    }

    public static List<Track> loadLocalTracks(Context context) {
        List<Track> tracks = new ArrayList<>();
        Uri collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String[] projection = new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
        };
        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";
        String sort = MediaStore.Audio.Media.TITLE + " COLLATE NOCASE ASC";

        try (Cursor cursor = context.getContentResolver().query(
                collection,
                projection,
                selection,
                null,
                sort
        )) {
            if (cursor == null) {
                return tracks;
            }
            int idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE);
            int artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST);
            int durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idCol);
                String title = cursor.getString(titleCol);
                String artist = cursor.getString(artistCol);
                long duration = durationCol >= 0 ? cursor.getLong(durationCol) : 0L;
                long albumId = albumIdCol >= 0 ? cursor.getLong(albumIdCol) : 0L;
                Uri trackUri = ContentUris.withAppendedId(collection, id);
                tracks.add(new Track(id, title, artist, trackUri.toString(), duration, albumId));
            }
        }
        return tracks;
    }

    public static Uri albumArtUri(long albumId) {
        if (albumId <= 0) {
            return null;
        }
        return ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId);
    }
}
