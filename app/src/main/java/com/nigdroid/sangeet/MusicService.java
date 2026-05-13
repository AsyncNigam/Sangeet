package com.nigdroid.sangeet;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MusicService extends Service implements MediaPlayer.OnCompletionListener {

    public static final String ACTION_PLAY_QUEUE = "com.nigdroid.sangeet.action.PLAY_QUEUE";
    public static final String ACTION_TOGGLE = "com.nigdroid.sangeet.action.TOGGLE";
    public static final String ACTION_PAUSE = "com.nigdroid.sangeet.action.PAUSE";
    public static final String ACTION_RESUME = "com.nigdroid.sangeet.action.RESUME";
    public static final String ACTION_FORWARD = "com.nigdroid.sangeet.action.FORWARD";
    public static final String ACTION_NEXT = "com.nigdroid.sangeet.action.NEXT";
    public static final String ACTION_STOP_SERVICE = "com.nigdroid.sangeet.action.STOP_SERVICE";

    public static final String EXTRA_TRACKS = "extra_tracks";
    public static final String EXTRA_INDEX = "extra_index";

    private static final String CHANNEL_ID = "sangeet_playback";
    private static final int NOTIFICATION_ID = 42;

    private final IBinder binder = new LocalBinder();
    private final CopyOnWriteArrayList<PlaybackListener> listeners = new CopyOnWriteArrayList<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressTicker = new Runnable() {
        @Override
        public void run() {
            tickProgress();
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                mainHandler.postDelayed(this, 450L);
            }
        }
    };

    private MediaPlayer mediaPlayer;
    private MediaSessionCompat mediaSession;
    private final List<Track> queue = new ArrayList<>();
    private int queueIndex = 0;
    private boolean prepared = false;
    private boolean released;

    public class LocalBinder extends Binder {
        MusicService getService() {
            return MusicService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
        initSession();
    }

    private void initSession() {
        mediaSession = new MediaSessionCompat(this, "Sangeet");
        mediaSession.setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                        | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        );
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            @Override
            public void onPlay() {
                resumePlayback();
            }

            @Override
            public void onPause() {
                pausePlayback();
            }

            @Override
            public void onSkipToNext() {
                playNext();
            }

            @Override
            public void onSeekTo(long pos) {
                seekToMs((int) pos);
            }
        });
        mediaSession.setActive(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                    NOTIFICATION_ID,
                    buildNotification(null, false),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            );
        } else {
            startForeground(NOTIFICATION_ID, buildNotification(null, false));
        }

        if (intent != null && intent.getAction() != null) {
            handleAction(intent.getAction(), intent);
        }

        return START_NOT_STICKY;
    }

    private void handleAction(String action, Intent intent) {
        switch (action) {
            case ACTION_PLAY_QUEUE:
                handlePlayQueue(intent);
                break;
            case ACTION_TOGGLE:
                togglePlayback();
                break;
            case ACTION_PAUSE:
                pausePlayback();
                break;
            case ACTION_RESUME:
                resumePlayback();
                break;
            case ACTION_FORWARD:
                skipForwardMs(15_000);
                break;
            case ACTION_NEXT:
                playNext();
                break;
            case ACTION_STOP_SERVICE:
                stopAndRelease();
                break;
            default:
                break;
        }
    }

    @SuppressWarnings("deprecation")
    private void handlePlayQueue(Intent intent) {
        ArrayList<Track> incoming = (ArrayList<Track>) intent.getSerializableExtra(EXTRA_TRACKS);
        int index = intent.getIntExtra(EXTRA_INDEX, 0);
        if (incoming == null || incoming.isEmpty()) {
            return;
        }
        queue.clear();
        queue.addAll(incoming);
        queueIndex = Math.min(Math.max(index, 0), queue.size() - 1);
        openCurrentTrack();
    }

    private void openCurrentTrack() {
        prepared = false;
        releasePlayer();
        Track track = queue.get(queueIndex);
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
            );
            mediaPlayer.setDataSource(this, Uri.parse(track.getUri()));
            mediaPlayer.setOnPreparedListener(mp -> {
                prepared = true;
                mp.start();
                updateSessionState(PlaybackStateCompat.STATE_PLAYING);
                postNotification(track, true);
                notifyTrack(track);
                notifyPlaying(true);
                startTicker();
            });
            mediaPlayer.setOnCompletionListener(this);
            mediaPlayer.prepareAsync();
            postNotification(track, false);
        } catch (IOException e) {
            releasePlayer();
        }
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            try {
                mediaPlayer.reset();
            } catch (IllegalStateException ignored) {
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
        prepared = false;
    }

    private void stopAndRelease() {
        synchronized (this) {
            if (released) {
                return;
            }
            released = true;
        }
        mainHandler.removeCallbacks(progressTicker);
        releasePlayer();
        queue.clear();
        try {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } catch (Exception ignored) {
        }
        if (mediaSession != null) {
            mediaSession.setActive(false);
            mediaSession.release();
            mediaSession = null;
        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        synchronized (this) {
            if (!released) {
                released = true;
                mainHandler.removeCallbacks(progressTicker);
                releasePlayer();
                if (mediaSession != null) {
                    mediaSession.setActive(false);
                    mediaSession.release();
                    mediaSession = null;
                }
            }
        }
        super.onDestroy();
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        mainHandler.removeCallbacks(progressTicker);
        if (queueIndex < queue.size() - 1) {
            queueIndex++;
            openCurrentTrack();
        } else {
            notifyPlaybackEnded();
            pauseAtEnd();
        }
    }

    private void pauseAtEnd() {
        if (mediaPlayer != null) {
            mediaPlayer.seekTo(0);
        }
        updateSessionState(PlaybackStateCompat.STATE_STOPPED);
        Track t = currentTrack();
        postNotification(t, false);
        notifyPlaying(false);
        notifyProgress(0, mediaPlayer != null ? mediaPlayer.getDuration() : 0);
    }

    private void togglePlayback() {
        if (mediaPlayer == null) {
            return;
        }
        if (mediaPlayer.isPlaying()) {
            pausePlayback();
        } else {
            resumePlayback();
        }
    }

    public void pausePlayback() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            mainHandler.removeCallbacks(progressTicker);
            updateSessionState(PlaybackStateCompat.STATE_PAUSED);
            postNotification(currentTrack(), false);
            notifyPlaying(false);
        }
    }

    public void resumePlayback() {
        if (mediaPlayer != null && prepared && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            updateSessionState(PlaybackStateCompat.STATE_PLAYING);
            postNotification(currentTrack(), true);
            notifyPlaying(true);
            startTicker();
        }
    }

    public void seekToMs(int positionMs) {
        if (mediaPlayer != null && prepared) {
            mediaPlayer.seekTo(Math.max(0, Math.min(positionMs, mediaPlayer.getDuration())));
            tickProgress();
        }
    }

    public void skipForwardMs(int deltaMs) {
        if (mediaPlayer != null && prepared) {
            int next = mediaPlayer.getCurrentPosition() + deltaMs;
            seekToMs(next);
        }
    }

    public void playNext() {
        if (queueIndex < queue.size() - 1) {
            queueIndex++;
            openCurrentTrack();
        }
    }

    public Track currentTrack() {
        if (queue.isEmpty() || queueIndex < 0 || queueIndex >= queue.size()) {
            return null;
        }
        return queue.get(queueIndex);
    }

    public boolean isPlaying() {
        return mediaPlayer != null && mediaPlayer.isPlaying();
    }

    public int getDurationMs() {
        return mediaPlayer != null && prepared ? mediaPlayer.getDuration() : 0;
    }

    public int getCurrentPositionMs() {
        return mediaPlayer != null && prepared ? mediaPlayer.getCurrentPosition() : 0;
    }

    public void addListener(PlaybackListener listener) {
        listeners.addIfAbsent(listener);
    }

    public void removeListener(PlaybackListener listener) {
        listeners.remove(listener);
    }

    private void notifyTrack(Track track) {
        for (PlaybackListener l : listeners) {
            l.onTrackChanged(track);
        }
    }

    private void notifyPlaying(boolean playing) {
        for (PlaybackListener l : listeners) {
            l.onIsPlayingChanged(playing);
        }
    }

    private void notifyProgress(long pos, long dur) {
        for (PlaybackListener l : listeners) {
            l.onProgress(pos, dur);
        }
    }

    private void notifyPlaybackEnded() {
        for (PlaybackListener l : listeners) {
            l.onPlaybackEnded();
        }
    }

    private void startTicker() {
        mainHandler.removeCallbacks(progressTicker);
        mainHandler.post(progressTicker);
    }

    private void tickProgress() {
        if (mediaPlayer == null || !prepared) {
            return;
        }
        notifyProgress(mediaPlayer.getCurrentPosition(), mediaPlayer.getDuration());
    }

    private void updateSessionState(int state) {
        if (mediaSession == null) {
            return;
        }
        long position = mediaPlayer != null && prepared ? mediaPlayer.getCurrentPosition() : 0L;
        float playbackSpeed = (state == PlaybackStateCompat.STATE_PLAYING) ? 1f : 0f;
        PlaybackStateCompat playbackState = new PlaybackStateCompat.Builder()
                .setActions(
                        PlaybackStateCompat.ACTION_PLAY
                                | PlaybackStateCompat.ACTION_PAUSE
                                | PlaybackStateCompat.ACTION_SEEK_TO
                                | PlaybackStateCompat.ACTION_SKIP_TO_NEXT
                )
                .setState(state, position, playbackSpeed)
                .build();
        mediaSession.setPlaybackState(playbackState);
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription(getString(R.string.notification_channel_desc));
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }

    private PendingIntent servicePendingIntent(String action, int requestCode) {
        Intent i = new Intent(this, MusicService.class).setAction(action);
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        return PendingIntent.getService(this, requestCode, i, flags);
    }

    private Notification buildNotification(@Nullable Track track, boolean playing) {
        Intent openUi = new Intent(this, Song_player.class);
        openUi.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        int piFlags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            piFlags |= PendingIntent.FLAG_IMMUTABLE;
        }
        PendingIntent content = PendingIntent.getActivity(this, 0, openUi, piFlags);

        String title = track != null ? track.getTitle() : getString(R.string.app_name);
        String subtitle = track != null ? track.getArtist() : getString(R.string.notification_playing);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(subtitle)
                .setSmallIcon(R.drawable.baseline_music_note_24)
                .setContentIntent(content)
                .setOnlyAlertOnce(true)
                .setOngoing(playing)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        int playPauseIcon = playing ? R.drawable.ic_pause_24 : R.drawable.ic_play_24;
        String playPauseTitle = playing ? getString(R.string.action_pause) : getString(R.string.action_play);
        String playPauseAction = playing ? ACTION_PAUSE : ACTION_RESUME;
        builder.addAction(playPauseIcon, playPauseTitle, servicePendingIntent(playPauseAction, 1));
        builder.addAction(R.drawable.ic_forward_24, getString(R.string.action_forward), servicePendingIntent(ACTION_FORWARD, 2));
        builder.addAction(R.drawable.ic_skip_next_24, getString(R.string.action_next), servicePendingIntent(ACTION_NEXT, 3));

        if (mediaSession != null) {
            builder.setStyle(
                    new androidx.media.app.NotificationCompat.MediaStyle()
                            .setMediaSession(mediaSession.getSessionToken())
                            .setShowActionsInCompactView(0, 1, 2)
            );
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE);
        }

        return builder.build();
    }

    private void postNotification(@Nullable Track track, boolean playing) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID, buildNotification(track, playing));
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        stopAndRelease();
        super.onTaskRemoved(rootIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }
}
