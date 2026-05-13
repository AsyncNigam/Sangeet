package com.nigdroid.sangeet;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.widget.SeekBar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.nigdroid.sangeet.databinding.ActivitySongPlayerBinding;

import java.util.Locale;

public class Song_player extends AppCompatActivity implements PlaybackListener {

    public static final String EXTRA_START_INDEX = "extra_start_index";

    private ActivitySongPlayerBinding binding;
    private MusicService musicService;
    private boolean bound;
    private boolean userSeeking;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.LocalBinder binder = (MusicService.LocalBinder) service;
            musicService = binder.getService();
            bound = true;
            musicService.addListener(Song_player.this);
            syncUiFromService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicService = null;
            bound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySongPlayerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && musicService != null) {
                    binding.timeCurrent.setText(formatMs(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userSeeking = false;
                if (musicService != null) {
                    musicService.seekToMs(seekBar.getProgress());
                }
            }
        });

        binding.btnPlayPause.setOnClickListener(v -> togglePlayPause());
        binding.btnForward.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.skipForwardMs(15_000);
            }
        });
        binding.btnNext.setOnClickListener(v -> {
            if (musicService != null) {
                musicService.playNext();
            }
        });
    }

    private void togglePlayPause() {
        if (musicService == null) {
            return;
        }
        if (musicService.isPlaying()) {
            musicService.pausePlayback();
        } else {
            musicService.resumePlayback();
        }
    }

    private void syncUiFromService() {
        if (musicService == null) {
            return;
        }
        Track t = musicService.currentTrack();
        if (t != null) {
            applyTrackMetadata(t);
        }
        onIsPlayingChanged(musicService.isPlaying());
        int duration = Math.max(musicService.getDurationMs(), 1);
        binding.seekBar.setMax(duration);
        binding.seekBar.setProgress(musicService.getCurrentPositionMs());
        binding.timeTotal.setText(formatMs(duration));
        binding.timeCurrent.setText(formatMs(musicService.getCurrentPositionMs()));
    }

    private void applyTrackMetadata(Track track) {
        binding.trackTitle.setText(track.getTitle());
        String artist = track.getArtist();
        binding.trackArtist.setText(artist.isEmpty() ? getString(R.string.unknown_artist) : artist);
        Uri art = MusicLibrary.albumArtUri(track.getAlbumId());
        if (art != null) {
            binding.albumArt.setImageURI(art);
        } else {
            binding.albumArt.setImageResource(R.drawable.baseline_music_note_24);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Intent intent = new Intent(this, MusicService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        if (bound) {
            if (musicService != null) {
                musicService.removeListener(this);
            }
            unbindService(connection);
            bound = false;
        }
        super.onStop();
    }

    @Override
    public void onTrackChanged(Track track) {
        if (track == null) {
            return;
        }
        applyTrackMetadata(track);
    }

    @Override
    public void onIsPlayingChanged(boolean playing) {
        binding.btnPlayPause.setIconResource(playing ? R.drawable.ic_pause_24 : R.drawable.ic_play_24);
        binding.btnPlayPause.setContentDescription(
                getString(playing ? R.string.action_pause : R.string.action_play)
        );
    }

    @Override
    public void onProgress(long positionMs, long durationMs) {
        if (userSeeking) {
            return;
        }
        int max = Math.max((int) durationMs, 1);
        if (binding.seekBar.getMax() != max) {
            binding.seekBar.setMax(max);
        }
        binding.seekBar.setProgress((int) Math.min(positionMs, max));
        binding.timeCurrent.setText(formatMs(positionMs));
        binding.timeTotal.setText(formatMs(durationMs));
    }

    @Override
    public void onPlaybackEnded() {
        // Queue may auto-advance; UI updates arrive via onTrackChanged / onIsPlayingChanged.
    }

    private static String formatMs(long ms) {
        if (ms < 0) {
            ms = 0;
        }
        long totalSeconds = ms / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.US, "%d:%02d", minutes, seconds);
    }
}
