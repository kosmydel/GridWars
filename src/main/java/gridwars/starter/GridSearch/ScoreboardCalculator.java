package gridwars.starter.GridSearch;

import cern.ais.gridwars.HeadlessRunner;
import cern.ais.gridwars.api.bot.PlayerBot;
import gridwars.starter.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ScoreboardCalculator {
    public static void main(String[] args) {

        List<Supplier<? extends PlayerBot>> list = List.of(
//                () -> new GluttonBot(115, 1.0),
                GluttonBot::new,
                GluttonBotPRO5::new,
                GluttonBotPRO6::new,
                GluttonBotPRO6_T::new,
                GluttonBotPRO5_T::new,
                SquirrelBot::new
        );
        PlayBotsScoreboard(list, 3);
    }
    static class ParalellResult{
        public String blueBotName;
        public String redBotName;
        public HeadlessRunner.GAME_RESULT result;
        public int round;
        public boolean valid = false;
    }

    private static void PlayBotsScoreboard(List<Supplier<? extends PlayerBot>> list, int rounds_per_match)
    {
        Map<String, Integer> scoreboardWins = new HashMap<>();
        Map<String, Integer> scoreboardDraws = new HashMap<>();
        Map<String, Integer> scoreboardLosses = new HashMap<>();

        var results = IntStream.range(0, list.size()).boxed()
                .flatMap(i -> IntStream.range(i+1, list.size()).boxed()
                        .map(j -> new int[]{i, j}))
                .flatMap(arr -> IntStream.range(0, rounds_per_match).boxed()
                        .map(k -> new int[]{arr[0], arr[1], k}))
                .parallel()
                .map(arr -> {
                    ParalellResult result = new ParalellResult();
                    try {
                        int i = arr[0];
                        int j = arr[1];
                        int round = arr[2];
                        PlayerBot redBot = list.get(i).get();
                        PlayerBot blueBot = list.get(j).get();
                        String redBotName = getBotName(redBot);
                        String blueBotName = getBotName(blueBot);
                        result.redBotName = redBotName;
                        result.blueBotName = blueBotName;
                        result.round = round;

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
                        result.result = HeadlessRunner.runGame(blueBot, redBot, blueBotLog, redBotLog);
                        result.valid = true;

                    }
                    catch (Exception e)
                    {
                        e.printStackTrace();
                    }
                    return result;

                }).collect(Collectors.toList());
        for(var result : results) {

            System.out.printf("%d %s %s %s\n", result.round, result.blueBotName, getSign(result.result), result.redBotName);
            switch (result.result)
            {
                case RED_WON:
                    //redbotwon
                    scoreboardWins.computeIfPresent(result.redBotName, (key, old) -> old + 1);
                    scoreboardLosses.computeIfPresent(result.blueBotName, (key, old) -> old + 1);
                    break;
                case DRAW:
                    //draw
                    scoreboardDraws.computeIfPresent(result.redBotName, (key, old) -> old + 1);
                    scoreboardDraws.computeIfPresent(result.blueBotName, (key, old) -> old + 1);
                    break;
                case BLUE_WON:
                    //bluebot
                    scoreboardWins.computeIfPresent(result.blueBotName, (key, old) -> old + 1);
                    scoreboardLosses.computeIfPresent(result.redBotName, (key, old) -> old + 1);
                    break;
            }

        }
        System.out.println("Name =>  Wins, Draws, Losses");
        List<String> keys = new LinkedList<>(scoreboardWins.keySet());
        keys.sort(Comparator.comparingInt(scoreboardWins::get));
        for(String key: keys)
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

    private static String getSign(HeadlessRunner.GAME_RESULT ret)
    {
        if(ret == HeadlessRunner.GAME_RESULT.RED_WON)
            return "<";
        if(ret == HeadlessRunner.GAME_RESULT.BLUE_WON)
            return ">";
        return "=";
    }
}
