package org.quiz;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayer;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.managers.AudioManager;

import java.util.ArrayList;
import java.util.List;

public class BotListener extends ListenerAdapter {

    private final AudioPlayerManager playerManager;
    private List<QuizGame> quizGameList;

    public BotListener() {
        this.quizGameList = new ArrayList<>();
        this.playerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerRemoteSources(playerManager);
    }

    private QuizGame getGame(MessageReceivedEvent event) {
        for (QuizGame game : quizGameList) {
            String userId = event.getAuthor().getId();
            if(game.getGameMemberIds().contains(userId)){
                return game;
            }
        }
        return null;
    }
    private boolean gameExistsInGuild(Guild guild) {
        for (QuizGame game : quizGameList) {
            if (guild.getVoiceChannels().contains(game.getAudioChannel())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String message = event.getMessage().getContentRaw();
        String[] command = message.split(" ");

        if (event.getAuthor().isBot()) {
            return;
        }

        if (command.equals("!test")){
            AudioPlayer player = playerManager.createPlayer();

            playerManager.loadItem("https://www.youtube.com/watch?v=lC6aZfX4dG4", new AudioLoadResultHandler() {
                @Override
                public void trackLoaded(AudioTrack audioTrack) {
                    player.playTrack(audioTrack);
                }

                @Override
                public void playlistLoaded(AudioPlaylist audioPlaylist) {

                }

                @Override
                public void noMatches() {

                }

                @Override
                public void loadFailed(FriendlyException e) {

                }
            });
        }

        if (command[0].equals("!startgame")) {

            if (gameExistsInGuild(event.getGuild())){
                event.getChannel().sendMessage("Already a game in: " + event.getMember().getVoiceState().getChannel().getName()).queue();
                return;
            }

            String gameId = command[1];
            quizGameList.add(new QuizGame(event, playerManager, gameId));
            event.getChannel().sendMessage("Game initiated in: " + event.getMember().getVoiceState().getChannel().getName() + "\nWait for the gamemaster to start").queue();
            return;
        }

        QuizGame game = getGame(event);
        if (game != null) {
            game.handleOnMessageReceived(event);
        }

    }
}
