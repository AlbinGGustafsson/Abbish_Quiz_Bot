package org.quiz;

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.middleman.AudioChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.util.*;

public class QuizGame extends AudioEventAdapter {

    private TrackLoader trackLoader;
    private String gameId;
    private SpotifyHandler spotifyHandler;

    private Member gameMaster;
    private AudioChannel audioChannel;
    private final AudioPlayer player;
    private List<Member> gameMembers;
    private Map<User, PrivateChannel> gameMembersPrivateChannels = new HashMap<>();
    private MessageReceivedEvent startGameEvent;
    private LinkedList<AudioTrack> gameSongQueue = new LinkedList<>();

    private boolean gameStarted = false;

    public QuizGame(MessageReceivedEvent startGameEvent, AudioPlayerManager playerManager, String gameId) {
        this.trackLoader = new TrackLoader(playerManager);
        this.spotifyHandler = new SpotifyHandler();
        this.player = playerManager.createPlayer();
        this.startGameEvent = startGameEvent;
        this.gameId = gameId;
        this.gameMaster = startGameEvent.getMember();
        this.audioChannel = gameMaster.getVoiceState().getChannel();
        this.gameMembers = audioChannel.getMembers();
        startGame();
    }

    private void startGame() {
        User user = gameMaster.getUser();
        sendMessageToUser(user, "Provide spotify links by doing !add spotify-url\n(!start when done)");
    }

    public void handleOnMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().getId().equals(gameMaster.getId())) {
            if (event.isFromType(ChannelType.PRIVATE)) {
                handlePrivateGameMasterMessage(event);
                return;
            }
            handleGameMasterMessage(event);
            return;
        }
        if (event.isFromType(ChannelType.PRIVATE)) {
            handleMemberPrivateMessage(event);
            return;
        }
        handleMemberMessage(event);


        String message = event.getMessage().getContentRaw();
        if (message.equals("!gameinfo")) {
            event.getChannel().sendMessage(toString()).queue();
        }
    }

    private void handleMemberMessage(MessageReceivedEvent event) {
        System.out.println("handleMemberMessage");
    }

    private void handleMemberPrivateMessage(MessageReceivedEvent event) {
        if (!gameStarted) {
            event.getChannel().sendMessage("Wait for gamemaster to start the game").queue();
        }
        System.out.println("handleMemberPrivateMessage");
    }

    private void handleGameMasterMessage(MessageReceivedEvent event) {
        System.out.println("handleGameMasterMessage");
    }

    private void handlePrivateGameMasterMessage(MessageReceivedEvent event) {
        System.out.println("handlePrivateGameMasterMessage");

        String message = event.getMessage().getContentRaw();
        String[] command = message.split(" ");

        if (command[0].equals("!add") && command.length == 2) {
            List<String> trackInfoList = spotifyHandler.getTrackInfoList(command[1]);
            System.out.println(trackInfoList);
            List<AudioTrack> tracks = trackLoader.getTracks(trackInfoList);
            tracks.forEach(t -> System.out.println(t.getInfo().title));
        }
    }

    public void sendMessageToUser(User user, String message) {
        PrivateChannel privateChannel = gameMembersPrivateChannels.get(user);

        if (privateChannel != null) {
            privateChannel.sendMessage(message).queue();
        } else {
            user.openPrivateChannel().queue(newPrivateChannel -> {
                gameMembersPrivateChannels.put(user, newPrivateChannel);
                newPrivateChannel.sendMessage(message).queue();
            }, throwable -> {
                System.out.println("Failed to send DM: " + throwable.getMessage());
            });
        }
    }

    @Override
    public void onPlayerPause(AudioPlayer player) {
        // Player was paused
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        // Player was resumed
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        // A track started playing
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
        }
    }

    public AudioChannel getAudioChannel() {
        return audioChannel;
    }

    public List<String> getGameMemberIds() {
        ArrayList<String> ids = new ArrayList<>();
        gameMembers.forEach(m -> ids.add(m.getId()));
        return ids;
    }

    @Override
    public String toString() {
        StringBuilder members = new StringBuilder();

        String gameMasterMention = String.format("<@%s>", gameMaster.getId());

        for (Member member : gameMembers) {
            String mention = String.format("<@%s>", member.getId());
            members.append(mention).append(" ");
        }

        return String.format("Game Id: %s Channel: %s Game Master: %s Players: %s", gameId, audioChannel.getName(), gameMasterMention, members);
    }


}
