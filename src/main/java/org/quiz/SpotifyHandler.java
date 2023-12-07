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

    private final String clientId = "3e0633ab28cc42578c77efd03cdb7fb5";
    private final String clientSecret = "32ea65e4149740669d18f7780f291f6d";

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

    public String extractTrackId(String url) {
        ensureTokenIsValid();
        Pattern pattern = Pattern.compile("spotify\\.com/track/([a-zA-Z0-9]+)");
        Matcher matcher = pattern.matcher(url);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            throw new RuntimeException();
        }
    }

    public List<String> getTrackIdsFromPlaylistOrAlbum(String url) {
        ensureTokenIsValid();
        String id = extractIdFromUrl(url);
        return url.contains("playlist") ? getTrackInfoFromPlaylist(id) : getTrackInfoFromAlbum(id);
    }

    public String getTrackInfo(String trackId) {
        try {
            Track track = spotifyApi.getTrack(trackId).build().execute();
            ArtistSimplified[] artists = track.getArtists();

            StringBuilder artistsStringBuilder = new StringBuilder();
            for (ArtistSimplified artist : artists) {
                artistsStringBuilder.append(artist.getName()).append(" ");
            }

            return track.getName() + " by " + artistsStringBuilder.toString().trim();
        } catch (Exception e) {
            throw new RuntimeException("Error fetching track info: " + e.getMessage());
        }
    }

    private String extractIdFromUrl(String url) {
        Pattern pattern = Pattern.compile("spotify\\.com/(playlist|album)/([a-zA-Z0-9]+)");
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