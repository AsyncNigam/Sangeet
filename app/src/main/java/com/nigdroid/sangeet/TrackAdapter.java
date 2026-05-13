package com.nigdroid.sangeet;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.nigdroid.sangeet.databinding.ItemTrackGlassBinding;

import java.util.List;
import java.util.Locale;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    public interface OnTrackClickListener {
        void onTrackClick(int position);
    }

    private final List<Track> tracks;
    private final OnTrackClickListener listener;

    public TrackAdapter(List<Track> tracks, OnTrackClickListener listener) {
        this.tracks = tracks;
        this.listener = listener;
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemTrackGlassBinding binding = ItemTrackGlassBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        );
        return new TrackViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        holder.bind(tracks.get(position), position);
    }

    @Override
    public int getItemCount() {
        return tracks.size();
    }

    static String formatDuration(long durationMs) {
        if (durationMs <= 0) {
            return "--:--";
        }
        long totalSeconds = durationMs / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.US, "%d:%02d", minutes, seconds);
    }

    class TrackViewHolder extends RecyclerView.ViewHolder {

        private final ItemTrackGlassBinding binding;

        TrackViewHolder(ItemTrackGlassBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Track track, int position) {
            binding.title.setText(track.getTitle());
            String artist = track.getArtist();
            binding.subtitle.setText(artist.isEmpty() ? binding.getRoot().getContext().getString(R.string.unknown_artist) : artist);
            binding.duration.setText(formatDuration(track.getDurationMs()));
            Uri art = MusicLibrary.albumArtUri(track.getAlbumId());
            if (art != null) {
                binding.cover.setImageURI(art);
            } else {
                binding.cover.setImageResource(R.drawable.baseline_music_note_24);
            }
            binding.getRoot().setOnClickListener(v -> listener.onTrackClick(position));
        }
    }
}
