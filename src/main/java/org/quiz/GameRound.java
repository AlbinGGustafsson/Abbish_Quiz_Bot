package org.quiz;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.text.similarity.LevenshteinDistance;


import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class GameRound {
    private String title;

    private long startTime;
    private List<String> artists;
    private List<Member> gameMembers;
    private Map<String, Double> titleTimeMap = new LinkedHashMap<>();
    private Map<String, Double> artistTimeMap = new LinkedHashMap<>();

    public GameRound(String trackInfo, List<Member> gameMembers) {
        this.startTime = System.currentTimeMillis();
        this.gameMembers = gameMembers;
        parseTrackInfo(trackInfo);
    }

    private void parseTrackInfo(String input) {
        // Splitting the input string into two parts: title and artists
        String[] parts = input.split(";", 2);
        // Checking if the input format is correct
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid input format. Expected format: 'Title:Artist1;Artist2;...'. Received: " + input);
        }
        title = parts[0];
        artists = Arrays.asList(parts[1].split(":"));
    }

    public void handleGuess(MessageReceivedEvent event){
        String message = event.getMessage().getContentRaw().toLowerCase();
        String authorId = event.getAuthor().getId();

        System.out.println("Guess: " + message);
        long guessTime = System.currentTimeMillis();
        double timeDiff = calculateTimeDifferenceInSeconds(startTime, guessTime);

        if (isGuessApproximatelyCorrect(message, title.toLowerCase())) {
            if (titleTimeMap.putIfAbsent(authorId, timeDiff) == null) {
                event.getChannel().sendMessage("Got guessed the title correct in " + String.format("%.2f", timeDiff) + " seconds!").queue();
            }
            return;
        }

        for (String artist : artists) {
            if (isGuessApproximatelyCorrect(message, artist.toLowerCase())) {
                if (artistTimeMap.putIfAbsent(authorId, timeDiff) == null) {
                    event.getChannel().sendMessage("Got guessed the artist correct in " + String.format("%.2f", timeDiff) + " seconds!").queue();
                }
                return;
            }
        }
    }

    private boolean isGuessApproximatelyCorrect(String guess, String correctAnswer) {
        LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
        int distance = levenshteinDistance.apply(guess, correctAnswer);
        double similarity = 1.0 - (double) distance / correctAnswer.length();
        return similarity >= 0.5;
    }
    private double calculateTimeDifferenceInSeconds(long startTime, long endTime) {
        double differenceInMillis = endTime - startTime;
        return Math.round(differenceInMillis / 1000.0 * 100.0) / 100.0;
    }

    public Map<Member, Double> getScore() {
        Map<Member, Double> scores = new LinkedHashMap<>();
        double bonusForBoth = 5.0; // Set a bonus value

        // Calculate scores from guesses
        for (Member member : gameMembers) {
            String memberId = member.getId();
            double score = 0.0;
            boolean guessedTitle = false;
            boolean guessedArtist = false;

            // Check title guess
            if (titleTimeMap.containsKey(memberId)) {
                double time = titleTimeMap.get(memberId);
                score += time > 0 ? (1 / time) * 10 : 0; // Score multiplied by 10
                guessedTitle = true;
            }

            // Check artist guess
            if (artistTimeMap.containsKey(memberId)) {
                double time = artistTimeMap.get(memberId);
                score += time > 0 ? (1 / time) * 10 : 0; // Score multiplied by 10
                guessedArtist = true;
            }

            // Add bonus if both title and artist are guessed
            if (guessedTitle && guessedArtist) {
                score += bonusForBoth;
            }

            scores.put(member, score);
        }
        return scores;
    }

    public static String formatTotalScores(List<GameRound> gameRounds) {
        Map<Member, Double> totalScores = new LinkedHashMap<>();

        // Aggregate scores from each round
        for (GameRound round : gameRounds) {
            round.getScore().forEach((member, score) ->
                    totalScores.merge(member, score, Double::sum));
        }

        // Sort the total scores in descending order
        Stream<Map.Entry<Member, Double>> sorted = totalScores.entrySet().stream()
                .sorted(Map.Entry.<Member, Double>comparingByValue().reversed());

        // Build the formatted string
        StringBuilder totalScoreMessage = new StringBuilder();
        totalScoreMessage.append("🏆 Total Scores 🏆\n");

        sorted.forEach(entry -> {
            Member member = entry.getKey();
            Double score = entry.getValue();
            totalScoreMessage.append(member.getEffectiveName())
                    .append(": ")
                    .append(String.format("%.2f", score))
                    .append("\n");
        });

        return totalScoreMessage.toString();
    }

    public static String formatScores(Map<Member, Double> roundScore) {
        // Create a stream from the entry set, sort it in descending order by value (score)
        Stream<Map.Entry<Member, Double>> sorted = roundScore.entrySet().stream()
                .sorted(Map.Entry.<Member, Double>comparingByValue().reversed());

        StringBuilder scoreMessage = new StringBuilder();
        scoreMessage.append("🎵 Round Scores 🎵\n");

        // Iterate through each sorted entry and append it to the StringBuilder
        sorted.forEach(entry -> {
            Member member = entry.getKey();
            Double score = entry.getValue();

            scoreMessage.append(member.getEffectiveName())
                    .append(": ")
                    .append(String.format("%.2f", score))
                    .append("\n");
        });

        return scoreMessage.toString();
    }

}
