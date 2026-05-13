package com.nigdroid.sangeet;

import java.util.Locale;

final class TrackTitleFormatter {

    private TrackTitleFormatter() {
    }

    static String formatTitle(String title, String displayName) {
        String raw = firstNonBlank(title, displayName);
        if (raw == null) {
            return "";
        }
        raw = raw.trim();
        if (raw.isEmpty()) {
            return "";
        }
        raw = stripAudioExtension(raw);
        raw = raw.replace('_', ' ').replace("  ", " ").trim();
        if (raw.matches("\\d+")) {
            if (displayName != null && !displayName.trim().isEmpty() && !displayName.trim().equals(raw)) {
                return formatTitle(displayName, null);
            }
            return "";
        }
        return toTitleCaseWords(raw);
    }

    static String formatArtist(String artist) {
        if (artist == null) {
            return "";
        }
        String a = artist.trim();
        if (a.isEmpty()) {
            return "";
        }
        String lower = a.toLowerCase(Locale.US);
        if ("<unknown>".equals(lower)
                || "unknown".equals(lower)
                || "null".equals(lower)
                || "artist".equals(lower)) {
            return "";
        }
        return toTitleCaseWords(a);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.trim().isEmpty()) {
            return a.trim();
        }
        if (b != null && !b.trim().isEmpty()) {
            return b.trim();
        }
        return null;
    }

    private static String stripAudioExtension(String name) {
        int dot = name.lastIndexOf('.');
        if (dot <= 0 || dot >= name.length() - 1) {
            return name;
        }
        String ext = name.substring(dot + 1).toLowerCase(Locale.US);
        if (ext.matches("mp3|m4a|flac|wav|ogg|aac|wma|opus|3gp|amr|aiff")) {
            return name.substring(0, dot);
        }
        return name;
    }

    private static String toTitleCaseWords(String input) {
        String[] parts = input.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            String w = parts[i];
            if (w.isEmpty()) {
                continue;
            }
            if (i > 0) {
                sb.append(' ');
            }
            if (w.length() == 1) {
                sb.append(w.toUpperCase(Locale.US));
            } else {
                sb.append(w.substring(0, 1).toUpperCase(Locale.US));
                sb.append(w.substring(1).toLowerCase(Locale.US));
            }
        }
        return sb.toString();
    }
}
