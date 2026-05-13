package com.nigdroid.sangeet;

import java.io.Serializable;

public class Track implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long id;
    private final String title;
    private final String artist;
    private final String uri;
    private final long durationMs;
    private final long albumId;

    public Track(long id, String title, String artist, String uri, long durationMs, long albumId) {
        this.id = id;
        this.title = title != null ? title : "";
        this.artist = artist != null ? artist : "";
        this.uri = uri != null ? uri : "";
        this.durationMs = durationMs;
        this.albumId = albumId;
    }

    public long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public String getUri() {
        return uri;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public long getAlbumId() {
        return albumId;
    }
}
