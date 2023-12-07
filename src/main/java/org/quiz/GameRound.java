package org.quiz;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import java.lang.reflect.Member;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GameRound {

    private String trackInfo;
    private List<Member> gameMembers;
    private Map<Member, Double> titleTimeMap = new LinkedHashMap<>();
    private Map<Member, Double> artistTimeMap = new LinkedHashMap<>();

    public GameRound(String trackInfo, List<Member> gameMembers) {
        this.trackInfo = trackInfo;
        this.gameMembers = gameMembers;
    }

    private void handleGuess(MessageReceivedEvent event){

    }

    private Map<Member, Double> getScore(){
        return null;
    }

}
