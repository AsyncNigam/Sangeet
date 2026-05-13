package com.nigdroid.sangeet;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.nigdroid.sangeet.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private final List<Track> tracks = new ArrayList<>();
    private TrackAdapter adapter;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), this::onPermissionResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        adapter = new TrackAdapter(tracks, this::onTrackSelected);
        binding.trackList.setLayoutManager(new LinearLayoutManager(this));
        binding.trackList.setAdapter(adapter);

        binding.btnGrantPermission.setOnClickListener(v -> requestAudioPermissions());

        if (hasAudioAccess()) {
            loadTracks();
            showPermissionCard(false);
        } else {
            showPermissionCard(true);
        }
    }

    private void onPermissionResult(@NonNull Map<String, Boolean> result) {
        boolean allGranted = true;
        for (Boolean granted : result.values()) {
            if (!Boolean.TRUE.equals(granted)) {
                allGranted = false;
                break;
            }
        }
        showPermissionCard(!allGranted);
        if (allGranted) {
            loadTracks();
        }
    }

    private boolean hasAudioAccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            boolean audio = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO)
                    == PackageManager.PERMISSION_GRANTED;
            boolean notif = ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
            return audio && notif;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestAudioPermissions() {
        permissionLauncher.launch(getRequiredPermissions());
    }

    private String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return new String[]{
                    Manifest.permission.READ_MEDIA_AUDIO,
                    Manifest.permission.POST_NOTIFICATIONS
            };
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }
        return new String[]{};
    }

    private void showPermissionCard(boolean visible) {
        binding.permissionCard.setVisibility(visible ? View.VISIBLE : View.GONE);
    }

    private void loadTracks() {
        tracks.clear();
        tracks.addAll(MusicLibrary.loadLocalTracks(this));
        adapter.notifyDataSetChanged();
        boolean empty = tracks.isEmpty();
        binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.trackList.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private void onTrackSelected(int position) {
        if (!hasAudioAccess()) {
            showPermissionCard(true);
            return;
        }
        if (position < 0 || position >= tracks.size()) {
            return;
        }

        Intent serviceIntent = new Intent(this, MusicService.class);
        serviceIntent.setAction(MusicService.ACTION_PLAY_QUEUE);
        serviceIntent.putExtra(MusicService.EXTRA_TRACKS, new ArrayList<>(tracks));
        serviceIntent.putExtra(MusicService.EXTRA_INDEX, position);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Intent player = new Intent(this, Song_player.class);
        player.putExtra(Song_player.EXTRA_START_INDEX, position);
        startActivity(player);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (hasAudioAccess()) {
            loadTracks();
            showPermissionCard(false);
        }
    }
}
