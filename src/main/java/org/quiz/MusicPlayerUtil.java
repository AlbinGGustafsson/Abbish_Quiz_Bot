package org.quiz;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MusicPlayerUtil {
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void playTrackForDuration(AudioPlayer player, AudioTrack track, int startMillis, int durationSeconds) {
        // Start the track
        player.playTrack(track); // Cloning the track so we can manipulate it without affecting the original

        // Seek to the start position
        player.getPlayingTrack().setPosition(startMillis);

        // Schedule to stop the track after the duration
        scheduler.schedule(() -> player.stopTrack(), durationSeconds, TimeUnit.SECONDS);
    }
}