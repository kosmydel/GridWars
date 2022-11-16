package gridwars.starter.GridSearch;

import cern.ais.gridwars.HeadlessRunner;
import cern.ais.gridwars.api.bot.PlayerBot;
import gridwars.starter.*;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class GridSearch {
    public static void main(String[] args) {

        List<Supplier<? extends PlayerBot>> list = List.of(
                GluttonBot4::new,
                GluttonBot::new,
                GluttonBot2::new,
                GluttonBot3::new,
                iFABot::new,
                MovingBot::new);
        PlayBotsScoreboard(list);
    }
    private static void PlayBotsScoreboard(List<Supplier<? extends PlayerBot>> list)
    {
        Map<String, Integer> scoreboardWins = new HashMap<>();
        Map<String, Integer> scoreboardDraws = new HashMap<>();
        Map<String, Integer> scoreboardLosses = new HashMap<>();

        for(int i = 0; i < list.size(); i++)
        {
            for(int j= i + 1; j < list.size(); j++)
            {
                for(int round = 0 ; round < 3 ; round++)
                {
                    try{
                        PlayerBot redBot = list.get(i).get();
                        PlayerBot blueBot = list.get(j).get();
                        String redBotName = getBotName(redBot);
                        String blueBotName = getBotName(blueBot);

                        scoreboardWins.putIfAbsent(redBotName, 0);
                        scoreboardDraws.putIfAbsent(redBotName, 0);
                        scoreboardLosses.putIfAbsent(redBotName, 0);

                        scoreboardWins.putIfAbsent(blueBotName, 0);
                        scoreboardDraws.putIfAbsent(blueBotName, 0);
                        scoreboardLosses.putIfAbsent(blueBotName, 0);


                        String redBotLog = String.format("./logs/%s-%s_%d-%s.log",
                                redBotName, blueBotName, round, redBotName);
                        String blueBotLog = String.format("./logs/%s-%s_%d-%s.log",
                                redBotName, blueBotName, round, blueBotName);
                        int ret = HeadlessRunner.runGame(blueBot, redBot, blueBotLog, redBotLog);
                        System.out.printf("%d %s %s %s\n", round, blueBotName, getSign(ret), redBotName);
                        switch (ret)
                        {
                            case -1:
                                //redbotwon
                                scoreboardWins.computeIfPresent(redBotName, (key, old) -> old + 1);
                                scoreboardLosses.computeIfPresent(blueBotName, (key, old) -> old + 1);
                                break;
                            case 0:
                                //draw
                                scoreboardDraws.computeIfPresent(redBotName, (key, old) -> old + 1);
                                scoreboardDraws.computeIfPresent(blueBotName, (key, old) -> old + 1);
                                break;
                            case 1:
                                //bluebot
                                scoreboardWins.computeIfPresent(blueBotName, (key, old) -> old + 1);
                                scoreboardLosses.computeIfPresent(redBotName, (key, old) -> old + 1);
                                break;
                        }
                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                }


            }
        }
        System.out.println("Name =>  Wins, Draws, Losses");
        for(String key: scoreboardWins.keySet())
        {
            System.out.printf("%s => %d - %d - %d\n",key,scoreboardWins.get(key), scoreboardDraws.get(key), scoreboardLosses.get(key));
        }
    }


    private static String getBotName(PlayerBot bot)
    {
        if(bot instanceof INameGen)
        {
            return ((INameGen)bot).getName();
        }
        return bot.getClass().getCanonicalName();
    }

    private static String getSign(int ret)
    {
        if(ret == -1)
            return "<";
        if(ret == 1)
            return ">";
        return "=";
    }
}
