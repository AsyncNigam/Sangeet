package com.nigdroid.sangeet;

public interface PlaybackListener {
    void onTrackChanged(Track track);

    void onIsPlayingChanged(boolean playing);

    void onProgress(long positionMs, long durationMs);

    void onPlaybackEnded();
}
