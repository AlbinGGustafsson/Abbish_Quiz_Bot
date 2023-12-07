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
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.*;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class QuizGame extends AudioEventAdapter {

    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
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
    private List<String> trackInfoList = new ArrayList<>();

    private List<GameRound> gameRounds = new ArrayList<>();

    private int songCount = 0;

    private long songStartPos = 10000;
    private int songDuration = 20;

    private boolean gameStarted = false;

    private boolean gameMasterPlaying = false;

    public QuizGame(MessageReceivedEvent startGameEvent, AudioPlayerManager playerManager, String gameId) {
        this.trackLoader = new TrackLoader(playerManager);
        this.spotifyHandler = new SpotifyHandler();
        this.player = playerManager.createPlayer();
        this.startGameEvent = startGameEvent;
        this.gameId = gameId;
        this.gameMaster = startGameEvent.getMember();
        this.audioChannel = gameMaster.getVoiceState().getChannel();
        this.gameMembers = audioChannel.getMembers();
        player.addListener(this);

        startGame();
    }

    private AudioManager joinChannel(MessageReceivedEvent event) {
        AudioManager audioManager = event.getGuild().getAudioManager();
        if (!audioManager.isConnected()) {
            audioManager.openAudioConnection(event.getMember().getVoiceState().getChannel());
        }
        audioManager.setSendingHandler(new AudioPlayerSendHandler(player));
        return audioManager;
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

        gameRounds.get(songCount - 1).handleGuess(event);
    }

    private void handleGameMasterMessage(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();

        if (message.equals("!score")){
            String totalScoreMessage = GameRound.formatTotalScores(gameRounds);
            startGameEvent.getChannel().sendMessage(totalScoreMessage).queue();
        }
    }

    private void handlePrivateGameMasterMessage(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        String[] command = message.split(" ");

        if (message.equals("!playing")){
            gameMasterPlaying = true;

        }

        if (command[0].equals("!add") && command.length == 2) {
            trackInfoList = spotifyHandler.getTrackInfoList(command[1]);
            List<AudioTrack> tracks = trackLoader.getTracks(trackInfoList);
            gameSongQueue.addAll(tracks);

            event.getChannel().sendMessage(tracks.size() + " songs added\n" + trackInfoList + "\n").queue();
        }

        if (message.equals("!start")) {
            gameStarted = true;
            joinChannel(startGameEvent);
            //MusicPlayerUtil.playTrackForDuration(player, gameSongQueue.poll(), 10000, 10);
            startGameEvent.getChannel().sendMessage("Game on!").queue();
            player.playTrack(gameSongQueue.poll()); // Cloning the track so we can manipulate it without affecting the original
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
    }

    @Override
    public void onPlayerResume(AudioPlayer player) {
        // Player was resumed
    }

    @Override
    public void onTrackStart(AudioPlayer player, AudioTrack track) {
        GameRound gameRound = new GameRound(trackInfoList.get(songCount), gameMembers);
        gameRounds.add(gameRound);
        songCount++;
        startGameEvent.getChannel().sendMessage("Song " + songCount + " of " + trackInfoList.size()).queue();
        player.getPlayingTrack().setPosition(songStartPos);
        scheduler.schedule(() -> player.stopTrack(), songDuration, TimeUnit.SECONDS);
    }

    @Override
    public void onTrackEnd(AudioPlayer player, AudioTrack track, AudioTrackEndReason endReason) {
        if (endReason.mayStartNext) {
            // ...
        }

        Map<Member, Double> roundScore = gameRounds.get(songCount - 1).getScore();
        String formattedScores = GameRound.formatScores(roundScore);
        startGameEvent.getChannel().sendMessage(formattedScores).queue();

        startGameEvent.getChannel().sendMessage("The song was: " + trackInfoList.get(songCount - 1) + "! Starting next song").queue();
        player.playTrack(gameSongQueue.poll());
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
