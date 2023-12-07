package org.quiz;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class TrackLoader {

    private AudioPlayerManager playerManager;

    public TrackLoader(AudioPlayerManager playerManager) {
        this.playerManager = playerManager;
    }


    public List<AudioTrack> getTracks(List<String> infoList) {
        List<AudioTrack> tracks = new ArrayList<>();
        List<CompletableFuture<AudioTrack>> futures = new ArrayList<>();

        for (String query : infoList) {
            CompletableFuture<AudioTrack> futureTrack = youtubeSearch(query);
            futures.add(futureTrack);
        }

        // Wait for all futures to complete
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        // Add completed tracks to the list
        for (CompletableFuture<AudioTrack> future : futures) {
            try {
                AudioTrack track = future.get(); // This can throw an exception if the future completed exceptionally
                if (track != null) {
                    tracks.add(track);
                } else {
                    // Log or notify about the failure to queue the track
                }
            } catch (Exception e) {
                // Handle exceptions here
            }
        }

        return tracks;
    }

    private CompletableFuture<AudioTrack> youtubeSearch(String query) {
        CompletableFuture<AudioTrack> futureTrack = new CompletableFuture<>();
        playerManager.loadItem("ytsearch:" + query, new AudioLoadResultHandler() {
            @Override
            public void trackLoaded(AudioTrack track) {
                futureTrack.complete(track);
            }

            @Override
            public void playlistLoaded(AudioPlaylist playlist) {
                if (!playlist.getTracks().isEmpty()) {
                    futureTrack.complete(playlist.getTracks().get(0));
                } else {
                    futureTrack.complete(null);
                }
            }

            @Override
            public void noMatches() {
                futureTrack.complete(null);
            }

            @Override
            public void loadFailed(FriendlyException exception) {
                futureTrack.completeExceptionally(exception);
            }
        });

        return futureTrack;
    }

}
