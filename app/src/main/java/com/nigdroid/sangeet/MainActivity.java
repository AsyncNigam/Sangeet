package com.nigdroid.sangeet;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.nigdroid.sangeet.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS = "sangeet_prefs";
    private static final String KEY_HIDE_AUDIO_PROMPT = "hide_audio_prompt";

    private ActivityMainBinding binding;
    private final List<Track> allTracks = new ArrayList<>();
    private final List<Track> displayedTracks = new ArrayList<>();
    private TrackAdapter adapter;
    private int pendingPlayIndex = -1;
    private SharedPreferences prefs;

    private final ActivityResultLauncher<String[]> audioPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onAudioPermissionResult);

    private final ActivityResultLauncher<String[]> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onNotificationPermissionResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new TrackAdapter(displayedTracks, this::onTrackSelected);
        binding.trackList.setLayoutManager(new LinearLayoutManager(this));
        binding.trackList.setAdapter(adapter);
        binding.trackList.setHasFixedSize(true);
        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(220);
        animator.setChangeDuration(180);
        animator.setMoveDuration(220);
        animator.setRemoveDuration(150);
        binding.trackList.setItemAnimator(animator);

        binding.btnGrantPermission.setOnClickListener(v -> requestAudioPermissionOnly());
        binding.btnDismissPermission.setOnClickListener(v -> {
            prefs.edit().putBoolean(KEY_HIDE_AUDIO_PROMPT, true).apply();
            updatePermissionCard();
            applyEmptyState();
        });

        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                applySearchFilter(s != null ? s.toString() : "");
            }
        });

        refreshLibraryUi();
    }

    private void onAudioPermissionResult(@NonNull Map<String, Boolean> result) {
        boolean granted = Boolean.TRUE.equals(result.getOrDefault(audioPermissionName(), false));
        updatePermissionCard();
        if (granted) {
            loadTracksFromDevice();
        }
        applyEmptyState();
    }

    private void onNotificationPermissionResult(@NonNull Map<String, Boolean> result) {
        if (pendingPlayIndex < 0) {
            return;
        }
        int index = pendingPlayIndex;
        pendingPlayIndex = -1;
        startPlayback(index);
    }

    private void requestAudioPermissionOnly() {
        String[] perms = audioPermissionOnly();
        if (perms.length == 0) {
            loadTracksFromDevice();
            return;
        }
        audioPermissionLauncher.launch(perms);
    }

    private String audioPermissionName() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Manifest.permission.READ_MEDIA_AUDIO;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Manifest.permission.READ_EXTERNAL_STORAGE;
        }
        return Manifest.permission.READ_MEDIA_AUDIO;
    }

    private String[] audioPermissionOnly() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{Manifest.permission.READ_MEDIA_AUDIO};
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }
        return new String[]{};
    }

    private boolean hasAudioReadPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return true;
        }
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void updatePermissionCard() {
        boolean dismissed = prefs.getBoolean(KEY_HIDE_AUDIO_PROMPT, false);
        boolean needAudio = !hasAudioReadPermission();
        boolean show = needAudio && !dismissed;
        binding.permissionCard.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void refreshLibraryUi() {
        updatePermissionCard();
        if (hasAudioReadPermission()) {
            loadTracksFromDevice();
        } else {
            allTracks.clear();
            applySearchFilter(binding.searchInput.getText() != null ? binding.searchInput.getText().toString() : "");
        }
        applyEmptyState();
    }

    private void loadTracksFromDevice() {
        allTracks.clear();
        allTracks.addAll(MusicLibrary.loadLocalTracks(this));
        applySearchFilter(binding.searchInput.getText() != null ? binding.searchInput.getText().toString() : "");
    }

    private void applySearchFilter(String query) {
        displayedTracks.clear();
        String q = query.trim().toLowerCase(Locale.US);
        if (q.isEmpty()) {
            displayedTracks.addAll(allTracks);
        } else {
            for (Track t : allTracks) {
                String title = t.getTitle().toLowerCase(Locale.US);
                String artist = t.getArtist().toLowerCase(Locale.US);
                if (title.contains(q) || artist.contains(q)) {
                    displayedTracks.add(t);
                }
            }
        }
        adapter.notifyDataSetChanged();
        applyEmptyState();
    }

    private void applyEmptyState() {
        boolean hasAudio = hasAudioReadPermission();
        boolean dismissed = prefs.getBoolean(KEY_HIDE_AUDIO_PROMPT, false);
        if (!hasAudio && dismissed) {
            binding.emptyState.setText(R.string.empty_no_permission);
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.trackList.setVisibility(View.GONE);
            return;
        }
        if (!hasAudio) {
            binding.emptyState.setVisibility(View.GONE);
            binding.trackList.setVisibility(binding.permissionCard.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            return;
        }
        if (displayedTracks.isEmpty()) {
            binding.emptyState.setText(allTracks.isEmpty() ? R.string.empty_library : R.string.empty_search);
            binding.emptyState.setVisibility(View.VISIBLE);
            binding.trackList.setVisibility(View.GONE);
        } else {
            binding.emptyState.setVisibility(View.GONE);
            binding.trackList.setVisibility(View.VISIBLE);
        }
    }

    private void onTrackSelected(Track track) {
        if (!hasAudioReadPermission()) {
            updatePermissionCard();
            return;
        }
        int index = allTracks.indexOf(track);
        if (index < 0) {
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
            pendingPlayIndex = index;
            notificationPermissionLauncher.launch(new String[]{Manifest.permission.POST_NOTIFICATIONS});
            return;
        }
        startPlayback(index);
    }

    private void startPlayback(int index) {
        Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_PLAY_QUEUE);
        serviceIntent.putExtra(MusicService.EXTRA_TRACKS, new ArrayList<>(allTracks));
        serviceIntent.putExtra(MusicService.EXTRA_INDEX, index);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(this, serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Intent player = new Intent(this, Song_player.class);
        player.putExtra(Song_player.EXTRA_START_INDEX, index);
        ActivityCompat.startActivity(this, player, null);
        overridePendingTransition(R.anim.slide_in_up, R.anim.fade_hold);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshLibraryUi();
    }
}
