package org.quiz;

import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.model_objects.credentials.ClientCredentials;
import se.michaelthelin.spotify.model_objects.specification.*;
import se.michaelthelin.spotify.requests.authorization.client_credentials.ClientCredentialsRequest;
import se.michaelthelin.spotify.requests.data.albums.GetAlbumRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetPlaylistRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SpotifyHandler {

    private final String clientId = "CLIENT_ID";
    private final String clientSecret = "CLIENT_SECRET";

    private SpotifyApi spotifyApi;

    private long tokenExpirationTime = 0;

    public SpotifyHandler() {
        spotifyApi = new SpotifyApi.Builder()
                .setClientId(clientId)
                .setClientSecret(clientSecret)
                .build();
        authenticateSpotify();
    }

    private void authenticateSpotify() {
        try {
            ClientCredentialsRequest clientCredentialsRequest = spotifyApi.clientCredentials().build();
            ClientCredentials credentials = clientCredentialsRequest.execute();
            spotifyApi.setAccessToken(credentials.getAccessToken());

            // Set token expiration time
            long expiresIn = credentials.getExpiresIn();
            this.tokenExpirationTime = System.currentTimeMillis() + (expiresIn * 1000);
        } catch (Exception e) {
            throw new RuntimeException("Error during Spotify authentication: " + e.getMessage());
        }
    }

    private void ensureTokenIsValid() {
        if (System.currentTimeMillis() > tokenExpirationTime) {
            authenticateSpotify();
        }
    }

    public List<String> getTrackInfoList(String url) {
        ensureTokenIsValid();
        List<String> trackIds = new ArrayList<>();
        String id = extractIdFromUrl(url);

        if (url.contains("track")) {
            // Extract single track ID from track URL
            String trackId = getTrackInfo(id);
            trackIds.add(trackId);
        } else {
            // Extract IDs from playlist or album URL
            if (url.contains("playlist")) {
                trackIds.addAll(getTrackInfoFromPlaylist(id));
            } else if (url.contains("album")) {
                trackIds.addAll(getTrackInfoFromAlbum(id));
            } else {
                throw new RuntimeException("Invalid Spotify URL");
            }
        }

        return trackIds;
    }

    private String getTrackInfo(String trackId) {
        try {
            Track track = spotifyApi.getTrack(trackId).build().execute();
            ArtistSimplified[] artists = track.getArtists();

            StringBuilder artistsStringBuilder = new StringBuilder();
            for (ArtistSimplified artist : artists) {
                artistsStringBuilder.append(artist.getName()).append(":");
            }
            if (artistsStringBuilder.length() > 0) {
                artistsStringBuilder.setLength(artistsStringBuilder.length() - 1);
            }
            return track.getName() + ";" + artistsStringBuilder;
        } catch (Exception e) {
            throw new RuntimeException("Error fetching track info: " + e.getMessage());
        }
    }

    private String extractIdFromUrl(String url) {
        Pattern pattern = Pattern.compile("spotify\\.com/(playlist|album|track)/([a-zA-Z0-9]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(2);
        }
        throw new RuntimeException("Invalid Spotify URL");
    }

    private List<String> getTrackInfoFromPlaylist(String playlistId) {
        List<String> trackInfo = new ArrayList<>();
        try {
            GetPlaylistRequest request = spotifyApi.getPlaylist(playlistId).build();
            Paging<PlaylistTrack> playlistTrackPaging = request.execute().getTracks();

            for (PlaylistTrack playlistTrack : playlistTrackPaging.getItems()) {
                trackInfo.add(getTrackInfo(playlistTrack.getTrack().getId()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trackInfo;
    }

    private List<String> getTrackInfoFromAlbum(String albumId) {
        List<String> trackInfo = new ArrayList<>();
        try {
            GetAlbumRequest request = spotifyApi.getAlbum(albumId).build();
            Paging<TrackSimplified> albumTrackPaging = request.execute().getTracks();

            for (TrackSimplified track : albumTrackPaging.getItems()) {
                trackInfo.add(getTrackInfo(track.getId()));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return trackInfo;
    }
}