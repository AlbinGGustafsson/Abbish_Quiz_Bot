package org.quiz;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;

import java.util.Arrays;

public class QuizBot {

    //Client secret: MKFYylP44pJGEQ8QTCMYYZEPHasOuMDm

    public static GatewayIntent[] INTENTS = {GatewayIntent.MESSAGE_CONTENT, GatewayIntent.DIRECT_MESSAGES, GatewayIntent.GUILD_MESSAGES,
            GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.GUILD_VOICE_STATES, GatewayIntent.GUILD_PRESENCES};

    public static void main(String[] args) {

        JDABuilder builder = JDABuilder.createDefault("DISCORD_API_TOKEN", Arrays.asList(INTENTS));
        builder.enableCache(CacheFlag.VOICE_STATE);
        builder.setBulkDeleteSplittingEnabled(false);
        builder.setActivity(Activity.playing("chilling"));
        builder.setStatus(OnlineStatus.ONLINE);

        builder.addEventListeners(new BotListener());

        builder.build();
    }

}
